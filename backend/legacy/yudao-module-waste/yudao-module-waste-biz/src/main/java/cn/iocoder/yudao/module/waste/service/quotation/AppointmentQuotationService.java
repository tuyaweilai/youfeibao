package cn.iocoder.yudao.module.waste.service.quotation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约报价记录 Service 接口
 *
 * @author 芋道源码
 */
public interface AppointmentQuotationService {

    /**
     * 创建预约报价记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAppointmentQuotation(@Valid AppointmentQuotationCreateReqVO createReqVO);

    /**
     * 更新预约报价记录
     *
     * @param updateReqVO 更新信息
     */
    void updateAppointmentQuotation(@Valid AppointmentQuotationUpdateReqVO updateReqVO);

    /**
     * 删除预约报价记录
     *
     * @param id 编号
     */
    void deleteAppointmentQuotation(Long id);

    /**
     * 获得预约报价记录
     *
     * @param id 编号
     * @return 预约报价记录
     */
    AppointmentQuotationDO getAppointmentQuotation(Long id);

    /**
     * 获得预约报价记录详情
     *
     * @param id 编号
     * @return 预约报价记录详情
     */
    AppointmentQuotationRespVO getAppointmentQuotationDetail(Long id);

    /**
     * 获得预约报价记录分页
     *
     * @param pageReqVO 分页查询
     * @return 预约报价记录分页
     */
    PageResult<AppointmentQuotationRespVO> getAppointmentQuotationPage(AppointmentQuotationPageReqVO pageReqVO);

    /**
     * 获得预约报价记录列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 预约报价记录列表
     */
    List<AppointmentQuotationDO> getAppointmentQuotationList(AppointmentQuotationPageReqVO exportReqVO);

    /**
     * 根据预约单ID获得报价记录列表
     *
     * @param appointmentId 预约单ID
     * @return 报价记录列表
     */
    List<AppointmentQuotationDO> getAppointmentQuotationListByAppointmentId(Long appointmentId);

    /**
     * 根据回收企业ID获得报价记录列表
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 报价记录列表
     */
    List<AppointmentQuotationDO> getAppointmentQuotationListByRecyclingEnterpriseId(Long recyclingEnterpriseId);

    /**
     * 根据状态获得报价记录列表
     *
     * @param status 状态
     * @return 报价记录列表
     */
    List<AppointmentQuotationDO> getAppointmentQuotationListByStatus(Integer status);

    /**
     * 获得有效的报价记录列表
     *
     * @return 有效的报价记录列表
     */
    List<AppointmentQuotationDO> getValidAppointmentQuotations();

    /**
     * 获得过期的报价记录列表
     *
     * @return 过期的报价记录列表
     */
    List<AppointmentQuotationDO> getExpiredAppointmentQuotations();

    /**
     * 获得已接受的报价记录列表
     *
     * @return 已接受的报价记录列表
     */
    List<AppointmentQuotationDO> getAcceptedAppointmentQuotations();

    /**
     * 根据报价时间范围获得报价记录列表
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 报价记录列表
     */
    List<AppointmentQuotationDO> getAppointmentQuotationListByQuotationTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获得预约单的最新报价记录
     *
     * @param appointmentId 预约单ID
     * @return 最新报价记录
     */
    AppointmentQuotationDO getLatestAppointmentQuotationByAppointmentId(Long appointmentId);

    /**
     * 获得预约单的最低报价记录
     *
     * @param appointmentId 预约单ID
     * @return 最低报价记录
     */
    AppointmentQuotationDO getLowestPriceQuotationByAppointmentId(Long appointmentId);

    /**
     * 获得预约单的最高报价记录
     *
     * @param appointmentId 预约单ID
     * @return 最高报价记录
     */
    AppointmentQuotationDO getHighestPriceQuotationByAppointmentId(Long appointmentId);

    /**
     * 提交报价
     *
     * @param appointmentId 预约单ID
     * @param recyclingEnterpriseId 回收企业ID
     * @param quotedPrice 报价
     * @param quotationRemark 报价备注
     * @param validUntil 有效期至
     * @return 报价记录ID
     */
    Long submitQuotation(Long appointmentId, Long recyclingEnterpriseId, BigDecimal quotedPrice, 
                        String quotationRemark, LocalDateTime validUntil);

    /**
     * 接受报价
     *
     * @param id 报价记录ID
     * @param acceptedBy 接受人
     * @param acceptReason 接受原因
     */
    void acceptQuotation(Long id, String acceptedBy, String acceptReason);

    /**
     * 拒绝报价
     *
     * @param id 报价记录ID
     * @param rejectedBy 拒绝人
     * @param rejectReason 拒绝原因
     */
    void rejectQuotation(Long id, String rejectedBy, String rejectReason);

    /**
     * 撤回报价
     *
     * @param id 报价记录ID
     * @param withdrawnBy 撤回人
     * @param withdrawReason 撤回原因
     */
    void withdrawQuotation(Long id, String withdrawnBy, String withdrawReason);

    /**
     * 更新报价状态
     *
     * @param id 报价记录ID
     * @param status 状态
     */
    void updateQuotationStatus(Long id, Integer status);

    /**
     * 批量过期报价
     *
     * @param ids 报价记录ID列表
     */
    void batchExpireQuotations(List<Long> ids);

    /**
     * 校验报价记录是否存在
     *
     * @param id 报价记录ID
     * @return 报价记录信息
     */
    AppointmentQuotationDO validateAppointmentQuotationExists(Long id);

    /**
     * 提交报价（简化版本）
     *
     * @param appointmentId 预约单ID
     * @param recyclerEnterpriseId 回收企业ID
     * @param quotedPrice 报价
     * @param quotationNotes 报价备注
     */
    void submitQuotation(Long appointmentId, Long recyclerEnterpriseId, BigDecimal quotedPrice, String quotationNotes);

    /**
     * 接受报价（简化版本）
     *
     * @param id 报价记录ID
     */
    void acceptQuotation(Long id);

    /**
     * 拒绝报价（简化版本）
     *
     * @param id 报价记录ID
     * @param rejectionReason 拒绝原因
     */
    void rejectQuotation(Long id, String rejectionReason);

    /**
     * 撤回报价（简化版本）
     *
     * @param id 报价记录ID
     * @param withdrawalReason 撤回原因
     */
    void withdrawQuotation(Long id, String withdrawalReason);

    /**
     * 根据预约单ID获取报价记录
     *
     * @param appointmentId 预约单ID
     * @return 报价记录列表
     */
    List<AppointmentQuotationDO> getQuotationsByAppointmentId(Long appointmentId);

    /**
     * 根据回收企业ID获取报价记录
     *
     * @param recyclerEnterpriseId 回收企业ID
     * @return 报价记录列表
     */
    List<AppointmentQuotationDO> getQuotationsByRecyclerEnterpriseId(Long recyclerEnterpriseId);

    /**
     * 获取有效报价
     *
     * @param appointmentId 预约单ID
     * @return 有效报价列表
     */
    List<AppointmentQuotationDO> getValidQuotations(Long appointmentId);

    /**
     * 获取过期报价
     *
     * @param appointmentId 预约单ID
     * @return 过期报价列表
     */
    List<AppointmentQuotationDO> getExpiredQuotations(Long appointmentId);

    /**
     * 获取已接受报价
     *
     * @param appointmentId 预约单ID
     * @return 已接受报价列表
     */
    List<AppointmentQuotationDO> getAcceptedQuotations(Long appointmentId);

} 