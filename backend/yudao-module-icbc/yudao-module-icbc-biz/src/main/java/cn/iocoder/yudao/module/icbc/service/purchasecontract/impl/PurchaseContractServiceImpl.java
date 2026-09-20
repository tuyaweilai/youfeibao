package cn.iocoder.yudao.module.icbc.service.purchasecontract.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractAuditReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractCategoryRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractCloseReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSubmitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractVersionRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractCategoryDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractVersionDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract.IcbcPurchaseContractCategoryMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract.IcbcPurchaseContractMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract.IcbcPurchaseContractVersionMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseContractAuditStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseContractStatusEnum;
import cn.iocoder.yudao.module.icbc.service.purchasecontract.PurchaseContractService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 采购合同 Service 实现（#45 / T07，ADR 0027）。
 *
 * <p>状态机只有一条主线：草稿 →（送审）待审核 →（通过）生效 →（关闭）关闭；
 * 驳回退回草稿，过期由有效期推导。**审核通过前不得作为采购依据**。
 */
@Service
@Validated
@Slf4j
public class PurchaseContractServiceImpl implements PurchaseContractService {

    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Resource
    private IcbcPurchaseContractMapper contractMapper;
    @Resource
    private IcbcPurchaseContractCategoryMapper categoryMapper;
    @Resource
    private IcbcPurchaseContractVersionMapper versionMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;

