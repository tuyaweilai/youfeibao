package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpSupplierMapper;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_DELETE_FAIL_REFERENCED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_SUBJECT_TYPE_INVALID;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_SUBJECT_TYPE_NATURAL_NOT_ALLOWED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SUPPLIER_SUBJECT_TYPE_REQUIRED;

/**
 * ERP 供应商 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpSupplierServiceImpl implements ErpSupplierService {

    @Resource
    private ErpSupplierMapper supplierMapper;
    @Resource
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Resource
    private ErpPurchaseInMapper purchaseInMapper;
    @Resource
    private ErpPurchaseReturnMapper purchaseReturnMapper;

    @Override
    public Long createSupplier(ErpSupplierSaveReqVO createReqVO) {
        validateSubjectType(createReqVO.getSubjectType());
        ErpSupplierDO supplier = BeanUtils.toBean(createReqVO, ErpSupplierDO.class);
        supplierMapper.insert(supplier);
        return supplier.getId();
    }

    @Override
    public void updateSupplier(ErpSupplierSaveReqVO updateReqVO) {
        // 校验存在
        validateSupplierExists(updateReqVO.getId());
        // 校验主体类型
        validateSubjectType(updateReqVO.getSubjectType());
        // 更新
        ErpSupplierDO updateObj = BeanUtils.toBean(updateReqVO, ErpSupplierDO.class);
        supplierMapper.updateById(updateObj);
    }

    @Override
    public void deleteSupplier(Long id) {
        // 校验存在
        ErpSupplierDO supplier = validateSupplierExists(id);
        // 被历史单据引用时不可删；有历史记录的档案应停用（status=1）而不是删除（ADR 0027）
        if (countReferencedDocuments(id) > 0) {
            throw exception(SUPPLIER_DELETE_FAIL_REFERENCED, supplier.getName());
        }
        // 删除
        supplierMapper.deleteById(id);
    }

    /**
     * 校验单位供货方的主体类型。
     *
     * <p>主体类型必填；且不能是自然人——自然人出售者的档案在 {@code icbc_payee_info}，
     * 复制一份进 {@code erp_supplier} 会造出第二个事实源（ADR 0027）。
     */
    private void validateSubjectType(Integer subjectType) {
        if (subjectType == null) {
            throw exception(SUPPLIER_SUBJECT_TYPE_REQUIRED);
        }
        SellerSubjectTypeEnum subjectTypeEnum = SellerSubjectTypeEnum.valueOf(subjectType);
        if (subjectTypeEnum == null) {
            throw exception(SUPPLIER_SUBJECT_TYPE_INVALID, subjectType);
        }
        if (subjectTypeEnum.isNatural()) {
            throw exception(SUPPLIER_SUBJECT_TYPE_NATURAL_NOT_ALLOWED);
        }
    }

    /**
     * 统计引用该供货方的采购单据数量（订单 / 入库 / 退货）。
     */
    private long countReferencedDocuments(Long supplierId) {
        return purchaseOrderMapper.selectCount(ErpPurchaseOrderDO::getSupplierId, supplierId)
                + purchaseInMapper.selectCount(ErpPurchaseInDO::getSupplierId, supplierId)
                + purchaseReturnMapper.selectCount(ErpPurchaseReturnDO::getSupplierId, supplierId);
    }

    private ErpSupplierDO validateSupplierExists(Long id) {
        ErpSupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        return supplier;
    }

    @Override
    public ErpSupplierDO getSupplier(Long id) {
        return supplierMapper.selectById(id);
    }

    @Override
    public ErpSupplierDO validateSupplier(Long id) {
        ErpSupplierDO supplier = supplierMapper.selectById(id);
        if (supplier == null) {
            throw exception(SUPPLIER_NOT_EXISTS);
        }
        if (CommonStatusEnum.isDisable(supplier.getStatus())) {
            throw exception(SUPPLIER_NOT_ENABLE, supplier.getName());
        }
        return supplier;
    }

    @Override
    public List<ErpSupplierDO> getSupplierList(Collection<Long> ids) {
        return supplierMapper.selectBatchIds(ids);
    }

    @Override
    public PageResult<ErpSupplierDO> getSupplierPage(ErpSupplierPageReqVO pageReqVO) {
        return supplierMapper.selectPage(pageReqVO);
    }

    @Override
    public List<ErpSupplierDO> getSupplierListByStatus(Integer status) {
        return supplierMapper.selectListByStatus(status);
    }

}
