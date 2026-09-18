package cn.iocoder.yudao.module.icbc.service.payee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 工行收方信息 Service 接口
 *
 * @author 芋道源码
 */
public interface PayeeInfoService {

    /**
     * 创建收方信息
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPayeeInfo(@Valid PayeeInfoSaveReqVO createReqVO);

    /**
     * 更新收方信息
     *
     * @param updateReqVO 更新信息
     */
    void updatePayeeInfo(@Valid PayeeInfoSaveReqVO updateReqVO);

    /**
     * 删除收方信息
     *
     * @param id 编号
     */
    void deletePayeeInfo(Long id);

    /**
     * 获得收方信息
     *
     * @param id 编号
     * @return 收方信息
     */
    PayeeInfoDO getPayeeInfo(Long id);

    /**
     * 获得收方信息分页
     *
     * @param pageReqVO 分页查询
     * @return 收方信息分页
     */
    PageResult<PayeeInfoDO> getPayeeInfoPage(PayeeInfoPageReqVO pageReqVO);

    /**
     * 获得收方信息列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 收方信息列表
     */
    List<PayeeInfoDO> getPayeeInfoList(PayeeInfoPageReqVO exportReqVO);

    // ==================== 工行接口相关方法 ====================

    /**
     * 调用工行收方新增接口
     *
     * @param reqVO 收方新增请求
     * @return 收方信息ID
     */
    Long addPayeeToIcbc(@Valid PayeeAddReqVO reqVO);

    /**
     * 调用工行收方查询接口
     *
     * @param reqVO 查询请求
     * @return 收方信息列表
     */
    List<PayeeInfoDO> queryPayeeFromIcbc(@Valid PayeeQueryReqVO reqVO);

    /**
     * 处理工行收方审核回调
     *
     * @param outUserId 外部用户编号
     * @param auditStatus 审核状态
     * @param auditMsg 审核信息
     * @param icbcMediumId 工行电子账户账号
     */
    void handlePayeeAuditCallback(String outUserId, String auditStatus, String auditMsg, String icbcMediumId);

    /**
     * 根据合作方收方编号获取收方信息
     *
     * @param partnerPayeeId 合作方收方编号
     * @return 收方信息
     */
    PayeeInfoDO getPayeeInfoByPartnerPayeeId(String partnerPayeeId);

    /**
     * 根据身份证号码获取收方信息
     *
     * @param idCardNo 身份证号码
     * @return 收方信息
     */
    PayeeInfoDO getPayeeInfoByIdCardNo(String idCardNo);

} 