package cn.iocoder.yudao.module.waste.service.appointment;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.*;
import cn.iocoder.yudao.module.waste.controller.app.appointment.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;

import javax.validation.Valid;
import java.util.List;

/**
 * 危废转移预约 Service 接口
 *
 * @author 芋道源码
 */
public interface AppointmentService {

    /**
     * 创建危废转移预约
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAppointment(@Valid AppointmentCreateReqVO createReqVO);

    /**
     * 更新危废转移预约
     *
     * @param updateReqVO 更新信息
     */
    void updateAppointment(@Valid AppointmentUpdateReqVO updateReqVO);

    /**
     * 删除危废转移预约
     *
     * @param id 编号
     */
    void deleteAppointment(Long id);

    /**
     * 获得危废转移预约
     *
     * @param id 编号
     * @return 危废转移预约
     */
    AppointmentDO getAppointment(Long id);

    /**
     * 获得危废转移预约详情
     *
     * @param id 编号
     * @return 危废转移预约详情
     */
    AppointmentRespVO getAppointmentDetail(Long id);

    /**
     * 获得危废转移预约分页
     *
     * @param pageReqVO 分页查询
     * @return 危废转移预约分页
     */
    PageResult<AppointmentRespVO> getAppointmentPage(AppointmentPageReqVO pageReqVO);

    /**
     * 获得危废转移预约列表, 用于 Excel 导出
     *
     * @param exportReqVO 查询条件
     * @return 危废转移预约列表
     */
    List<AppointmentDO> getAppointmentList(AppointmentPageReqVO exportReqVO);

    /**
     * 根据预约单号获得预约信息
     *
     * @param appointmentNo 预约单号
     * @return 预约信息
     */
    AppointmentDO getAppointmentByNo(String appointmentNo);

    /**
     * 根据产废企业ID获得预约列表
     *
     * @param producerEnterpriseId 产废企业ID
     * @return 预约列表
     */
    List<AppointmentDO> getAppointmentListByProducerEnterpriseId(Long producerEnterpriseId);

    /**
     * 根据回收企业ID获得预约列表
     *
     * @param recyclerEnterpriseId 回收企业ID
     * @return 预约列表
     */
    List<AppointmentDO> getAppointmentListByRecyclerEnterpriseId(Long recyclerEnterpriseId);

    /**
     * 根据状态获得预约列表
     *
     * @param status 预约状态
     * @return 预约列表
     */
    List<AppointmentDO> getAppointmentListByStatus(Integer status);

    /**
     * 确认预约
     *
     * @param id 预约ID
     * @param confirmReason 确认原因
     */
    void confirmAppointment(Long id, String confirmReason);

    /**
     * 拒绝预约
     *
     * @param id 预约ID
     * @param rejectReason 拒绝原因
     */
    void rejectAppointment(Long id, String rejectReason);

    /**
     * 取消预约
     *
     * @param id 预约ID
     * @param cancelReason 取消原因
     */
    void cancelAppointment(Long id, String cancelReason);

    /**
     * 分配回收企业
     *
     * @param id 预约ID
     * @param recyclerEnterpriseId 回收企业ID
     * @param assignmentType 分配方式
     * @param operator 操作人
     */
    void assignRecyclerEnterprise(Long id, Long recyclerEnterpriseId, Integer assignmentType, String operator);

    /**
     * 自动分配回收企业
     *
     * @param id 预约ID
     */
    void autoAssignRecyclerEnterprise(Long id);

    /**
     * 生成订单
     *
     * @param id 预约ID
     * @return 订单ID
     */
    Long generateOrder(Long id);

    /**
     * 校验预约是否存在
     *
     * @param id 预约ID
     * @return 预约信息
     */
    AppointmentDO validateAppointmentExists(Long id);

    // ========== APP端接口 ==========

    /**
     * 【APP端】创建危废转移预约
     *
     * @param createReqVO 创建信息
     * @param producerEnterpriseId 产废企业ID（从登录用户获取）
     * @return 编号
     */
    Long createAppointmentByApp(@Valid AppAppointmentCreateReqVO createReqVO, Long producerEnterpriseId);

    /**
     * 【APP端】获得当前用户的预约分页
     *
     * @param pageReqVO 分页查询
     * @param producerEnterpriseId 产废企业ID（从登录用户获取）
     * @return 危废转移预约分页
     */
    PageResult<AppAppointmentRespVO> getMyAppointmentPage(AppAppointmentPageReqVO pageReqVO, Long producerEnterpriseId);

    /**
     * 【APP端】获得预约详情
     *
     * @param id 编号
     * @param producerEnterpriseId 产废企业ID（从登录用户获取）
     * @return 危废转移预约详情
     */
    AppAppointmentRespVO getMyAppointmentDetail(Long id, Long producerEnterpriseId);

    /**
     * 【APP端】取消预约
     *
     * @param id 预约ID
     * @param cancelReason 取消原因
     * @param producerEnterpriseId 产废企业ID（从登录用户获取）
     */
    void cancelMyAppointment(Long id, String cancelReason, Long producerEnterpriseId);

} 