package cn.iocoder.yudao.module.icbc.service.purchasecontract;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractAuditReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractCloseReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSubmitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractVersionRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.dal.mysql.purchasecontract.IcbcPurchaseContractMapper;
import cn.iocoder.yudao.module.icbc.enums.PurchaseContractAuditStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PurchaseContractStatusEnum;
import cn.iocoder.yudao.module.icbc.service.purchasecontract.impl.PurchaseContractServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PurchaseContractServiceImpl} 的单元测试（#45 / T07）。
 *
 * <p>覆盖验收：字段齐全（双方 / 有效期 / 适用品类 / 数量约定 / 计量与质量标准 / 价格规则 /
 * 运输责任 / 付款条款 / 附件）、状态流转（草稿 → 待审核 → 生效 → 关闭 / 过期）、
 * **未审核不得作为采购依据**、变更留版本且历史可回查。
 */
@Import({PurchaseContractServiceImpl.class, UnitTestConfiguration.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@Rollback
public class PurchaseContractServiceTest extends BaseDbUnitTest {

    @Resource
    private PurchaseContractService purchaseContractService;
    @Resource
    private IcbcPurchaseContractMapper contractMapper;
    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;
    @Resource
    private PayeeInfoMapper payeeInfoMapper;

    // ==================== 草稿与门禁 ====================

    @Test
    public void testCreate_isDraftAndNotUsableAsBasis() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");

        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));

        IcbcPurchaseContractDO contract = purchaseContractService.getContract(id);
        assertEquals(PurchaseContractStatusEnum.DRAFT.getStatus(), contract.getStatus());
        assertEquals(0, contract.getVersionNo());
        assertEquals("张三", contract.getCounterpartyName());
        assertNotNull(contract.getContractNo());
        // 未审核：不得作为采购依据
        assertServiceException(() -> purchaseContractService.assertUsableAsPurchaseBasis(id),
                PURCHASE_CONTRACT_NOT_EFFECTIVE, contract.getContractNo());
    }

    @Test
    public void testCreate_detailCarriesAllAgreedFields() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PurchaseContractSaveReqVO reqVO = naturalReq(payee.getId(), goodsId);
        reqVO.setQuantityAgreement("全年不少于 1000 吨");
        reqVO.setMeasureStandard("以场站地磅毛重减皮重再减扣杂为准");
        reqVO.setQualityStandard("废钢二级，杂质不超过 1%");
        reqVO.setPriceRule("按交货日挂牌价减 20 元/吨");
        reqVO.setTransportResponsibility("供方送货到场站，运费自担");
        reqVO.setPaymentTerms("结算确认后 3 个工作日内付款");
        reqVO.setAttachmentUrls("https://cdn/a.pdf,https://cdn/b.jpg");

        Long id = purchaseContractService.createContract(reqVO);

        PurchaseContractRespVO detail = purchaseContractService.getDetail(id);
        assertEquals("全年不少于 1000 吨", detail.getQuantityAgreement());
        assertEquals("以场站地磅毛重减皮重再减扣杂为准", detail.getMeasureStandard());
        assertEquals("废钢二级，杂质不超过 1%", detail.getQualityStandard());
        assertEquals("按交货日挂牌价减 20 元/吨", detail.getPriceRule());
        assertEquals("供方送货到场站，运费自担", detail.getTransportResponsibility());
        assertEquals("结算确认后 3 个工作日内付款", detail.getPaymentTerms());
        assertEquals("https://cdn/a.pdf,https://cdn/b.jpg", detail.getAttachmentUrls());
        assertEquals(1, detail.getCategories().size());
        assertEquals("废钢", detail.getCategories().get(0).getCategoryName());
        assertEquals("吨", detail.getCategories().get(0).getUnit());
    }

    // ==================== 状态流转 ====================

    @Test
    public void testSubmitThenAuditPass_effectiveAndUsableAsBasis() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PayeeInfoDO payee = insertPayee("张三");
        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));

        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));
        IcbcPurchaseContractDO pending = purchaseContractService.getContract(id);
        assertEquals(PurchaseContractStatusEnum.PENDING_AUDIT.getStatus(), pending.getStatus());
        assertEquals(1, pending.getVersionNo());
        assertServiceException(() -> purchaseContractService.assertUsableAsPurchaseBasis(id),
                PURCHASE_CONTRACT_NOT_EFFECTIVE, pending.getContractNo());

        purchaseContractService.audit(auditReq(id, true, "条款核对无误"));
        assertEquals(PurchaseContractStatusEnum.EFFECTIVE.getStatus(),
                purchaseContractService.getContract(id).getStatus());
        // 审核通过后才可作为采购依据
        assertNotNull(purchaseContractService.assertUsableAsPurchaseBasis(id));
    }

    @Test
    public void testAuditReject_requiresRemarkAndReturnsToDraft() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PayeeInfoDO payee = insertPayee("张三");
        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));
        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));

        assertServiceException(() -> purchaseContractService.audit(auditReq(id, false, "  ")),
                PURCHASE_CONTRACT_AUDIT_REMARK_REQUIRED);

        purchaseContractService.audit(auditReq(id, false, "付款条款与财务口径不符"));
        assertEquals(PurchaseContractStatusEnum.DRAFT.getStatus(),
                purchaseContractService.getContract(id).getStatus());
        // 驳回后仍不能作为采购依据
        assertServiceException(() -> purchaseContractService.assertUsableAsPurchaseBasis(id),
                PURCHASE_CONTRACT_NOT_EFFECTIVE,
                purchaseContractService.getContract(id).getContractNo());
    }

    @Test
    public void testEditWithdrawnVersion_resubmitIncrementsVersion() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PayeeInfoDO payee = insertPayee("张三");
        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));
        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));
        purchaseContractService.audit(auditReq(id, false, "价格规则没写清"));

        // 退回草稿后直接改，不产生新版本
        PurchaseContractSaveReqVO update = naturalReq(payee.getId(), goodsId);
        update.setId(id);
        update.setPriceRule("按交货日挂牌价");
        purchaseContractService.updateContract(update);
        assertEquals(1, purchaseContractService.getContract(id).getVersionNo());

        purchaseContractService.submitForAudit(submitReq(id, "按审核意见补充价格规则"));
        assertEquals(2, purchaseContractService.getContract(id).getVersionNo());
        purchaseContractService.audit(auditReq(id, true, "通过"));
        assertEquals(PurchaseContractStatusEnum.EFFECTIVE.getStatus(),
                purchaseContractService.getContract(id).getStatus());
        assertEquals(2, purchaseContractService.getDetail(id).getVersions().size());
    }

    @Test
    public void testClose_effectiveOnlyAndNoLongerBasis() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));

        // 草稿不能直接关闭
        PurchaseContractCloseReqVO close = new PurchaseContractCloseReqVO();
        close.setId(id);
        assertServiceException(() -> purchaseContractService.closeContract(close),
                PURCHASE_CONTRACT_STATUS_NOT_ALLOW, PurchaseContractStatusEnum.DRAFT.getName());

        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));
        purchaseContractService.audit(auditReq(id, true, "通过"));
        close.setReason("供应商已停止合作");
        purchaseContractService.closeContract(close);

        assertEquals(PurchaseContractStatusEnum.CLOSED.getStatus(),
                purchaseContractService.getContract(id).getStatus());
        assertServiceException(() -> purchaseContractService.assertUsableAsPurchaseBasis(id),
                PURCHASE_CONTRACT_NOT_EFFECTIVE,
                purchaseContractService.getContract(id).getContractNo());
    }

    @Test
    public void testExpired_derivedAndNotUsableAsBasis() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PurchaseContractSaveReqVO reqVO = naturalReq(payee.getId(), goodsId);
        reqVO.setStartDate(LocalDate.now().minusDays(10));
        reqVO.setEndDate(LocalDate.now().minusDays(1));
        Long id = purchaseContractService.createContract(reqVO);
        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));
        purchaseContractService.audit(auditReq(id, true, "通过"));

        // 过期由「已生效 + 有效期止早于今天」推导，不落库
        PurchaseContractRespVO detail = purchaseContractService.getDetail(id);
        assertTrue(detail.getExpired());
        assertEquals(PurchaseContractStatusEnum.EXPIRED.getName(), detail.getStatusName());
        assertFalse(detail.getUsableAsPurchaseBasis());
        assertEquals(PurchaseContractStatusEnum.EFFECTIVE.getStatus(), detail.getStatus());
        assertServiceException(() -> purchaseContractService.assertUsableAsPurchaseBasis(id),
                PURCHASE_CONTRACT_NOT_EFFECTIVE,
                purchaseContractService.getContract(id).getContractNo());
    }

    // ==================== 变更留版本 ====================

    @Test
    public void testChangeEffectiveContract_newVersionAndNeedsReaudit() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));
        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));
        purchaseContractService.audit(auditReq(id, true, "通过"));

        // 改已生效合同：必须写清改了什么
        PurchaseContractSaveReqVO update = naturalReq(payee.getId(), goodsId);
        update.setId(id);
        update.setPriceRule("按交货日挂牌价减 30 元/吨");
        assertServiceException(() -> purchaseContractService.updateContract(update),
                PURCHASE_CONTRACT_CHANGE_REASON_REQUIRED);

        update.setChangeReason("市场下行，下调 10 元/吨");
        purchaseContractService.updateContract(update);

        IcbcPurchaseContractDO changed = purchaseContractService.getContract(id);
        assertEquals(PurchaseContractStatusEnum.PENDING_AUDIT.getStatus(), changed.getStatus());
        assertEquals(2, changed.getVersionNo());
        // 新版未审核前整份合同不再是有效采购依据
        assertServiceException(() -> purchaseContractService.assertUsableAsPurchaseBasis(id),
                PURCHASE_CONTRACT_NOT_EFFECTIVE, changed.getContractNo());

        purchaseContractService.audit(auditReq(id, true, "同意调整"));
        IcbcPurchaseContractDO back = purchaseContractService.getContract(id);
        assertEquals(PurchaseContractStatusEnum.EFFECTIVE.getStatus(), back.getStatus());
        assertNotNull(purchaseContractService.assertUsableAsPurchaseBasis(id));
        assertEquals("按交货日挂牌价减 30 元/吨",
                purchaseContractService.getDetail(id).getPriceRule());
    }

    @Test
    public void testVersionHistory_keepsEverySubmittedVersionWithAuditResult() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long id = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));
        purchaseContractService.submitForAudit(submitReq(id, "首次送审"));
        purchaseContractService.audit(auditReq(id, false, "条款不全"));

        PurchaseContractSaveReqVO update = naturalReq(payee.getId(), goodsId);
        update.setId(id);
        update.setPaymentTerms("结算确认后 5 个工作日内付款");
        purchaseContractService.updateContract(update);
        purchaseContractService.submitForAudit(submitReq(id, "按驳回意见补充后重新送审"));
        purchaseContractService.audit(auditReq(id, true, "通过"));

        List<PurchaseContractVersionRespVO> versions =
                purchaseContractService.getDetail(id).getVersions();
        assertEquals(2, versions.size());
        // 版本倒序：最新在前
        assertEquals(2, versions.get(0).getVersionNo());
        assertEquals(PurchaseContractAuditStatusEnum.APPROVED.getStatus(), versions.get(0).getAuditStatus());
        assertEquals("按驳回意见补充后重新送审", versions.get(0).getChangeReason());
        assertEquals(1, versions.get(1).getVersionNo());
        assertEquals(PurchaseContractAuditStatusEnum.REJECTED.getStatus(), versions.get(1).getAuditStatus());
        assertEquals("条款不全", versions.get(1).getAuditRemark());
        // 每一版都有独立快照哈希，历史版本可回查
        assertNotNull(versions.get(0).getSnapshotHash());
        assertNotNull(versions.get(1).getSnapshotHash());
        assertNotEquals(versions.get(0).getSnapshotHash(), versions.get(1).getSnapshotHash());
    }

    // ==================== 双方 / 品类 / 有效期校验 ====================

    @Test
    public void testCounterparty_naturalAndSupplierExactlyOne() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PayeeInfoDO payee = insertPayee("张三");

        // 自然人主体却同时给了供货方：拒
        PurchaseContractSaveReqVO both = naturalReq(payee.getId(), goodsId);
        both.setSupplierId(1001L);
        assertServiceException(() -> purchaseContractService.createContract(both),
                PURCHASE_CONTRACT_COUNTERPARTY_REQUIRED);

        // 自然人主体却没给出售者档案：拒
        PurchaseContractSaveReqVO none = naturalReq(payee.getId(), goodsId);
        none.setPayeeId(null);
        assertServiceException(() -> purchaseContractService.createContract(none),
                PURCHASE_CONTRACT_COUNTERPARTY_REQUIRED);

        // 出售者档案不存在：拒
        PurchaseContractSaveReqVO missing = naturalReq(999999L, goodsId);
        assertServiceException(() -> purchaseContractService.createContract(missing),
                PURCHASE_CONTRACT_PAYEE_NOT_EXISTS);
    }

    @Test
    public void testCounterparty_supplierNeedsNameAndCanSwitchToNatural() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PurchaseContractSaveReqVO supplierReq = supplierReq(2002L, "某某再生资源有限公司", goodsId);
        supplierReq.setCounterpartyName("  ");
        assertServiceException(() -> purchaseContractService.createContract(supplierReq),
                PURCHASE_CONTRACT_SUPPLIER_NAME_REQUIRED);

        supplierReq.setCounterpartyName("某某再生资源有限公司");
        Long id = purchaseContractService.createContract(supplierReq);
        IcbcPurchaseContractDO supplierContract = purchaseContractService.getContract(id);
        assertEquals(2002L, supplierContract.getSupplierId());
        assertNull(supplierContract.getPayeeId());
        assertEquals("某某再生资源有限公司", supplierContract.getCounterpartyName());

        // 换成自然人出手：另一个 id 必须被真正清空（updateStrategy = ALWAYS）
        PayeeInfoDO payee = insertPayee("李四");
        PurchaseContractSaveReqVO update = naturalReq(payee.getId(), goodsId);
        update.setId(id);
        purchaseContractService.updateContract(update);
        IcbcPurchaseContractDO switched = purchaseContractService.getContract(id);
        assertEquals(payee.getId(), switched.getPayeeId());
        assertNull(switched.getSupplierId());
        assertEquals("李四", switched.getCounterpartyName());
        assertEquals(1, switched.getCounterpartyType());
    }

    @Test
    public void testDateRange_endBeforeStartRejected() {
        Long goodsId = insertGoodsConfig("废钢", "吨");
        PayeeInfoDO payee = insertPayee("张三");
        PurchaseContractSaveReqVO reqVO = naturalReq(payee.getId(), goodsId);
        reqVO.setStartDate(LocalDate.now().plusDays(1));
        reqVO.setEndDate(LocalDate.now());
        assertServiceException(() -> purchaseContractService.createContract(reqVO),
                PURCHASE_CONTRACT_DATE_INVALID);
    }

    @Test
    public void testCategories_requiredAndMustBelongToTenant() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");

        PurchaseContractSaveReqVO empty = naturalReq(payee.getId(), goodsId);
        empty.setCategoryIds(List.of());
        assertServiceException(() -> purchaseContractService.createContract(empty),
                PURCHASE_CONTRACT_CATEGORY_REQUIRED);

        PurchaseContractSaveReqVO missing = naturalReq(payee.getId(), goodsId);
        missing.setCategoryIds(List.of(888888L));
        assertServiceException(() -> purchaseContractService.createContract(missing),
                PURCHASE_CONTRACT_CATEGORY_NOT_EXISTS, 888888L);
    }

    // ==================== 删除 ====================

    @Test
    public void testDelete_draftOnly() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long draftId = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));

        Long effectiveId = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));
        purchaseContractService.submitForAudit(submitReq(effectiveId, "首次送审"));
        purchaseContractService.audit(auditReq(effectiveId, true, "通过"));

        assertServiceException(() -> purchaseContractService.deleteContract(effectiveId),
                PURCHASE_CONTRACT_STATUS_NOT_ALLOW, PurchaseContractStatusEnum.EFFECTIVE.getName());

        purchaseContractService.deleteContract(draftId);
        assertServiceException(() -> purchaseContractService.getContract(draftId),
                PURCHASE_CONTRACT_NOT_EXISTS);
        // 已生效的仍在
        assertNotNull(purchaseContractService.getContract(effectiveId));
    }

    // ==================== 分页 ====================

    @Test
    public void testPage_filterByStatusAndCounterparty() {
        PayeeInfoDO payee = insertPayee("张三");
        Long goodsId = insertGoodsConfig("废钢", "吨");
        Long draftId = purchaseContractService.createContract(naturalReq(payee.getId(), goodsId));
        Long effectiveId = purchaseContractService.createContract(supplierReq(2002L, "某某再生资源", goodsId));
        purchaseContractService.submitForAudit(submitReq(effectiveId, "首次送审"));
        purchaseContractService.audit(auditReq(effectiveId, true, "通过"));

        PurchaseContractPageReqVO draftQuery = new PurchaseContractPageReqVO();
        draftQuery.setStatus(PurchaseContractStatusEnum.DRAFT.getStatus());
        assertEquals(1, purchaseContractService.getContractPage(draftQuery).getTotal());
        assertEquals(draftId, purchaseContractService.getContractPage(draftQuery).getList().get(0).getId());

        PurchaseContractPageReqVO supplierQuery = new PurchaseContractPageReqVO();
        supplierQuery.setCounterpartyType(5);
        assertEquals(1, purchaseContractService.getContractPage(supplierQuery).getTotal());
        assertEquals("某某再生资源",
                purchaseContractService.getContractPage(supplierQuery).getList().get(0).getCounterpartyName());
    }

    // ==================== 造数 ====================

    private PurchaseContractSaveReqVO naturalReq(Long payeeId, Long goodsId) {
        PurchaseContractSaveReqVO reqVO = new PurchaseContractSaveReqVO();
        reqVO.setName("2026 年度废钢采购合同");
        reqVO.setCounterpartyType(1);
        reqVO.setPayeeId(payeeId);
        reqVO.setStartDate(LocalDate.now().minusDays(1));
        reqVO.setEndDate(LocalDate.now().plusDays(365));
        reqVO.setQuantityAgreement("不少于 1000 吨");
        reqVO.setCategoryIds(List.of(goodsId));
        return reqVO;
    }

    private PurchaseContractSaveReqVO supplierReq(Long supplierId, String name, Long goodsId) {
        PurchaseContractSaveReqVO reqVO = new PurchaseContractSaveReqVO();
        reqVO.setName("2026 年度废钢采购合同（单位）");
        reqVO.setCounterpartyType(5);
        reqVO.setSupplierId(supplierId);
        reqVO.setCounterpartyName(name);
        reqVO.setStartDate(LocalDate.now().minusDays(1));
        reqVO.setEndDate(LocalDate.now().plusDays(365));
        reqVO.setCategoryIds(List.of(goodsId));
        return reqVO;
    }

    private PurchaseContractSubmitReqVO submitReq(Long id, String changeReason) {
        PurchaseContractSubmitReqVO reqVO = new PurchaseContractSubmitReqVO();
        reqVO.setId(id);
        reqVO.setChangeReason(changeReason);
        return reqVO;
    }

    private PurchaseContractAuditReqVO auditReq(Long id, boolean approved, String remark) {
        PurchaseContractAuditReqVO reqVO = new PurchaseContractAuditReqVO();
        reqVO.setId(id);
        reqVO.setApproved(approved);
        reqVO.setRemark(remark);
        return reqVO;
    }

    private PayeeInfoDO insertPayee(String name) {
        PayeeInfoDO payee = PayeeInfoDO.builder()
                .partnerPayeeId("PARTNER_" + name)
                .name(name)
                .mobile("13800138000")
                .idCardNo("110101199001011234")
                .build();
        payeeInfoMapper.insert(payee);
        return payee;
    }

    private Long insertGoodsConfig(String name, String unit) {
        IcbcGoodsConfigDO goodsConfig = new IcbcGoodsConfigDO();
        goodsConfig.setName(name);
        goodsConfig.setUnit(unit);
        goodsConfig.setStatus(0);
        goodsConfigMapper.insert(goodsConfig);
        return goodsConfig.getId();
    }

}
