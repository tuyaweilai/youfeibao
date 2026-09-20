package cn.iocoder.yudao.module.icbc.service.purchasecontract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractAuditReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractCloseReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo.PurchaseContractSubmitReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchasecontract.IcbcPurchaseContractDO;

/**
 * 采购合同 Service 接口（#45 / T07，ADR 0027）。
 *
 * <p>粒度是「一个合同 → 多个采购订单 → 多次收货」。合同需要审核，**审核通过前不得作为有效
 * 采购依据**；{@link #assertUsableAsPurchaseBasis(Long)} 是唯一门禁，采购订单（#46）等后续单据
 * 都应经它判断。
 *
 * <p>与「框架收购协议」是两件事：后者是自然人出售者对开票与代办税费的授权附件（开票前置），
 * 前者是采购条款。两者不合并。
 */
public interface PurchaseContractService {

    /**
     * 新建采购合同（落为草稿，还没有任何采购效力）。
     *
     * @param createReqVO 新建信息
     * @return 合同编号
     */
    Long createContract(PurchaseContractSaveReqVO createReqVO);

    /**
     * 修改采购合同。
     *
     * <p>草稿直接改；已生效合同的修改会**新增一版快照并回到待审核**（变更原因必填），
     * 重新审核通过前整份合同不再是有效采购依据。
     *
     * @param updateReqVO 修改信息
     */
    void updateContract(PurchaseContractSaveReqVO updateReqVO);

    /**
     * 送审：落一版快照，状态进入待审核。
     *
     * @param submitReqVO 送审信息
     */
    void submitForAudit(PurchaseContractSubmitReqVO submitReqVO);

    /**
     * 审核：通过则生效（可作为采购依据），驳回则退回草稿（审核意见必填）。
     *
     * @param auditReqVO 审核信息
     */
    void audit(PurchaseContractAuditReqVO auditReqVO);

    /**
     * 关闭：已生效合同不再作为采购依据，历史业务仍可回查。
     *
     * @param closeReqVO 关闭信息
     */
    void closeContract(PurchaseContractCloseReqVO closeReqVO);

    /**
     * 删除采购合同（只允许删除草稿；已送审 / 已生效 / 已关闭的合同要保留留痕）。
     *
     * @param id 合同编号
     */
    void deleteContract(Long id);

    /**
     * 获得采购合同。
     *
     * @param id 合同编号
     * @return 采购合同
     */
    IcbcPurchaseContractDO getContract(Long id);

    /**
     * 获得采购合同详情（含适用品类与历史版本）。
     *
     * @param id 合同编号
     * @return 详情
     */
    PurchaseContractRespVO getDetail(Long id);

    /**
     * 获得采购合同分页。
     *
     * @param pageReqVO 分页条件
     * @return 分页
     */
    PageResult<PurchaseContractRespVO> getContractPage(PurchaseContractPageReqVO pageReqVO);

    /**
     * 校验合同可作为采购依据：必须已审核生效且未过期。
     *
     * <p>这是「未审核不得作为采购依据」的唯一落点；采购订单（#46）等后续单据调用它，
     * 不要各自复制一遍判断。
     *
     * @param id 合同编号
     * @return 生效中的合同
     */
    IcbcPurchaseContractDO assertUsableAsPurchaseBasis(Long id);

}
