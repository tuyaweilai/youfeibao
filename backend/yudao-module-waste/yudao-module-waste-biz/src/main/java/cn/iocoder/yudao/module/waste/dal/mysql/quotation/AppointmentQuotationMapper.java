package cn.iocoder.yudao.module.waste.dal.mysql.quotation;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.AppointmentQuotationPageReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约报价记录 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface AppointmentQuotationMapper extends BaseMapperX<AppointmentQuotationDO> {

    default PageResult<AppointmentQuotationDO> selectPage(AppointmentQuotationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eqIfPresent(AppointmentQuotationDO::getAppointmentId, reqVO.getAppointmentId())
                .eqIfPresent(AppointmentQuotationDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(AppointmentQuotationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AppointmentQuotationDO::getCreateTime, reqVO.getQuotationTime())
                .betweenIfPresent(AppointmentQuotationDO::getValidUntil, reqVO.getValidUntil())
                .betweenIfPresent(AppointmentQuotationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AppointmentQuotationDO::getCreateTime));
    }

    default List<AppointmentQuotationDO> selectList(AppointmentQuotationPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eqIfPresent(AppointmentQuotationDO::getAppointmentId, reqVO.getAppointmentId())
                .eqIfPresent(AppointmentQuotationDO::getRecyclingEnterpriseId, reqVO.getRecyclingEnterpriseId())
                .eqIfPresent(AppointmentQuotationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(AppointmentQuotationDO::getCreateTime, reqVO.getQuotationTime())
                .betweenIfPresent(AppointmentQuotationDO::getValidUntil, reqVO.getValidUntil())
                .betweenIfPresent(AppointmentQuotationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AppointmentQuotationDO::getCreateTime));
    }

    default List<AppointmentQuotationDO> selectByAppointmentId(Long appointmentId) {
        return selectList(AppointmentQuotationDO::getAppointmentId, appointmentId);
    }

    default List<AppointmentQuotationDO> selectByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectList(AppointmentQuotationDO::getRecyclingEnterpriseId, recyclingEnterpriseId);
    }

    default List<AppointmentQuotationDO> selectByStatus(Integer status) {
        return selectList(AppointmentQuotationDO::getStatus, status);
    }

    default List<AppointmentQuotationDO> selectValidQuotations() {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getStatus, 0) // 待确认状态
                .gt(AppointmentQuotationDO::getValidUntil, LocalDateTime.now()) // 未过期
                .orderByDesc(AppointmentQuotationDO::getCreateTime));
    }

    default List<AppointmentQuotationDO> selectExpiredQuotations() {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getStatus, 0) // 待确认状态
                .le(AppointmentQuotationDO::getValidUntil, LocalDateTime.now()) // 已过期
                .orderByDesc(AppointmentQuotationDO::getValidUntil));
    }

    default List<AppointmentQuotationDO> selectAcceptedQuotations() {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getStatus, 1) // 已被接受
                .orderByDesc(AppointmentQuotationDO::getAcceptedTime));
    }

    default AppointmentQuotationDO selectAcceptedByAppointmentId(Long appointmentId) {
        return selectOne(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, 1) // 已被接受
                .orderByDesc(AppointmentQuotationDO::getAcceptedTime)
                .last("LIMIT 1"));
    }

    default List<AppointmentQuotationDO> selectByAppointmentIdAndStatus(Long appointmentId, Integer status) {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, status)
                .orderByDesc(AppointmentQuotationDO::getCreateTime));
    }

    default AppointmentQuotationDO selectByAppointmentIdAndEnterpriseId(Long appointmentId, Long recyclingEnterpriseId) {
        return selectOne(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getRecyclingEnterpriseId, recyclingEnterpriseId)
                .orderByDesc(AppointmentQuotationDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default List<AppointmentQuotationDO> selectByContractId(Long contractId) {
        return selectList(AppointmentQuotationDO::getContractId, contractId);
    }

    // Service实现类需要的方法
    default List<AppointmentQuotationDO> selectListByAppointmentId(Long appointmentId) {
        return selectByAppointmentId(appointmentId);
    }

    default List<AppointmentQuotationDO> selectListByRecyclingEnterpriseId(Long recyclingEnterpriseId) {
        return selectByRecyclingEnterpriseId(recyclingEnterpriseId);
    }

    default List<AppointmentQuotationDO> selectListByStatus(Integer status) {
        return selectByStatus(status);
    }

    default List<AppointmentQuotationDO> selectListByQuotationTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .between(AppointmentQuotationDO::getCreateTime, startTime, endTime)
                .orderByDesc(AppointmentQuotationDO::getCreateTime));
    }

    default AppointmentQuotationDO selectLatestByAppointmentId(Long appointmentId) {
        return selectOne(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .orderByDesc(AppointmentQuotationDO::getCreateTime)
                .last("LIMIT 1"));
    }

    default AppointmentQuotationDO selectLowestPriceByAppointmentId(Long appointmentId) {
        return selectOne(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, 1) // 有效状态
                .orderByAsc(AppointmentQuotationDO::getQuotedPrice)
                .last("LIMIT 1"));
    }

    default AppointmentQuotationDO selectHighestPriceByAppointmentId(Long appointmentId) {
        return selectOne(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, 1) // 有效状态
                .orderByDesc(AppointmentQuotationDO::getQuotedPrice)
                .last("LIMIT 1"));
    }

    default List<AppointmentQuotationDO> selectValidQuotationsByAppointmentId(Long appointmentId) {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, 1) // 有效状态
                .gt(AppointmentQuotationDO::getValidUntil, LocalDateTime.now()) // 未过期
                .orderByDesc(AppointmentQuotationDO::getCreateTime));
    }

    default List<AppointmentQuotationDO> selectExpiredQuotationsByAppointmentId(Long appointmentId) {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, 1) // 有效状态
                .le(AppointmentQuotationDO::getValidUntil, LocalDateTime.now()) // 已过期
                .orderByDesc(AppointmentQuotationDO::getValidUntil));
    }

    default List<AppointmentQuotationDO> selectAcceptedQuotationsByAppointmentId(Long appointmentId) {
        return selectList(new LambdaQueryWrapperX<AppointmentQuotationDO>()
                .eq(AppointmentQuotationDO::getAppointmentId, appointmentId)
                .eq(AppointmentQuotationDO::getStatus, 2) // 已接受状态
                .orderByDesc(AppointmentQuotationDO::getAcceptedTime));
    }

} 