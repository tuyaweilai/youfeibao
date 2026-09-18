package cn.iocoder.yudao.module.waste.service.appointment;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.waste.controller.admin.appointment.vo.*;
import cn.iocoder.yudao.module.waste.controller.app.appointment.vo.*;
import cn.iocoder.yudao.module.waste.convert.appointment.AppointmentConvert;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.enums.AppointmentStatusEnum;
import cn.iocoder.yudao.module.waste.enums.AssignmentTypeEnum;
import cn.iocoder.yudao.module.enterprise.service.EnterpriseInfoService;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.*;

/**
 * 危废转移预约 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Validated
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    @Resource
    private AppointmentMapper appointmentMapper;
    
    @Resource
    private EnterpriseInfoService enterpriseInfoService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAppointment(AppointmentCreateReqVO createReqVO) {
        // 转换对象
        AppointmentDO appointment = AppointmentConvert.INSTANCE.convert(createReqVO);
        
        // 生成预约单号
        appointment.setAppointmentNo(generateAppointmentNo());
        
        // 设置初始状态
        appointment.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
        
        // 根据产废企业ID设置企业名称
        if (appointment.getProducerEnterpriseId() != null) {
            try {
                EnterpriseInfoDO enterprise = enterpriseInfoService.getEnterpriseInfo(appointment.getProducerEnterpriseId());
                if (enterprise != null && enterprise.getName() != null && !enterprise.getName().trim().isEmpty()) {
                    appointment.setProducerEnterpriseName(enterprise.getName());
                } else {
                    // 如果企业信息不存在或名称为空，设置默认值
                    appointment.setProducerEnterpriseName("");
                    log.warn("[createAppointment][产废企业信息不存在或名称为空，企业ID：{}]", appointment.getProducerEnterpriseId());
                }
            } catch (Exception e) {
                // 如果获取企业信息出错，设置默认值并记录日志
                appointment.setProducerEnterpriseName("");
                log.error("[createAppointment][获取产废企业信息失败，企业ID：{}]", appointment.getProducerEnterpriseId(), e);
            }
        } else {
            // 如果企业ID为空，设置默认值
            appointment.setProducerEnterpriseName("");
        }
        
        // 设置默认值
        if (appointment.getAssignmentType() == null) {
            appointment.setAssignmentType(AssignmentTypeEnum.AUTO.getType());
        }
        if (appointment.getBusinessMode() == null) {
            appointment.setBusinessMode(0); // 默认独立运营
        }
        if (appointment.getIsUrgent() == null) {
            appointment.setIsUrgent(false);
        }
        if (appointment.getPriorityLevel() == null) {
            appointment.setPriorityLevel(0); // 默认普通优先级
        }

        // 插入数据库
        appointmentMapper.insert(appointment);
        
        // 如果指定了回收企业，直接分配
        if (appointment.getRecyclerEnterpriseId() != null) {
            assignRecyclerEnterprise(appointment.getId(), appointment.getRecyclerEnterpriseId(), 
                    AssignmentTypeEnum.MANUAL.getType(), "system");
        } else if (AssignmentTypeEnum.AUTO.getType().equals(appointment.getAssignmentType())) {
            // 自动分配回收企业
            autoAssignRecyclerEnterprise(appointment.getId());
        }
        
        log.info("[createAppointment][创建预约成功，预约ID：{}，预约单号：{}]", 
                appointment.getId(), appointment.getAppointmentNo());
        
        return appointment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAppointment(AppointmentUpdateReqVO updateReqVO) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(updateReqVO.getId());
        
        // 校验状态是否允许修改
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (statusEnum != AppointmentStatusEnum.PENDING && statusEnum != AppointmentStatusEnum.WAITING_RECYCLER_CONFIRM) {
            throw exception(APPOINTMENT_STATUS_INVALID);
        }
        
        // 转换对象并更新
        AppointmentDO updateObj = AppointmentConvert.INSTANCE.convert(updateReqVO);
        appointmentMapper.updateById(updateObj);
        
        log.info("[updateAppointment][更新预约成功，预约ID：{}]", updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAppointment(Long id) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验状态是否允许删除
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (statusEnum != AppointmentStatusEnum.PENDING && statusEnum != AppointmentStatusEnum.CANCELLED) {
            throw exception(APPOINTMENT_STATUS_INVALID);
        }
        
        // 删除
        appointmentMapper.deleteById(id);
        
        log.info("[deleteAppointment][删除预约成功，预约ID：{}]", id);
    }

    @Override
    public AppointmentDO getAppointment(Long id) {
        return appointmentMapper.selectById(id);
    }

    @Override
    public AppointmentRespVO getAppointmentDetail(Long id) {
        AppointmentDO appointment = getAppointment(id);
        return AppointmentConvert.INSTANCE.convert(appointment);
    }

    @Override
    public PageResult<AppointmentRespVO> getAppointmentPage(AppointmentPageReqVO pageReqVO) {
        PageResult<AppointmentDO> pageResult = appointmentMapper.selectPage(pageReqVO);
        return AppointmentConvert.INSTANCE.convertPage(pageResult);
    }

    @Override
    public List<AppointmentDO> getAppointmentList(AppointmentPageReqVO exportReqVO) {
        return appointmentMapper.selectList(exportReqVO);
    }

    @Override
    public AppointmentDO getAppointmentByNo(String appointmentNo) {
        return appointmentMapper.selectByAppointmentNo(appointmentNo);
    }

    @Override
    public List<AppointmentDO> getAppointmentListByProducerEnterpriseId(Long producerEnterpriseId) {
        return appointmentMapper.selectListByProducerEnterpriseId(producerEnterpriseId);
    }

    @Override
    public List<AppointmentDO> getAppointmentListByRecyclerEnterpriseId(Long recyclerEnterpriseId) {
        return appointmentMapper.selectListByRecyclerEnterpriseId(recyclerEnterpriseId);
    }

    @Override
    public List<AppointmentDO> getAppointmentListByStatus(Integer status) {
        return appointmentMapper.selectListByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmAppointment(Long id, String confirmReason) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验状态
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (!statusEnum.canConfirm()) {
            throw exception(APPOINTMENT_CANNOT_CONFIRM);
        }
        
        // 校验是否已分配回收企业
        if (appointment.getRecyclerEnterpriseId() == null) {
            throw exception(APPOINTMENT_RECYCLER_NOT_ASSIGNED);
        }
        
        // 更新状态
        AppointmentDO updateObj = new AppointmentDO();
        updateObj.setId(id);
        updateObj.setAppointmentStatus(AppointmentStatusEnum.CONFIRMED.getStatus());
        updateObj.setConfirmTime(LocalDateTime.now());
        appointmentMapper.updateById(updateObj);
        
        log.info("[confirmAppointment][确认预约成功，预约ID：{}，确认原因：{}]", id, confirmReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectAppointment(Long id, String rejectReason) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验状态
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (!statusEnum.canReject()) {
            throw exception(APPOINTMENT_CANNOT_REJECT);
        }
        
        // 更新状态
        AppointmentDO updateObj = new AppointmentDO();
        updateObj.setId(id);
        updateObj.setAppointmentStatus(AppointmentStatusEnum.REJECTED.getStatus());
        updateObj.setRejectTime(LocalDateTime.now());
        updateObj.setRejectReason(rejectReason);
        appointmentMapper.updateById(updateObj);
        
        log.info("[rejectAppointment][拒绝预约成功，预约ID：{}，拒绝原因：{}]", id, rejectReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Long id, String cancelReason) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验状态
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (!statusEnum.canCancel()) {
            throw exception(APPOINTMENT_CANNOT_CANCEL);
        }
        
        // 更新状态
        AppointmentDO updateObj = new AppointmentDO();
        updateObj.setId(id);
        updateObj.setAppointmentStatus(AppointmentStatusEnum.CANCELLED.getStatus());
        updateObj.setCancelTime(LocalDateTime.now());
        updateObj.setCancelReason(cancelReason);
        appointmentMapper.updateById(updateObj);
        
        log.info("[cancelAppointment][取消预约成功，预约ID：{}，取消原因：{}]", id, cancelReason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRecyclerEnterprise(Long id, Long recyclerEnterpriseId, Integer assignmentType, String operator) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验状态
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (statusEnum != AppointmentStatusEnum.PENDING) {
            throw exception(APPOINTMENT_STATUS_INVALID);
        }
        
        // 校验回收企业是否存在和有效
        EnterpriseInfoDO recyclerEnterprise = enterpriseInfoService.getEnterpriseInfo(recyclerEnterpriseId);
        if (recyclerEnterprise == null) {
            throw exception(RECYCLER_ENTERPRISE_NOT_EXISTS);
        }
        
        // 更新分配信息
        AppointmentDO updateObj = new AppointmentDO();
        updateObj.setId(id);
        updateObj.setRecyclerEnterpriseId(recyclerEnterpriseId);
        updateObj.setRecyclerEnterpriseName(recyclerEnterprise.getName());
        updateObj.setAssignmentType(assignmentType);
        updateObj.setAssignmentTime(LocalDateTime.now());
        updateObj.setAssignmentOperator(operator);
        updateObj.setAppointmentStatus(AppointmentStatusEnum.WAITING_RECYCLER_CONFIRM.getStatus());
        appointmentMapper.updateById(updateObj);
        
        // TODO: 记录分配历史
        
        log.info("[assignRecyclerEnterprise][分配回收企业成功，预约ID：{}，回收企业ID：{}，分配方式：{}]", 
                id, recyclerEnterpriseId, assignmentType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoAssignRecyclerEnterprise(Long id) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // TODO: 实现自动分配算法
        // 1. 根据废物类型、地址等条件查找合适的回收企业
        // 2. 应用分配规则
        // 3. 选择最优的回收企业
        
        // 暂时使用简单的逻辑：选择第一个可用的回收企业
        Long recyclerEnterpriseId = findAvailableRecyclerEnterprise(appointment);
        if (recyclerEnterpriseId == null) {
            throw exception(NO_AVAILABLE_RECYCLER);
        }
        
        assignRecyclerEnterprise(id, recyclerEnterpriseId, AssignmentTypeEnum.AUTO.getType(), "system");
        
        log.info("[autoAssignRecyclerEnterprise][自动分配回收企业成功，预约ID：{}，回收企业ID：{}]", 
                id, recyclerEnterpriseId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long generateOrder(Long id) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验状态
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (!statusEnum.canGenerateOrder()) {
            throw exception(APPOINTMENT_STATUS_INVALID);
        }
        
        // TODO: 调用订单服务创建订单
        Long orderId = createOrderFromAppointment(appointment);
        
        // 更新预约状态
        AppointmentDO updateObj = new AppointmentDO();
        updateObj.setId(id);
        updateObj.setOrderId(orderId);
        updateObj.setAppointmentStatus(AppointmentStatusEnum.ORDER_GENERATED.getStatus());
        appointmentMapper.updateById(updateObj);
        
        log.info("[generateOrder][生成订单成功，预约ID：{}，订单ID：{}]", id, orderId);
        
        return orderId;
    }

    @Override
    public AppointmentDO validateAppointmentExists(Long id) {
        if (id == null) {
            return null;
        }
        AppointmentDO appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw exception(APPOINTMENT_NOT_EXISTS);
        }
        return appointment;
    }

    /**
     * 生成预约单号
     */
    private String generateAppointmentNo() {
        // 格式：AP + yyyyMMdd + 6位随机数
        String date = LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%06d", (int) (Math.random() * 1000000));
        return "AP" + date + random;
    }

    /**
     * 查找可用的回收企业
     */
    private Long findAvailableRecyclerEnterprise(AppointmentDO appointment) {
        // TODO: 实现查找逻辑
        // 1. 根据废物类型查找有资质的回收企业
        // 2. 根据地址查找附近的回收企业
        // 3. 根据负载情况选择最优企业
        
        // 暂时返回固定值，实际应该从企业服务获取
        return 1L;
    }

    /**
     * 从预约创建订单
     */
    private Long createOrderFromAppointment(AppointmentDO appointment) {
        // TODO: 调用订单服务创建订单
        // 1. 转换预约信息为订单信息
        // 2. 调用订单服务创建订单
        // 3. 返回订单ID
        
        // 暂时返回固定值，实际应该调用订单服务
        return IdUtil.getSnowflakeNextId();
    }

    // ========== APP端接口实现 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAppointmentByApp(AppAppointmentCreateReqVO createReqVO, Long producerEnterpriseId) {
        // 转换对象
        AppointmentDO appointment = AppointmentConvert.INSTANCE.convert(createReqVO);
        
        // 设置产废企业ID（从登录用户获取）
        appointment.setProducerEnterpriseId(producerEnterpriseId);
        
        // 生成预约单号
        appointment.setAppointmentNo(generateAppointmentNo());
        
        // 设置初始状态
        appointment.setAppointmentStatus(AppointmentStatusEnum.PENDING.getStatus());
        
        // 根据产废企业ID设置企业名称
        if (appointment.getProducerEnterpriseId() != null) {
            try {
                EnterpriseInfoDO enterprise = enterpriseInfoService.getEnterpriseInfo(appointment.getProducerEnterpriseId());
                if (enterprise != null && enterprise.getName() != null && !enterprise.getName().trim().isEmpty()) {
                    appointment.setProducerEnterpriseName(enterprise.getName());
                } else {
                    // 如果企业信息不存在或名称为空，设置默认值
                    appointment.setProducerEnterpriseName("");
                    log.warn("[createAppointmentByApp][产废企业信息不存在或名称为空，企业ID：{}]", appointment.getProducerEnterpriseId());
                }
            } catch (Exception e) {
                // 如果获取企业信息出错，设置默认值并记录日志
                appointment.setProducerEnterpriseName("");
                log.error("[createAppointmentByApp][获取产废企业信息失败，企业ID：{}]", appointment.getProducerEnterpriseId(), e);
            }
        } else {
            // 如果企业ID为空，设置默认值
            appointment.setProducerEnterpriseName("");
        }
        
        // 设置默认值
        if (appointment.getAssignmentType() == null) {
            appointment.setAssignmentType(AssignmentTypeEnum.AUTO.getType());
        }
        if (appointment.getBusinessMode() == null) {
            appointment.setBusinessMode(0); // 默认独立运营
        }
        if (appointment.getIsUrgent() == null) {
            appointment.setIsUrgent(false);
        }
        if (appointment.getPriorityLevel() == null) {
            appointment.setPriorityLevel(0); // 默认普通优先级
        }

        // 插入数据库
        appointmentMapper.insert(appointment);
        
        // 自动分配回收企业
        if (AssignmentTypeEnum.AUTO.getType().equals(appointment.getAssignmentType())) {
            autoAssignRecyclerEnterprise(appointment.getId());
        }
        
        log.info("[createAppointmentByApp][APP端创建预约成功，预约ID：{}，预约单号：{}，产废企业ID：{}]", 
                appointment.getId(), appointment.getAppointmentNo(), producerEnterpriseId);
        
        return appointment.getId();
    }

    @Override
    public PageResult<AppAppointmentRespVO> getMyAppointmentPage(AppAppointmentPageReqVO pageReqVO, Long producerEnterpriseId) {
        // 构建查询条件，只查询当前用户的预约
        AppointmentPageReqVO adminPageReqVO = new AppointmentPageReqVO();
        adminPageReqVO.setPageNo(pageReqVO.getPageNo());
        adminPageReqVO.setPageSize(pageReqVO.getPageSize());
        adminPageReqVO.setProducerEnterpriseId(producerEnterpriseId); // 限制只查询当前用户的预约
        adminPageReqVO.setAppointmentNo(pageReqVO.getAppointmentNo());
        adminPageReqVO.setWasteCode(pageReqVO.getWasteCode());
        adminPageReqVO.setWasteName(pageReqVO.getWasteName());
        adminPageReqVO.setAppointmentStatus(pageReqVO.getAppointmentStatus());
        adminPageReqVO.setIsUrgent(pageReqVO.getIsUrgent());
        adminPageReqVO.setCreateTime(pageReqVO.getCreateTime());
        adminPageReqVO.setExpectedPickupTime(pageReqVO.getExpectedPickupTime());
        
        PageResult<AppointmentDO> pageResult = appointmentMapper.selectPage(adminPageReqVO);
        return AppointmentConvert.INSTANCE.convertAppPage(pageResult);
    }

    @Override
    public AppAppointmentRespVO getMyAppointmentDetail(Long id, Long producerEnterpriseId) {
        AppointmentDO appointment = getAppointment(id);
        if (appointment == null) {
            throw exception(APPOINTMENT_NOT_EXISTS);
        }
        
        // 校验是否是当前用户的预约
        if (!producerEnterpriseId.equals(appointment.getProducerEnterpriseId())) {
            throw exception(APPOINTMENT_NOT_EXISTS); // 不暴露具体错误，统一返回不存在
        }
        
        return AppointmentConvert.INSTANCE.convertApp(appointment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelMyAppointment(Long id, String cancelReason, Long producerEnterpriseId) {
        // 校验存在
        AppointmentDO appointment = validateAppointmentExists(id);
        
        // 校验是否是当前用户的预约
        if (!producerEnterpriseId.equals(appointment.getProducerEnterpriseId())) {
            throw exception(APPOINTMENT_NOT_EXISTS); // 不暴露具体错误，统一返回不存在
        }
        
        // 校验状态
        AppointmentStatusEnum statusEnum = AppointmentStatusEnum.valueOf(appointment.getAppointmentStatus());
        if (!statusEnum.canCancel()) {
            throw exception(APPOINTMENT_CANNOT_CANCEL);
        }
        
        // 更新状态
        AppointmentDO updateObj = new AppointmentDO();
        updateObj.setId(id);
        updateObj.setAppointmentStatus(AppointmentStatusEnum.CANCELLED.getStatus());
        updateObj.setCancelTime(LocalDateTime.now());
        updateObj.setCancelReason(cancelReason);
        appointmentMapper.updateById(updateObj);
        
        log.info("[cancelMyAppointment][APP端取消预约成功，预约ID：{}，取消原因：{}，产废企业ID：{}]", 
                id, cancelReason, producerEnterpriseId);
    }

} 