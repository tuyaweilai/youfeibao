package cn.iocoder.yudao.module.icbc.service.invoice.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceApplyResultVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceQueryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo.RedInvoiceRevokeReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.OrderItemDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.OrderItemMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedInvoiceReasonEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceCancelReq;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceCancelResult;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceQueryReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceGoods;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeReq;
import cn.iocoder.yudao.module.icbc.gateway.model.RedInvoiceRevokeResult;
import cn.iocoder.yudao.module.icbc.service.invoice.InvoiceOrderService;
import cn.iocoder.yudao.module.icbc.service.invoice.RedInvoiceService;
import cn.iocoder.yudao.module.icbc.util.AmountUtils;
import cn.iocoder.yudao.module.icbc.util.IcbcTimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 红字冲销与发票取消 Service 实现。
 *
 * <p>红冲是「另开一张红票冲蓝票」，不是删票；取消是「票还没开出来就别开了」。
 * 两者都要求：能重试但不重复下单、失败与异常可见、状态在通知与主动查询两条路径上一致。
 */
@Slf4j
@Service
public class RedInvoiceServiceImpl implements RedInvoiceService {

    /** 状态码：撤销成功 */
    private static final String REVOKE_STATUS_SUCCESS = "10";
    /** 状态码：撤销失败 */
    private static final String REVOKE_STATUS_FAILED = "11";
    /** 状态码：发票取消成功 */
    private static final String REVERSAL_STATUS_SUCCESS = "00";

    @Resource
    private RedInvoiceMapper redInvoiceMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private InvoiceOrderService invoiceOrderService;
    @Resource
    private IcbcGateway icbcGateway;

