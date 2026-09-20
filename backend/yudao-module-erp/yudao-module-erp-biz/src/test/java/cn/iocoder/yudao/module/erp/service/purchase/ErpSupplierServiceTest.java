package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.erp.UnitTestConfiguration;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.TaxpayerQualificationEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_SUBJECT_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_SUBJECT_TYPE_NATURAL_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_SUBJECT_TYPE_REQUIRED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ErpSupplierServiceImpl} 的单元测试（T06 / #44 单位供货方档案）。
 *
 * <p>锁住四件事：六态主体类型可维护（自然人除外）；税号 / 纳税人资格 / 对公账户 / 地址 / 联系人可维护；
 * 被采购单据引用时不可删；只有自然人以外的五类能建档。
 */
@Import({ErpSupplierServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class ErpSupplierServiceTest extends BaseDbUnitTest {

    @Resource
    private ErpSupplierServiceImpl supplierService;
    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;

    @Test
    public void testCreateUpdateGetAndPage() {
        // 创建：个体工商户，税号 / 纳税人资格 / 对公账户 / 地址 / 联系人齐备
        Long id = supplierService.createSupplier(buildReq(null, "某某再生资源经营部",
                SellerSubjectTypeEnum.INDIVIDUAL_BUSINESS.getType()));
        ErpSupplierDO supplier = supplierService.getSupplier(id);
        assertEquals(SellerSubjectTypeEnum.INDIVIDUAL_BUSINESS.getType(), supplier.getSubjectType());
        assertEquals(TaxpayerQualificationEnum.SMALL_SCALE.getQualification(), supplier.getTaxpayerQualification());
        assertEquals("91310000MA1FL0000X", supplier.getTaxNo());
        assertEquals("上海市浦东新区张江镇 1 号", supplier.getAddress());
        assertEquals("李四", supplier.getContact());
        assertEquals("622908212277228617", supplier.getBankAccount());

        // 更新：换成企业法人，主体类型与地址可改
        ErpSupplierSaveReqVO updateReq = buildReq(id, "某某环保集团有限公司",
                SellerSubjectTypeEnum.ENTERPRISE_LEGAL.getType());
        updateReq.setAddress("上海市宝山区罗店镇 2 号");
        supplierService.updateSupplier(updateReq);
        ErpSupplierDO updated = supplierService.getSupplier(id);
        assertEquals(SellerSubjectTypeEnum.ENTERPRISE_LEGAL.getType(), updated.getSubjectType());
        assertEquals("上海市宝山区罗店镇 2 号", updated.getAddress());

        // 分页按主体类型筛选
        ErpSupplierPageReqVO pageReqVO = new ErpSupplierPageReqVO();
        pageReqVO.setSubjectType(SellerSubjectTypeEnum.ENTERPRISE_LEGAL.getType());
        PageResult<ErpSupplierDO> page = supplierService.getSupplierPage(pageReqVO);
        assertEquals(1L, page.getTotal());

        ErpSupplierPageReqVO otherPageReqVO = new ErpSupplierPageReqVO();
        otherPageReqVO.setSubjectType(SellerSubjectTypeEnum.FARMER_COOPERATIVE.getType());
        assertEquals(0L, supplierService.getSupplierPage(otherPageReqVO).getTotal());
    }

    @Test
    public void testCreate_naturalPersonRejected() {
        // 自然人出售者的档案在 icbc_payee_info，不能复制进单位供货方（ADR 0027）
        assertServiceException(
                () -> supplierService.createSupplier(buildReq(null, "张三",
                        SellerSubjectTypeEnum.NATURAL.getType())),
                SUPPLIER_SUBJECT_TYPE_NATURAL_NOT_ALLOWED);
    }

    @Test
    public void testUpdate_naturalPersonRejected() {
        Long id = supplierService.createSupplier(buildReq(null, "某某再生资源有限公司",
                SellerSubjectTypeEnum.ENTERPRISE_LEGAL.getType()));

        assertServiceException(
                () -> supplierService.updateSupplier(buildReq(id, "张三",
                        SellerSubjectTypeEnum.NATURAL.getType())),
                SUPPLIER_SUBJECT_TYPE_NATURAL_NOT_ALLOWED);
        // 更新被拒后原值不变
        assertEquals(SellerSubjectTypeEnum.ENTERPRISE_LEGAL.getType(),
                supplierService.getSupplier(id).getSubjectType());
    }

    @Test
    public void testCreate_subjectTypeRequired() {
        assertServiceException(
                () -> supplierService.createSupplier(buildReq(null, "未分类主体", null)),
                SUPPLIER_SUBJECT_TYPE_REQUIRED);
    }

    @Test
    public void testCreate_invalidSubjectTypeRejected() {
        assertServiceException(
                () -> supplierService.createSupplier(buildReq(null, "非法主体", 99)),
                SUPPLIER_SUBJECT_TYPE_INVALID, 99);
    }

    @Test
    public void testCreate_fiveNonNaturalSubjectTypesAccepted() {
        for (SellerSubjectTypeEnum subjectType : SellerSubjectTypeEnum.values()) {
            if (subjectType.isNatural()) {
                assertTrue(SellerSubjectTypeEnum.isNaturalType(subjectType.getType()));
                continue;
            }
            assertFalse(SellerSubjectTypeEnum.isNaturalType(subjectType.getType()));
            Long id = supplierService.createSupplier(buildReq(null, subjectType.getName(), subjectType.getType()));
            assertNotNull(id);
            assertEquals(subjectType.getType(), supplierService.getSupplier(id).getSubjectType());
        }
    }

    @Test
    public void testDelete_ok() {
        Long id = supplierService.createSupplier(buildReq(null, "可删除主体",
                SellerSubjectTypeEnum.SOLE_PROPRIETORSHIP.getType()));

        supplierService.deleteSupplier(id);
        assertNull(supplierService.getSupplier(id));
    }

    @Test
    public void testDelete_blockedWhenReferencedByPurchaseOrder() {
        Long id = supplierService.createSupplier(buildReq(null, "已被引用主体",
                SellerSubjectTypeEnum.ENTERPRISE_LEGAL.getType()));
        purchaseOrderMapper.insert(new ErpPurchaseOrderDO().setNo("PO-001").setStatus(10).setSupplierId(id));

        assertServiceException(() -> supplierService.deleteSupplier(id),
                SUPPLIER_DELETE_FAIL_REFERENCED, "已被引用主体");
        // 引用存在时档案仍在
        assertNotNull(supplierService.getSupplier(id));
    }

    private static ErpSupplierSaveReqVO buildReq(Long id, String name, Integer subjectType) {
        ErpSupplierSaveReqVO req = new ErpSupplierSaveReqVO();
        req.setId(id);
        req.setName(name);
        req.setSubjectType(subjectType);
        req.setTaxpayerQualification(TaxpayerQualificationEnum.SMALL_SCALE.getQualification());
        req.setTaxNo("91310000MA1FL0000X");
        req.setContact("李四");
        req.setMobile("15601691300");
        req.setTelephone("021-12345678");
        req.setAddress("上海市浦东新区张江镇 1 号");
        req.setBankName("工商银行张江支行");
        req.setBankAccount("622908212277228617");
        req.setStatus(0);
        req.setSort(1);
        return req;
    }

}
