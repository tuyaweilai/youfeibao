package cn.iocoder.yudao.module.icbc.service.inputinvoice.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceLinkRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoicePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.inputinvoice.vo.InputInvoiceSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice.IcbcInputInvoiceDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.inputinvoice.IcbcInputInvoiceLinkDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.inputinvoice.IcbcInputInvoiceLinkMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.inputinvoice.IcbcInputInvoiceMapper;
import cn.iocoder.yudao.module.icbc.enums.InputInvoiceBizTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.InputInvoiceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.InputInvoiceTypeEnum;
import cn.iocoder.yudao.module.icbc.service.inputinvoice.InputInvoiceService;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderAmountDTO;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 进项收票 Service 实现（#49 T11，ADR 0029）。
 */
@Service
@Validated
@Slf4j
public class InputInvoiceServiceImpl implements InputInvoiceService {

    @Resource
    private IcbcInputInvoiceMapper invoiceMapper;
    @Resource
    private IcbcInputInvoiceLinkMapper linkMapper;
    @Resource
    private PurchaseOrderService purchaseOrderService;

    // ==================== 登记 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createInvoice(InputInvoiceSaveReqVO createReqVO) {
        assertInvoiceType(createReqVO.getInvoiceType());
        IcbcInputInvoiceDO invoice = BeanUtils.toBean(createReqVO, IcbcInputInvoiceDO.class);
        invoice.setId(null);
        invoice.setInvoiceNo(createReqVO.getInvoiceNo().trim());
        invoice.setInvoiceCode(StrUtil.trimToNull(createReqVO.getInvoiceCode()));
        invoice.setSellerName(createReqVO.getSellerName().trim());
        invoice.setSellerTaxNo(StrUtil.trimToNull(createReqVO.getSellerTaxNo()));
        invoice.setSellerKey(sellerKeyOf(invoice.getSellerTaxNo(), invoice.getSellerName()));
        applyAmount(invoice, createReqVO);
        assertNotDuplicated(invoice.getSellerKey(), invoice.getInvoiceNo(), null);
        invoice.setLinkedAmount(BigDecimal.ZERO);
        invoice.setStatus(InputInvoiceStatusEnum.REGISTERED.getStatus());
        invoiceMapper.insert(invoice);
        return invoice.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateInvoice(InputInvoiceSaveReqVO updateReqVO) {
        IcbcInputInvoiceDO invoice = getInvoice(updateReqVO.getId());
        assertEditable(invoice);
        assertInvoiceType(updateReqVO.getInvoiceType());

        invoice.setInvoiceNo(updateReqVO.getInvoiceNo().trim());
        invoice.setInvoiceCode(StrUtil.trimToNull(updateReqVO.getInvoiceCode()));
        invoice.setInvoiceType(updateReqVO.getInvoiceType());
        invoice.setInvoiceDate(updateReqVO.getInvoiceDate());
        invoice.setSellerName(updateReqVO.getSellerName().trim());
        invoice.setSellerTaxNo(StrUtil.trimToNull(updateReqVO.getSellerTaxNo()));
        invoice.setSellerKey(sellerKeyOf(invoice.getSellerTaxNo(), invoice.getSellerName()));
        applyAmount(invoice, updateReqVO);
        invoice.setRemark(updateReqVO.getRemark());
        assertNotDuplicated(invoice.getSellerKey(), invoice.getInvoiceNo(), invoice.getId());
        invoiceMapper.updateById(invoice);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteInvoice(Long id) {
        IcbcInputInvoiceDO invoice = getInvoice(id);
        assertEditable(invoice);
        // 未勾稽的票理论上没有勾稽记录；清一次是防御，避免脏数据把唯一键带进下次登记
        linkMapper.deleteByInvoiceId(id);
        invoiceMapper.deleteById(id);
    }

    // ==================== 勾稽 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InputInvoiceLinkRespVO linkToBiz(InputInvoiceLinkReqVO reqVO) {
        IcbcInputInvoiceDO invoice = getInvoice(reqVO.getInvoiceId());
        InputInvoiceBizTypeEnum bizType = InputInvoiceBizTypeEnum.ofType(reqVO.getBizType())
                .orElseThrow(() -> exception(INPUT_INVOICE_LINK_BIZ_TYPE_INVALID, reqVO.getBizType()));
        BigDecimal linkedAmount = reqVO.getLinkedAmount();
        if (linkedAmount == null || linkedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(INPUT_INVOICE_LINK_AMOUNT_INVALID);
        }
        // 单据号与单据金额以单据事实为准：采购订单按 id 取（#46 的只读视图 getOrderAmount），
        // 不采用调用方传入的值——金额上限的口径只有一处，客户端的值只做展示。
        String bizNo = StrUtil.trimToNull(reqVO.getBizNo());
        BigDecimal bizAmount = reqVO.getBizAmount();
        if (bizType == InputInvoiceBizTypeEnum.PURCHASE_ORDER) {
            PurchaseOrderAmountDTO order = purchaseOrderService.getOrderAmount(reqVO.getBizId());
            bizNo = order.getOrderNo();
            bizAmount = order.getTotalAmount();
        }
        if (bizAmount == null) {
            throw exception(INPUT_INVOICE_LINK_AMOUNT_INVALID);
        }
        if (linkMapper.selectByInvoiceAndBiz(invoice.getId(), bizType.getType(), reqVO.getBizId()) != null) {
            throw exception(INPUT_INVOICE_LINK_ALREADY_EXISTS,
                    StrUtil.blankToDefault(bizNo, String.valueOf(reqVO.getBizId())));
        }

        // 单据侧上限：同一张单据上的累计勾稽金额不得超过单据金额
        BigDecimal linkedOnBiz = sumLinkedAmount(linkMapper.selectListByBiz(bizType.getType(), reqVO.getBizId()));
        if (linkedAmount.compareTo(bizAmount.subtract(linkedOnBiz)) > 0) {
            throw exception(INPUT_INVOICE_LINK_EXCEED_BIZ_AMOUNT,
                    linkedAmount, bizAmount, linkedOnBiz);
        }
        // 发票侧上限：一张票的累计勾稽金额不得超过其价税合计
        BigDecimal linkedOnInvoice = invoice.getLinkedAmount() == null ? BigDecimal.ZERO : invoice.getLinkedAmount();
        if (linkedAmount.compareTo(invoice.getTotalAmount().subtract(linkedOnInvoice)) > 0) {
            throw exception(INPUT_INVOICE_LINK_EXCEED_INVOICE_AMOUNT,
                    linkedAmount, invoice.getTotalAmount(), linkedOnInvoice);
        }

        IcbcInputInvoiceLinkDO link = IcbcInputInvoiceLinkDO.builder()
                .invoiceId(invoice.getId())
                .bizType(bizType.getType())
                .bizId(reqVO.getBizId())
                .bizNo(bizNo)
                .bizAmount(bizAmount)
                .linkedAmount(linkedAmount)
                .remark(reqVO.getRemark())
                .build();
        linkMapper.insert(link);

        refreshInvoiceStatus(invoice, linkedOnInvoice.add(linkedAmount));
        return toLinkResp(link);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlink(Long linkId) {
        IcbcInputInvoiceLinkDO link = linkMapper.selectById(linkId);
        if (link == null) {
            throw exception(INPUT_INVOICE_LINK_NOT_EXISTS);
        }
        linkMapper.deleteById(linkId);
        IcbcInputInvoiceDO invoice = getInvoice(link.getInvoiceId());
        refreshInvoiceStatus(invoice, sumLinkedAmount(linkMapper.selectListByInvoiceId(invoice.getId())));
    }

    // ==================== 查询 ====================

    @Override
    public IcbcInputInvoiceDO getInvoice(Long id) {
        IcbcInputInvoiceDO invoice = invoiceMapper.selectById(id);
        if (invoice == null) {
            throw exception(INPUT_INVOICE_NOT_EXISTS);
        }
        return invoice;
    }

    @Override
    public InputInvoiceRespVO getDetail(Long id) {
        IcbcInputInvoiceDO invoice = getInvoice(id);
        InputInvoiceRespVO resp = toResp(invoice);
        resp.setLinks(getLinkList(id));
        return resp;
    }

    @Override
    public PageResult<InputInvoiceRespVO> getInvoicePage(InputInvoicePageReqVO pageReqVO) {
        PageResult<IcbcInputInvoiceDO> page = invoiceMapper.selectPage(pageReqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public List<InputInvoiceLinkRespVO> getLinkList(Long invoiceId) {
        return linkMapper.selectListByInvoiceId(invoiceId).stream().map(this::toLinkResp).toList();
    }

    // ==================== 内部 ====================

    private void assertInvoiceType(Integer invoiceType) {
        if (InputInvoiceTypeEnum.ofType(invoiceType).isEmpty()) {
            throw exception(INPUT_INVOICE_TYPE_INVALID, invoiceType);
        }
    }

    /** 仅「已登记」（还没有任何勾稽）的票可改可删；已勾稽的票改票面事实会让票、货、款对不上。 */
    private void assertEditable(IcbcInputInvoiceDO invoice) {
        if (!InputInvoiceStatusEnum.REGISTERED.getStatus().equals(invoice.getStatus())) {
            throw exception(INPUT_INVOICE_STATUS_NOT_ALLOW, InputInvoiceStatusEnum.nameOf(invoice.getStatus()));
        }
    }

    private void assertNotDuplicated(String sellerKey, String invoiceNo, Long excludeId) {
        IcbcInputInvoiceDO existing = invoiceMapper.selectBySellerKeyAndInvoiceNo(sellerKey, invoiceNo);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw exception(INPUT_INVOICE_DUPLICATED, existing.getSellerName(), invoiceNo);
        }
    }

    private void applyAmount(IcbcInputInvoiceDO invoice, InputInvoiceSaveReqVO reqVO) {
        BigDecimal amount = reqVO.getAmount();
        BigDecimal taxAmount = reqVO.getTaxAmount() == null ? BigDecimal.ZERO : reqVO.getTaxAmount();
        BigDecimal totalAmount = amount.add(taxAmount);
        if (amount.compareTo(BigDecimal.ZERO) < 0 || taxAmount.compareTo(BigDecimal.ZERO) < 0
                || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw exception(INPUT_INVOICE_AMOUNT_INVALID);
        }
        invoice.setAmount(amount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
    }

    /** 销方唯一标识：优先税号，没有税号时退化为名称（名称也进行了 trim）。 */
    private String sellerKeyOf(String sellerTaxNo, String sellerName) {
        return StrUtil.isNotBlank(sellerTaxNo) ? sellerTaxNo : sellerName;
    }

    /** 勾稽金额变化后重算并落库已勾稽合计与状态。 */
    private void refreshInvoiceStatus(IcbcInputInvoiceDO invoice, BigDecimal linkedAmount) {
        invoice.setLinkedAmount(linkedAmount);
        if (linkedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InputInvoiceStatusEnum.REGISTERED.getStatus());
        } else if (linkedAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InputInvoiceStatusEnum.LINKED.getStatus());
        } else {
            invoice.setStatus(InputInvoiceStatusEnum.PARTIALLY_LINKED.getStatus());
        }
        invoiceMapper.updateById(invoice);
    }

    private BigDecimal sumLinkedAmount(List<IcbcInputInvoiceLinkDO> links) {
        return links.stream().map(IcbcInputInvoiceLinkDO::getLinkedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private InputInvoiceRespVO toResp(IcbcInputInvoiceDO invoice) {
        InputInvoiceRespVO resp = BeanUtils.toBean(invoice, InputInvoiceRespVO.class);
        resp.setInvoiceTypeName(InputInvoiceTypeEnum.nameOf(invoice.getInvoiceType()));
        resp.setStatusName(InputInvoiceStatusEnum.nameOf(invoice.getStatus()));
        BigDecimal linked = invoice.getLinkedAmount() == null ? BigDecimal.ZERO : invoice.getLinkedAmount();
        resp.setRemainingAmount(invoice.getTotalAmount().subtract(linked));
        return resp;
    }

    private InputInvoiceLinkRespVO toLinkResp(IcbcInputInvoiceLinkDO link) {
        InputInvoiceLinkRespVO resp = BeanUtils.toBean(link, InputInvoiceLinkRespVO.class);
        resp.setBizTypeName(InputInvoiceBizTypeEnum.nameOf(link.getBizType()));
        return resp;
    }

}
