package cn.iocoder.yudao.module.icbc.service.acquisition.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.InvoiceOrderDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.invoice.RedInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.acquisition.IcbcAcquisitionMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.InvoiceOrderMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.invoice.RedInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InvoiceIssueStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PaymentStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PreInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RedOffsetStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.TaxStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.UploadStatusEnum;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionProgress;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionProgressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 收购进度派生实现（ADR 0038）。
 *
 * <p>档位的判定是一条**纯函数**：看这笔收购自己（有没有被企业作废）和它那一张开票单的四条
 * 状态线就够了，不需要看历史、也不看顺序。所以「通知重复 / 乱序 / 早到」在这里天然安全——
 * 每次都是拿当前真值重算，而不是按事件累加。
 */
@Slf4j
@Service
public class AcquisitionProgressServiceImpl implements AcquisitionProgressService {

    @Resource
    private IcbcAcquisitionMapper acquisitionMapper;
    @Resource
    private InvoiceOrderMapper invoiceOrderMapper;
    @Resource
    private RedInvoiceMapper redInvoiceMapper;

    @Override
    public AcquisitionProgress derive(IcbcAcquisitionDO acquisition, InvoiceOrderDO order) {
        return derive(acquisition, order, null);
    }

    @Override
    public AcquisitionProgress derive(IcbcAcquisitionDO acquisition, InvoiceOrderDO order,
                                     RedInvoiceDO redInvoice) {
        return AcquisitionProgress.builder()
                .stage(resolveStage(acquisition, order))
                .abnormalReasons(resolveAbnormalReasons(order, redInvoice))
                .build();
    }