    // ==================== 写入 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createContract(PurchaseContractSaveReqVO createReqVO) {
        assertDateRange(createReqVO);
        IcbcPurchaseContractDO contract = BeanUtils.toBean(createReqVO, IcbcPurchaseContractDO.class);
        contract.setId(null);
        contract.setContractNo("PC" + LocalDateTime.now().format(NO_FORMATTER)
                + IdUtil.fastSimpleUUID().substring(0, 4).toUpperCase());
        contract.setStatus(PurchaseContractStatusEnum.DRAFT.getStatus());
        contract.setVersionNo(0);
        applyCounterparty(contract, createReqVO);
        contractMapper.insert(contract);
        replaceCategories(contract.getId(), createReqVO.getCategoryIds());
        return contract.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateContract(PurchaseContractSaveReqVO updateReqVO) {
        IcbcPurchaseContractDO contract = getContract(updateReqVO.getId());
        Integer status = contract.getStatus();
        boolean effective = PurchaseContractStatusEnum.EFFECTIVE.getStatus().equals(status);
        if (!effective && !PurchaseContractStatusEnum.DRAFT.getStatus().equals(status)) {
            throw exception(PURCHASE_CONTRACT_STATUS_NOT_ALLOW,
                    PurchaseContractStatusEnum.nameOf(status));
        }
        // 改已生效合同 = 提新版：必须说明改了什么，且重新审核通过前整份合同不再是采购依据
        if (effective && StrUtil.isBlank(updateReqVO.getChangeReason())) {
            throw exception(PURCHASE_CONTRACT_CHANGE_REASON_REQUIRED);
        }
        assertDateRange(updateReqVO);

        IcbcPurchaseContractDO update = BeanUtils.toBean(updateReqVO, IcbcPurchaseContractDO.class);
        contract.setName(update.getName());
        contract.setStartDate(update.getStartDate());
        contract.setEndDate(update.getEndDate());
        contract.setQuantityAgreement(update.getQuantityAgreement());
        contract.setMeasureStandard(update.getMeasureStandard());
        contract.setQualityStandard(update.getQualityStandard());
        contract.setPriceRule(update.getPriceRule());
        contract.setTransportResponsibility(update.getTransportResponsibility());
        contract.setPaymentTerms(update.getPaymentTerms());
        contract.setAttachmentUrls(update.getAttachmentUrls());
        contract.setRemark(update.getRemark());
        applyCounterparty(contract, updateReqVO);
        if (effective) {
            markPendingAudit(contract);
        }
        contractMapper.updateById(contract);
        replaceCategories(contract.getId(), updateReqVO.getCategoryIds());
        if (effective) {
            createVersion(contract, updateReqVO.getChangeReason());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForAudit(PurchaseContractSubmitReqVO submitReqVO) {
        IcbcPurchaseContractDO contract = getContract(submitReqVO.getId());
        if (!PurchaseContractStatusEnum.DRAFT.getStatus().equals(contract.getStatus())) {
            throw exception(PURCHASE_CONTRACT_STATUS_NOT_ALLOW,
                    PurchaseContractStatusEnum.nameOf(contract.getStatus()));
        }
        markPendingAudit(contract);
        contractMapper.updateById(contract);
        createVersion(contract, submitReqVO.getChangeReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(PurchaseContractAuditReqVO auditReqVO) {
        IcbcPurchaseContractDO contract = getContract(auditReqVO.getId());
        if (!PurchaseContractStatusEnum.PENDING_AUDIT.getStatus().equals(contract.getStatus())) {
            throw exception(PURCHASE_CONTRACT_STATUS_NOT_ALLOW,
                    PurchaseContractStatusEnum.nameOf(contract.getStatus()));
        }
        boolean approved = Boolean.TRUE.equals(auditReqVO.getApproved());
        if (!approved && StrUtil.isBlank(auditReqVO.getRemark())) {
            throw exception(PURCHASE_CONTRACT_AUDIT_REMARK_REQUIRED);
        }
        contract.setStatus(approved ? PurchaseContractStatusEnum.EFFECTIVE.getStatus()
                : PurchaseContractStatusEnum.DRAFT.getStatus());
        contract.setAuditedBy(SecurityFrameworkUtils.getLoginUserId());
        contract.setAuditedTime(LocalDateTime.now());
        contract.setAuditRemark(auditReqVO.getRemark());
        contractMapper.updateById(contract);

        // 审核结论写在被审的那一版上，历史版本仍可回查
        IcbcPurchaseContractVersionDO latest = versionMapper.selectLatest(contract.getId());
        if (latest != null) {
            latest.setAuditStatus(approved ? PurchaseContractAuditStatusEnum.APPROVED.getStatus()
                    : PurchaseContractAuditStatusEnum.REJECTED.getStatus());
            latest.setAuditedBy(SecurityFrameworkUtils.getLoginUserId());
            latest.setAuditedTime(LocalDateTime.now());
            latest.setAuditRemark(auditReqVO.getRemark());
            versionMapper.updateById(latest);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeContract(PurchaseContractCloseReqVO closeReqVO) {
        IcbcPurchaseContractDO contract = getContract(closeReqVO.getId());
        if (!PurchaseContractStatusEnum.EFFECTIVE.getStatus().equals(contract.getStatus())) {
            throw exception(PURCHASE_CONTRACT_STATUS_NOT_ALLOW,
                    PurchaseContractStatusEnum.nameOf(contract.getStatus()));
        }
        contract.setStatus(PurchaseContractStatusEnum.CLOSED.getStatus());
        contract.setClosedBy(SecurityFrameworkUtils.getLoginUserId());
        contract.setClosedTime(LocalDateTime.now());
        contract.setCloseReason(closeReqVO.getReason());
        contractMapper.updateById(contract);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteContract(Long id) {
        IcbcPurchaseContractDO contract = getContract(id);
        if (!PurchaseContractStatusEnum.DRAFT.getStatus().equals(contract.getStatus())) {
            throw exception(PURCHASE_CONTRACT_STATUS_NOT_ALLOW,
                    PurchaseContractStatusEnum.nameOf(contract.getStatus()));
        }
        categoryMapper.deleteByContractId(id);
        versionMapper.deleteByContractId(id);
        contractMapper.deleteById(id);
    }

    // ==================== 查询 ====================

    @Override
    public IcbcPurchaseContractDO getContract(Long id) {
        IcbcPurchaseContractDO contract = contractMapper.selectById(id);
        if (contract == null) {
            throw exception(PURCHASE_CONTRACT_NOT_EXISTS);
        }
        return contract;
    }

    @Override
    public PurchaseContractRespVO getDetail(Long id) {
        IcbcPurchaseContractDO contract = getContract(id);
        PurchaseContractRespVO resp = toResp(contract);
        resp.setCategories(toCategoryResp(categoryMapper.selectListByContractId(id)));
        resp.setVersions(versionMapper.selectListByContractId(id).stream()
                .map(this::toVersionResp).toList());
        return resp;
    }

    @Override
    public PageResult<PurchaseContractRespVO> getContractPage(PurchaseContractPageReqVO pageReqVO) {
        PageResult<IcbcPurchaseContractDO> page = contractMapper.selectPage(pageReqVO);
        return new PageResult<>(page.getList().stream().map(this::toResp).toList(), page.getTotal());
    }

    @Override
    public IcbcPurchaseContractDO assertUsableAsPurchaseBasis(Long id) {
        IcbcPurchaseContractDO contract = getContract(id);
        if (!PurchaseContractStatusEnum.EFFECTIVE.getStatus().equals(contract.getStatus())
                || isExpired(contract)) {
            throw exception(PURCHASE_CONTRACT_NOT_EFFECTIVE, contract.getContractNo());
        }
        return contract;
    }

    // ==================== 内部 ====================

    /**
     * 「双方」里的对手方：按主体类型六态落到其中一个档案，恰好一个非空（ADR 0029）。
     *
     * <p>自然人出售者取 {@code icbc_payee_info} 并校验归属本租户；非自然人的单位供货方在 ERP 侧
     * （{@code erp_supplier}），前端从供货方下拉选择后把编号与名称快照一并带上——ERP 与 icbc 之间
     * 只有 {@code erp-api} 一条通路，本合同侧不反向依赖 ERP 实现。
     */
    private void applyCounterparty(IcbcPurchaseContractDO contract, PurchaseContractSaveReqVO reqVO) {
        SellerSubjectTypeEnum subjectType = SellerSubjectTypeEnum.valueOf(reqVO.getCounterpartyType());
        if (subjectType == null) {
            throw exception(PURCHASE_CONTRACT_COUNTERPARTY_REQUIRED);
        }
        contract.setCounterpartyType(subjectType.getType());
        if (subjectType.isNatural()) {
            if (reqVO.getPayeeId() == null || reqVO.getSupplierId() != null) {
                throw exception(PURCHASE_CONTRACT_COUNTERPARTY_REQUIRED);
            }
            PayeeInfoDO payee = payeeInfoMapper.selectById(reqVO.getPayeeId());
            if (payee == null) {
                throw exception(PURCHASE_CONTRACT_PAYEE_NOT_EXISTS);
            }
            contract.setPayeeId(payee.getId());
            contract.setSupplierId(null);
            contract.setCounterpartyName(payee.getName());
        } else {
            if (reqVO.getSupplierId() == null || reqVO.getPayeeId() != null) {
                throw exception(PURCHASE_CONTRACT_COUNTERPARTY_REQUIRED);
            }
            if (StrUtil.isBlank(reqVO.getCounterpartyName())) {
                throw exception(PURCHASE_CONTRACT_SUPPLIER_NAME_REQUIRED);
            }
            contract.setPayeeId(null);
            contract.setSupplierId(reqVO.getSupplierId());
            contract.setCounterpartyName(reqVO.getCounterpartyName().trim());
        }
    }

    /** 适用品类：至少一个，逐个校验属于本租户的编码配置，并落名称 / 单位快照。 */
    private void replaceCategories(Long contractId, List<Long> categoryIds) {
        if (CollUtil.isEmpty(categoryIds)) {
            throw exception(PURCHASE_CONTRACT_CATEGORY_REQUIRED);
        }
        categoryMapper.deleteByContractId(contractId);
        for (Long goodsConfigId : new LinkedHashSet<>(categoryIds)) {
            IcbcGoodsConfigDO goodsConfig = goodsConfigMapper.selectById(goodsConfigId);
            if (goodsConfig == null) {
                throw exception(PURCHASE_CONTRACT_CATEGORY_NOT_EXISTS, goodsConfigId);
            }
            categoryMapper.insert(IcbcPurchaseContractCategoryDO.builder()
                    .contractId(contractId)
                    .goodsConfigId(goodsConfig.getId())
                    .categoryName(goodsConfig.getName())
                    .unit(goodsConfig.getUnit())
                    .build());
        }
    }

    /** 送审 / 提新版的同一步：版本号 +1、回到待审核、落送审人与时间（送审前不得作为采购依据）。 */
    private void markPendingAudit(IcbcPurchaseContractDO contract) {
        contract.setVersionNo((contract.getVersionNo() == null ? 0 : contract.getVersionNo()) + 1);
        contract.setStatus(PurchaseContractStatusEnum.PENDING_AUDIT.getStatus());
        contract.setSubmittedBy(SecurityFrameworkUtils.getLoginUserId());
        contract.setSubmittedTime(LocalDateTime.now());
    }

    private void assertDateRange(PurchaseContractSaveReqVO reqVO) {
        if (reqVO.getStartDate() != null && reqVO.getEndDate() != null
                && reqVO.getEndDate().isBefore(reqVO.getStartDate())) {
            throw exception(PURCHASE_CONTRACT_DATE_INVALID);
        }
    }

    /**
     * 落一版整份合同快照（含适用品类），版本只追加、不覆盖；快照哈希证明历史版本未被改动。
     */
    private void createVersion(IcbcPurchaseContractDO contract, String changeReason) {
        List<IcbcPurchaseContractCategoryDO> categories =
                categoryMapper.selectListByContractId(contract.getId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractId", contract.getId());
        snapshot.put("contractNo", contract.getContractNo());
        snapshot.put("name", contract.getName());
        snapshot.put("counterpartyType", contract.getCounterpartyType());
        snapshot.put("counterpartyName", contract.getCounterpartyName());
        snapshot.put("payeeId", contract.getPayeeId());
        snapshot.put("supplierId", contract.getSupplierId());
        snapshot.put("startDate", contract.getStartDate());
        snapshot.put("endDate", contract.getEndDate());
        snapshot.put("quantityAgreement", contract.getQuantityAgreement());
        snapshot.put("measureStandard", contract.getMeasureStandard());
        snapshot.put("qualityStandard", contract.getQualityStandard());
        snapshot.put("priceRule", contract.getPriceRule());
        snapshot.put("transportResponsibility", contract.getTransportResponsibility());
        snapshot.put("paymentTerms", contract.getPaymentTerms());
        snapshot.put("attachmentUrls", contract.getAttachmentUrls());
        snapshot.put("categories", categories.stream().map(category -> {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("goodsConfigId", category.getGoodsConfigId());
            line.put("categoryName", category.getCategoryName());
            line.put("unit", category.getUnit());
            return line;
        }).toList());
        String json = JSON.toJSONString(snapshot);
        versionMapper.insert(IcbcPurchaseContractVersionDO.builder()
                .contractId(contract.getId())
                .versionNo(contract.getVersionNo())
                .snapshotJson(json)
                .snapshotHash(DigestUtil.sha256Hex(json))
                .changeReason(changeReason)
                .changedBy(currentUser())
                .auditStatus(PurchaseContractAuditStatusEnum.PENDING.getStatus())
                .build());
    }

    private boolean isExpired(IcbcPurchaseContractDO contract) {
        return contract.getEndDate() != null && contract.getEndDate().isBefore(LocalDate.now());
    }

    private String currentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? "system" : String.valueOf(userId);
    }

    // ==================== 映射 ====================

    private PurchaseContractRespVO toResp(IcbcPurchaseContractDO contract) {
        PurchaseContractRespVO resp = BeanUtils.toBean(contract, PurchaseContractRespVO.class);
        resp.setCounterpartyTypeName(SellerSubjectTypeEnum.nameOf(contract.getCounterpartyType()));
        boolean expired = isExpired(contract);
        resp.setExpired(expired);
        boolean effective = PurchaseContractStatusEnum.EFFECTIVE.getStatus().equals(contract.getStatus());
        resp.setUsableAsPurchaseBasis(effective && !expired);
        resp.setStatusName(expired && effective ? PurchaseContractStatusEnum.EXPIRED.getName()
                : PurchaseContractStatusEnum.nameOf(contract.getStatus()));
        resp.setCategories(toCategoryResp(categoryMapper.selectListByContractId(contract.getId())));
        return resp;
    }

    private List<PurchaseContractCategoryRespVO> toCategoryResp(List<IcbcPurchaseContractCategoryDO> categories) {
        return categories.stream().map(category -> BeanUtils.toBean(category, PurchaseContractCategoryRespVO.class))
                .toList();
    }

    private PurchaseContractVersionRespVO toVersionResp(IcbcPurchaseContractVersionDO version) {
        PurchaseContractVersionRespVO resp = BeanUtils.toBean(version, PurchaseContractVersionRespVO.class);
        resp.setAuditStatusName(PurchaseContractAuditStatusEnum.nameOf(version.getAuditStatus()));
        return resp;
    }

}