    // ==================== 红字冲销 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedInvoiceApplyResultVO apply(RedInvoiceApplyReqVO reqVO) {
        // 1. 蓝票必须存在且已开出
        InvoiceOrderDO order = invoiceOrderService.getOrderByPartnerOrderId(reqVO.getPartnerOrderId());
        if (order == null) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_NOT_EXISTS);
        }
        if (StrUtil.isBlank(order.getInvoiceNo()) && !InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus())) {
            throw exception(ErrorCodeConstants.RED_INVOICE_BLUE_NOT_ISSUED);
        }

        // 2. 原因合法
        RedInvoiceReasonEnum reason = RedInvoiceReasonEnum.ofCode(reqVO.getReason())
                .orElseThrow(() -> exception(ErrorCodeConstants.RED_INVOICE_REASON_INVALID));

        // 3. 幂等 / 重试：同一蓝票已有生效中的红冲时不再下单；失败可原单重试，已撤销可重新发起
        RedInvoiceDO existing = redInvoiceMapper.selectLatestByPartnerOrderId(reqVO.getPartnerOrderId());
        boolean redo = existing != null && isRetrySameRecord(existing);
        if (existing != null && !redo && !isRevoked(existing)) {
            return buildDuplicateResult(existing);
        }
        // 已撤销：保留旧记录留痕，新开一条红冲（新流水号）
        boolean newRecord = existing == null || isRevoked(existing);

        // 4. 金额与明细校验（开票有误必须全额且与蓝票一致）
        BigDecimal blueAmount = order.getInvoiceAmount() != null ? order.getInvoiceAmount() : order.getTotalAmount();
        BigDecimal amount = resolveAmount(reqVO, reason, blueAmount);
        List<RedInvoiceGoods> goods = resolveGoods(reqVO, order, amount, reason);

        // 5. 落库（先落库再下单：结果未知时靠这条记录判定「已提交过」，不产生第二张红票）
        RedInvoiceDO red = newRecord ? new RedInvoiceDO() : existing;
        red.setPartnerOrderId(order.getPartnerOrderId());
        red.setInvoiceOrderId(order.getId());
        red.setAcquisitionId(order.getAcquisitionId());
        red.setReason(reason.getCode());
        red.setAmount(amount);
        red.setTaxAmount(order.getTaxAmount());
        red.setRedOffsetStatus(RedOffsetStatusEnum.INITIAL.getStatus());
        red.setRedOffsetStatusCode(RedOffsetStatusEnum.INITIAL.getCode());
        red.setRemark(reqVO.getRemark());
        if (!newRecord) {
            redInvoiceMapper.updateById(red);
        } else {
            red.setRedOffsetNo(generateRedOffsetNo());
            redInvoiceMapper.insert(red);
        }

        // 6. 经适配层取得红字确认单页面
        IcbcGatewayResult<IcbcPage> result = icbcGateway.applyRedInvoice(RedInvoiceReq.builder()
                .outRedOffsetId(red.getRedOffsetNo())
                .outOrderId(order.getPartnerOrderId())
                .redOffsetReason(reason.getCode())
                .redOffsetAmount(amount != null ? amount.toPlainString() : null)
                .isRedo(redo ? "Y" : "N")
                .jumpUrl(buildJumpUrl(reqVO))
                .goods(goods)
                .build());
        if (result.isUnknown()) {
            throw exception(ErrorCodeConstants.RED_INVOICE_RESULT_UNKNOWN);
        }
        if (!result.isSuccess()) {
            markFailed(red, result.getReturnMsg());
            RedInvoiceApplyResultVO failed = buildResult(red, false, false);
            failed.setMessage(StrUtil.blankToDefault(result.getReturnMsg(), "红冲申请失败"));
            return failed;
        }

        RedInvoiceApplyResultVO resp = buildResult(red, true, false);
        resp.setConfirmPageHtml(result.getData() != null ? result.getData().getFormHtml() : null);
        resp.setMessage("已取得红字确认单页面");
        log.info("红冲申请成功 - redOffsetNo: {}, partnerOrderId: {}, reason: {}",
                red.getRedOffsetNo(), order.getPartnerOrderId(), reason.getCode());
        return resp;
    }

    private boolean isRetrySameRecord(RedInvoiceDO red) {
        RedOffsetStatusEnum status = RedOffsetStatusEnum.ofStatus(red.getRedOffsetStatus()).orElse(null);
        // 申请失败 / 上传失败 / 撤销失败：复用同一条记录重新发起（同一个红冲流水号 + isRedo=Y）
        return status == RedOffsetStatusEnum.APPLY_FAILED
                || status == RedOffsetStatusEnum.UPLOAD_FAILED
                || status == RedOffsetStatusEnum.REVOKE_FAILED;
    }

    private boolean isRevoked(RedInvoiceDO red) {
        return RedOffsetStatusEnum.REVOKED.getStatus().equals(red.getRedOffsetStatus());
    }

    private BigDecimal resolveAmount(RedInvoiceApplyReqVO reqVO, RedInvoiceReasonEnum reason, BigDecimal blueAmount) {
        if (reason.isFullOffsetRequired()) {
            // 开票有误必须全额红冲：金额留空按蓝票金额，填写则必须一致
            if (reqVO.getAmount() != null && reqVO.getAmount().compareTo(blueAmount) != 0) {
                throw exception(ErrorCodeConstants.RED_INVOICE_AMOUNT_MISMATCH,
                        reqVO.getAmount(), blueAmount);
            }
            return blueAmount;
        }
        if (reqVO.getAmount() == null || reqVO.getAmount().signum() <= 0) {
            throw exception(ErrorCodeConstants.RED_INVOICE_AMOUNT_REQUIRED);
        }
        if (reqVO.getAmount().compareTo(blueAmount) > 0) {
            throw exception(ErrorCodeConstants.RED_INVOICE_AMOUNT_MISMATCH,
                    reqVO.getAmount(), blueAmount);
        }
        return reqVO.getAmount();
    }

    /**
     * 明细：开票有误必须与蓝票一致。未提供明细时按蓝票明细全额生成；提供时逐行比对，
     * 金额 / 数量对不上即拒绝。
     */
    private List<RedInvoiceGoods> resolveGoods(RedInvoiceApplyReqVO reqVO, InvoiceOrderDO order,
                                               BigDecimal amount, RedInvoiceReasonEnum reason) {
        List<OrderItemDO> blueItems = orderItemMapper.selectListByOrderId(order.getId());
        boolean provided = reqVO.getGoods() != null && !reqVO.getGoods().isEmpty();
        if (reason.isFullOffsetRequired() && provided) {
            assertFullOffsetGoodsMatch(reqVO.getGoods(), blueItems);
        }
        if (provided) {
            List<RedInvoiceGoods> goods = new ArrayList<>();
            for (RedInvoiceApplyReqVO.Goods item : reqVO.getGoods()) {
                goods.add(toRedGoods(item.getGoodsSeqno(), item.getBlueGoodsSeqno(), item.getProjectName(),
                        item.getGoodsNum(), item.getGoodsAmt(), item.getWeight(), item.getPrice(), item.getUnits()));
            }
            return goods;
        }
        // 开票有误未给明细：按蓝票明细全额生成（与蓝票逐行一致）
        if (reason.isFullOffsetRequired()) {
            return buildFullOffsetGoodsFromBlue(order, blueItems);
        }
        // 部分红冲（退货 / 中止 / 折让）未给明细：用单行承载红冲金额，
        // 保证明细合计 = 红冲金额；需要拆到蓝票各行时由调用方传入 goods
        return Collections.singletonList(toRedGoods("1", "1",
                blueItems.isEmpty() ? order.getRemark() : blueItems.get(0).getItemName(),
                null, amount != null ? amount.toPlainString() : null, null, null,
                blueItems.isEmpty() ? null : blueItems.get(0).getUnit()));
    }

    private List<RedInvoiceGoods> buildFullOffsetGoodsFromBlue(InvoiceOrderDO order, List<OrderItemDO> blueItems) {
        List<RedInvoiceGoods> goods = new ArrayList<>();
        int seq = 1;
        for (OrderItemDO item : blueItems) {
            goods.add(toRedGoods(String.valueOf(seq), String.valueOf(seq), item.getItemName(),
                    item.getQuantity() != null ? item.getQuantity().toPlainString() : null,
                    item.getAmount() != null ? item.getAmount().toPlainString() : null,
                    item.getSpecification(),
                    item.getUnitPrice() != null ? item.getUnitPrice().toPlainString() : null,
                    item.getUnit()));
            seq++;
        }
        // 蓝票没有明细时（历史数据），用单行兜底，保证报文不缺明细
        if (goods.isEmpty()) {
            goods.add(toRedGoods("1", "1", order.getRemark(),
                    null, order.getInvoiceAmount() != null ? order.getInvoiceAmount().toPlainString() : null,
                    null, null, null));
        }
        return goods;
    }

    private void assertFullOffsetGoodsMatch(List<RedInvoiceApplyReqVO.Goods> goods, List<OrderItemDO> blueItems) {
        if (goods.size() != blueItems.size()) {
            throw exception(ErrorCodeConstants.RED_INVOICE_GOODS_MISMATCH);
        }
        for (int i = 0; i < goods.size(); i++) {
            RedInvoiceApplyReqVO.Goods red = goods.get(i);
            OrderItemDO blue = blueItems.get(i);
            if (!amountEquals(red.getGoodsAmt(), blue.getAmount())
                    || !amountEquals(red.getGoodsNum(), blue.getQuantity())
                    || !amountEquals(red.getPrice(), blue.getUnitPrice())) {
                throw exception(ErrorCodeConstants.RED_INVOICE_GOODS_MISMATCH);
            }
        }
    }

    private boolean amountEquals(String red, BigDecimal blue) {
        BigDecimal parsed = AmountUtils.parse(red);
        if (parsed == null || blue == null) {
            return parsed == null && blue == null;
        }
        return parsed.compareTo(blue) == 0;
    }

    private RedInvoiceGoods toRedGoods(String goodsSeqno, String blueGoodsSeqno, String projectName,
                                       String goodsNum, String goodsAmt, String weight, String price, String units) {
        return RedInvoiceGoods.builder()
                .goodsSeqno(goodsSeqno)
                .blueGoodsSeqno(blueGoodsSeqno)
                .projectName(projectName)
                .goodsNum(goodsNum)
                .goodsAmt(goodsAmt)
                .weight(weight)
                .price(price)
                .units(units)
                .build();
    }

    private String buildJumpUrl(RedInvoiceApplyReqVO reqVO) {
        return StrUtil.isBlank(reqVO.getJumpUrlBase()) ? null : reqVO.getJumpUrlBase() + "/icbc/red/return";
    }

    private String generateRedOffsetNo() {
        return "RED" + System.currentTimeMillis() + RandomUtil.randomNumbers(4);
    }

    private void markFailed(RedInvoiceDO red, String message) {
        red.setRedOffsetStatus(RedOffsetStatusEnum.APPLY_FAILED.getStatus());
        String text = StrUtil.blankToDefault(message, red.getRemark());
        red.setRemark(text != null && text.length() > 500 ? text.substring(0, 500) : text);
        red.setRedOffsetStatusCode(RedOffsetStatusEnum.APPLY_FAILED.getCode());
        redInvoiceMapper.updateById(red);
    }

    // ==================== 撤销 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RedInvoiceQueryRespVO revoke(RedInvoiceRevokeReqVO reqVO) {
        RedInvoiceDO red = requireByRedOffsetNo(reqVO.getRedOffsetNo());
        RedOffsetStatusEnum status = RedOffsetStatusEnum.ofStatus(red.getRedOffsetStatus())
                .orElse(RedOffsetStatusEnum.INITIAL);
        if (!status.isRevocable()) {
            throw exception(ErrorCodeConstants.RED_INVOICE_NOT_REVOCABLE, status.getName());
        }
        IcbcGatewayResult<RedInvoiceRevokeResult> result = icbcGateway.revokeRedInvoice(RedInvoiceRevokeReq.builder()
                .outRedOffsetId(red.getRedOffsetNo())
                .outOrderId(red.getPartnerOrderId())
                .build());
        if (result.isUnknown()) {
            throw exception(ErrorCodeConstants.RED_INVOICE_RESULT_UNKNOWN);
        }
        if (!result.isSuccess()) {
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }
        String revokeStatus = result.getData() != null ? result.getData().getRevokeStatus() : null;
        red.setRevokeStatus(revokeStatus);
        red.setRevokeTime(LocalDateTime.now());
        if (REVOKE_STATUS_SUCCESS.equals(revokeStatus)) {
            red.setRedOffsetStatus(RedOffsetStatusEnum.REVOKED.getStatus());
            red.setRedOffsetStatusCode(RedOffsetStatusEnum.REVOKED.getCode());
        } else if (REVOKE_STATUS_FAILED.equals(revokeStatus)) {
            red.setRedOffsetStatus(RedOffsetStatusEnum.REVOKE_FAILED.getStatus());
            red.setRedOffsetStatusCode(RedOffsetStatusEnum.REVOKE_FAILED.getCode());
        } else {
            red.setRedOffsetStatus(RedOffsetStatusEnum.REVOKING.getStatus());
            red.setRedOffsetStatusCode(RedOffsetStatusEnum.REVOKING.getCode());
        }
        redInvoiceMapper.updateById(red);
        log.info("红字确认单撤销 - redOffsetNo: {}, revokeStatus: {}", red.getRedOffsetNo(), revokeStatus);
        return toQueryResp(red);
    }

    private RedInvoiceDO requireByRedOffsetNo(String redOffsetNo) {
        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(redOffsetNo);
        if (red == null) {
            throw exception(ErrorCodeConstants.RED_INVOICE_NOT_EXISTS);
        }
        return red;
    }

    // ==================== 查询与收敛 ====================

    @Override
    public RedInvoiceQueryRespVO getByRedOffsetNo(String redOffsetNo) {
        return toQueryResp(requireByRedOffsetNo(redOffsetNo));
    }

    @Override
    public RedInvoiceQueryRespVO getByPartnerOrderId(String partnerOrderId) {
        RedInvoiceDO red = redInvoiceMapper.selectLatestByPartnerOrderId(partnerOrderId);
        return red == null ? null : toQueryResp(red);
    }

    @Override
    public RedInvoiceQueryRespVO refresh(String redOffsetNo) {
        RedInvoiceDO red = requireByRedOffsetNo(redOffsetNo);
        IcbcGatewayResult<InvoiceInfo> result = icbcGateway.queryInvoiceInfo(InvoiceQueryReq.builder()
                .outOrderId(red.getPartnerOrderId())
                .outRedOffsetId(red.getRedOffsetNo())
                .build());
        if (result.isSuccess() && result.getData() != null) {
            applyRedInvoiceInfo(red.getRedOffsetNo(), result.getData());
            red = redInvoiceMapper.selectByRedOffsetNo(redOffsetNo);
        }
        return toQueryResp(red);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRedInvoiceInfo(String outRedOffsetId, InvoiceInfo info) {
        RedInvoiceDO red = redInvoiceMapper.selectByRedOffsetNo(outRedOffsetId);
        if (red == null) {
            // 通知早于平台数据落库：抛业务异常，通知落失败、数据落库后可重放
            throw exception(ErrorCodeConstants.CALLBACK_BUSINESS_NOT_EXISTS);
        }
        RedInvoiceDO update = new RedInvoiceDO();
        update.setId(red.getId());
        boolean changed = false;

        if (StrUtil.isNotBlank(info.getRedOffsetStatus())) {
            Integer next = RedOffsetStatusEnum.toStatus(info.getRedOffsetStatus());
            if (RedOffsetStatusEnum.shouldApply(red.getRedOffsetStatus(), next)) {
                update.setRedOffsetStatus(next);
                update.setRedOffsetStatusCode(info.getRedOffsetStatus());
                changed = true;
            }
        }
        if (StrUtil.isNotBlank(info.getRedOffsetInvoiceCode())) {
            // 红票号是最硬的凭证：有号即红冲成功
            update.setRedInvoiceNo(info.getRedOffsetInvoiceCode());
            update.setRedOffsetStatus(RedOffsetStatusEnum.SUCCESS.getStatus());
            update.setRedOffsetStatusCode(RedOffsetStatusEnum.SUCCESS.getCode());
            changed = true;
        }
        if (StrUtil.isNotBlank(info.getRedOffsetReason())) {
            update.setReason(info.getRedOffsetReason());
            changed = true;
        }
        LocalDateTime redInvoiceDate = IcbcTimeUtils.parse(info.getInvoiceDate());
        if (redInvoiceDate != null) {
            update.setRedInvoiceDate(redInvoiceDate);
            changed = true;
        }
        if (changed) {
            redInvoiceMapper.updateById(update);
        }
        log.info("红冲状态收敛 - redOffsetNo: {}, redOffsetStatus: {}, redInvoiceNo: {}",
                outRedOffsetId, update.getRedOffsetStatus(), update.getRedInvoiceNo());
    }

    @Override
    public RedInvoiceDO getLatestRedInvoice(String partnerOrderId) {
        return redInvoiceMapper.selectLatestByPartnerOrderId(partnerOrderId);
    }

    // ==================== 发票取消 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelPreInvoice(String partnerOrderId) {
        InvoiceOrderDO order = invoiceOrderService.getOrderByPartnerOrderId(partnerOrderId);
        if (order == null) {
            throw exception(ErrorCodeConstants.INVOICE_ORDER_NOT_EXISTS);
        }
        if (PaymentStatusEnum.isSuccess(order.getPaymentStatus()) || InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus())) {
            throw exception(ErrorCodeConstants.INVOICE_CANCEL_PAID);
        }
        if (!PreInvoiceStatusEnum.SUCCESS.getStatus().equals(order.getPreInvoiceStatus())) {
            throw exception(ErrorCodeConstants.INVOICE_CANCEL_NOT_PRE_SUCCESS);
        }
        IcbcGatewayResult<InvoiceCancelResult> result = icbcGateway.cancelInvoice(InvoiceCancelReq.builder()
                .outOrderId(partnerOrderId)
                .build());
        if (result.isUnknown()) {
            throw exception(ErrorCodeConstants.INVOICE_CANCEL_RESULT_UNKNOWN);
        }
        if (!result.isSuccess()) {
            throw exception(ErrorCodeConstants.ICBC_API_CALL_FAILED);
        }
        String reversalStatus = result.getData() != null ? result.getData().getReversalStatus() : null;
        if (!REVERSAL_STATUS_SUCCESS.equals(reversalStatus)) {
            throw exception(ErrorCodeConstants.INVOICE_CANCEL_FAILED, StrUtil.blankToDefault(
                    result.getData() != null ? result.getData().getReversalMsg() : null, "取消未成功"));
        }
        invoiceOrderService.applyInvoiceCancelled(partnerOrderId);
        log.info("发票取消成功 - partnerOrderId: {}", partnerOrderId);
    }

    // ==================== 转换 ====================

    private RedInvoiceApplyResultVO buildResult(RedInvoiceDO red, boolean success, boolean duplicate) {
        RedInvoiceApplyResultVO resp = new RedInvoiceApplyResultVO();
        resp.setSuccess(success);
        resp.setDuplicate(duplicate);
        resp.setRedOffsetNo(red.getRedOffsetNo());
        resp.setPartnerOrderId(red.getPartnerOrderId());
        resp.setReason(red.getReason());
        resp.setAmount(red.getAmount());
        resp.setRedOffsetStatus(red.getRedOffsetStatus());
        resp.setRedOffsetStatusName(RedOffsetStatusEnum.nameOf(red.getRedOffsetStatus()));
        resp.setNextAction(RedOffsetStatusEnum.nextActionOf(red.getRedOffsetStatus()));
        return resp;
    }

    private RedInvoiceApplyResultVO buildDuplicateResult(RedInvoiceDO red) {
        RedInvoiceApplyResultVO resp = buildResult(red, true, true);
        resp.setMessage("该蓝票已有生效中的红冲记录，未重复发起");
        return resp;
    }

    private RedInvoiceQueryRespVO toQueryResp(RedInvoiceDO red) {
        RedInvoiceQueryRespVO resp = new RedInvoiceQueryRespVO();
        resp.setRedOffsetNo(red.getRedOffsetNo());
        resp.setPartnerOrderId(red.getPartnerOrderId());
        resp.setInvoiceOrderId(red.getInvoiceOrderId());
        resp.setAcquisitionId(red.getAcquisitionId());
        resp.setReason(red.getReason());
        resp.setReasonName(RedInvoiceReasonEnum.nameOf(red.getReason()));
        resp.setAmount(red.getAmount());
        resp.setTaxAmount(red.getTaxAmount());
        resp.setRedOffsetStatus(red.getRedOffsetStatus());
        resp.setRedOffsetStatusName(RedOffsetStatusEnum.nameOf(red.getRedOffsetStatus()));
        resp.setRedOffsetStatusCode(red.getRedOffsetStatusCode());
        resp.setRedInvoiceNo(red.getRedInvoiceNo());
        resp.setRedInvoiceDate(red.getRedInvoiceDate());
        resp.setRevokeStatus(red.getRevokeStatus());
        resp.setRevokeTime(red.getRevokeTime());
        resp.setNextAction(RedOffsetStatusEnum.nextActionOf(red.getRedOffsetStatus()));
        resp.setCreateTime(red.getCreateTime());
        return resp;
    }
}