    @Override
    public Map<Long, AcquisitionProgress> deriveFor(List<IcbcAcquisitionDO> acquisitions) {
        if (acquisitions == null || acquisitions.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> acquisitionIds = acquisitions.stream()
                .map(IcbcAcquisitionDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, InvoiceOrderDO> orders = new LinkedHashMap<>();
        for (InvoiceOrderDO order : invoiceOrderMapper.selectListByAcquisitionIds(acquisitionIds)) {
            if (order.getAcquisitionId() != null) {
                // 一笔收购只对应一张票（ADR 0038）：真出现多条时取列表里的第一条（按 id 倒序）
                orders.putIfAbsent(order.getAcquisitionId(), order);
            }
        }
        List<String> partnerOrderIds = orders.values().stream()
                .map(InvoiceOrderDO::getPartnerOrderId)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
        Map<String, RedInvoiceDO> redInvoices = new LinkedHashMap<>();
        for (RedInvoiceDO redInvoice : redInvoiceMapper.selectListByPartnerOrderIds(partnerOrderIds)) {
            // 同一张蓝票可有多条红冲记录（撤销后再冲）：列表已按 id 倒序，取第一条
            redInvoices.putIfAbsent(redInvoice.getPartnerOrderId(), redInvoice);
        }
        Map<Long, AcquisitionProgress> result = new LinkedHashMap<>();
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            InvoiceOrderDO order = orders.get(acquisition.getId());
            RedInvoiceDO redInvoice = order == null ? null : redInvoices.get(order.getPartnerOrderId());
            result.put(acquisition.getId(), derive(acquisition, order, redInvoice));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sync(IcbcAcquisitionDO acquisition) {
        if (acquisition == null || acquisition.getId() == null) {
            return;
        }
        String partnerOrderId = acquisition.getInvoicePartnerOrderId();
        InvoiceOrderDO order = StrUtil.isBlank(partnerOrderId)
                ? null : invoiceOrderMapper.selectByPartnerOrderId(partnerOrderId);
        RedInvoiceDO redInvoice = StrUtil.isBlank(partnerOrderId)
                ? null : redInvoiceMapper.selectLatestByPartnerOrderId(partnerOrderId);
        AcquisitionProgress progress = derive(acquisition, order, redInvoice);
        if (Objects.equals(progress.getStatus(), acquisition.getStatus())) {
            return;
        }
        IcbcAcquisitionDO update = new IcbcAcquisitionDO();
        update.setId(acquisition.getId());
        update.setStatus(progress.getStatus());
        acquisitionMapper.updateById(update);
        log.info("收购进度收敛 - acquisitionNo: {}, {} -> {}{}", acquisition.getAcquisitionNo(),
                AcquisitionStatusEnum.nameOf(acquisition.getStatus()), progress.getStageName(),
                progress.isAbnormal() ? "，异常：" + progress.getAbnormalReasons() : "");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncByPartnerOrderId(String partnerOrderId) {
        if (StrUtil.isBlank(partnerOrderId)) {
            return;
        }
        List<IcbcAcquisitionDO> acquisitions = acquisitionMapper
                .selectListByInvoicePartnerOrderIds(Collections.singletonList(partnerOrderId));
        if (acquisitions.isEmpty()) {
            // 通知早于数据落库：不动任何数据，通知落失败后可在通知监控里重放
            log.warn("按合作方订单号收敛收购进度时未找到收购单 - partnerOrderId: {}", partnerOrderId);
            return;
        }
        for (IcbcAcquisitionDO acquisition : acquisitions) {
            sync(acquisition);
        }
    }

    /**
     * 档位。判定顺序即优先级：
     * <ol>
     *   <li>企业已作废（带原因）——作废是敏感动作，压过一切；</li>
     *   <li>没有开票单——还没发起开票申请；</li>
     *   <li>工行侧预开票被取消——这笔票作废了，等重新发起；</li>
     *   <li>票已开出 → 已付款 → 预开票成功 → 其余；</li>
     *   <li>剩下的都算「待自然人确认」：预开票在途、预开票失败、确认还没落定，都属于
     *       「还没走到付款」，具体是哪种由异常标注说清楚。</li>
     * </ol>
     */
    private AcquisitionStatusEnum resolveStage(IcbcAcquisitionDO acquisition, InvoiceOrderDO order) {
        if (StrUtil.isNotBlank(acquisition.getCancelReason())) {
            return AcquisitionStatusEnum.CANCELLED;
        }
        if (order == null) {
            return AcquisitionStatusEnum.REGISTERED;
        }
        if (PreInvoiceStatusEnum.CANCELLED.getStatus().equals(order.getPreInvoiceStatus())) {
            return AcquisitionStatusEnum.CANCELLED;
        }
        if (InvoiceIssueStatusEnum.isIssued(order.getInvoiceStatus())) {
            return AcquisitionStatusEnum.INVOICED;
        }
        if (PaymentStatusEnum.isSuccess(order.getPaymentStatus())) {
            return AcquisitionStatusEnum.PAID;
        }
        if (PreInvoiceStatusEnum.SUCCESS.getStatus().equals(order.getPreInvoiceStatus())) {
            return AcquisitionStatusEnum.PENDING_PAYMENT;
        }
        return AcquisitionStatusEnum.WAITING_SELLER_CONFIRM;
    }

    /**
     * 异常标注：四条状态线各自独立判断，一条都不许被好看的档位盖住（ADR 0021）。
     * 已开票之后缴税 / 上传仍可能失败，所以这里不看档位、只看状态线本身。
     */
    private List<String> resolveAbnormalReasons(InvoiceOrderDO order, RedInvoiceDO redInvoice) {
        if (order == null) {
            return Collections.emptyList();
        }
        List<String> reasons = new ArrayList<>();
        if (PreInvoiceStatusEnum.isException(order.getPreInvoiceStatus())) {
            reasons.add("预开票：" + PreInvoiceStatusEnum.nameOf(order.getPreInvoiceStatus()));
        }
        if (PreInvoiceStatusEnum.CANCELLED.getStatus().equals(order.getPreInvoiceStatus())) {
            // 预开票被取消：档位已经是「已作废」，但取消的原因来自工行侧、与企业的作废不是一回事，
            // 必须写出来，否则这条作废在收货员眼里没有来由
            reasons.add("预开票：预开票已取消（未支付）");
        }
        if (PaymentStatusEnum.isException(order.getPaymentStatus())) {
            reasons.add("付款：" + PaymentStatusEnum.nameOf(order.getPaymentStatus()));
        }
        if (InvoiceIssueStatusEnum.isException(order.getInvoiceStatus())) {
            reasons.add("开票：" + InvoiceIssueStatusEnum.nameOf(order.getInvoiceStatus()));
        }
        if (TaxStatusEnum.isException(order.getTaxStatus())) {
            reasons.add("缴税：" + TaxStatusEnum.nameOf(order.getTaxStatus()));
        }
        if (UploadStatusEnum.isException(order.getUploadStatus())) {
            reasons.add("上传：" + UploadStatusEnum.nameOf(order.getUploadStatus()));
        }
        if (redInvoice != null && RedOffsetStatusEnum.SUCCESS.getStatus().equals(redInvoice.getRedOffsetStatus())) {
            // 红冲**不回退档位**：货款真的付了、蓝票真的开过，退回「待付款」是撒谎。
            // 但红票开出后蓝票在税务上已作废，这件事不能让「已开票」盖住（ADR 0038）
            reasons.add("发票流：已红冲" + (StrUtil.isNotBlank(redInvoice.getReason())
                    ? "（" + redInvoice.getReason() + "）" : ""));
        }
        return reasons;
    }

}
