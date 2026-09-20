package cn.iocoder.yudao.module.logistics.dal.mysql.driver;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.DriverQualificationPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.DriverQualificationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 司机资质信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface DriverQualificationMapper extends BaseMapperX<DriverQualificationDO> {

    default PageResult<DriverQualificationDO> selectPage(DriverQualificationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DriverQualificationDO>()
                .eqIfPresent(DriverQualificationDO::getEnterpriseId, reqVO.getEnterpriseId())
                .likeIfPresent(DriverQualificationDO::getDriverCode, reqVO.getDriverCode())
                .likeIfPresent(DriverQualificationDO::getDrivingLicenseNo, reqVO.getDrivingLicenseNo())
                .eqIfPresent(DriverQualificationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DriverQualificationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DriverQualificationDO::getId));
    }

    default List<DriverQualificationDO> selectList(DriverQualificationPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<DriverQualificationDO>()
                .eqIfPresent(DriverQualificationDO::getEnterpriseId, reqVO.getEnterpriseId())
                .likeIfPresent(DriverQualificationDO::getDriverCode, reqVO.getDriverCode())
                .likeIfPresent(DriverQualificationDO::getDrivingLicenseNo, reqVO.getDrivingLicenseNo())
                .eqIfPresent(DriverQualificationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(DriverQualificationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DriverQualificationDO::getId));
    }

    default DriverQualificationDO selectByUserId(Long userId) {
        return selectOne(DriverQualificationDO::getUserId, userId);
    }

    default DriverQualificationDO selectByDriverCode(String driverCode) {
        return selectOne(DriverQualificationDO::getDriverCode, driverCode);
    }

    default DriverQualificationDO selectByDrivingLicenseNo(String drivingLicenseNo) {
        return selectOne(DriverQualificationDO::getDrivingLicenseNo, drivingLicenseNo);
    }

    default List<DriverQualificationDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(DriverQualificationDO::getEnterpriseId, enterpriseId);
    }

    default List<DriverQualificationDO> selectListByStatus(Integer status) {
        return selectList(DriverQualificationDO::getStatus, status);
    }

} 