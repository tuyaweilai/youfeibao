package cn.iocoder.yudao.module.enterprise.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo.EnterpriseQualificationPageReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseQualificationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 企业资质 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseQualificationMapper extends BaseMapperX<EnterpriseQualificationDO> {

    /**
     * 根据企业ID和资质类型，查询资质信息
     *
     * @param enterpriseId 企业ID
     * @param qualificationType 资质类型
     * @return 资质信息
     */
    default EnterpriseQualificationDO selectByEnterpriseIdAndType(Long enterpriseId, Integer qualificationType) {
        return selectOne(new LambdaQueryWrapperX<EnterpriseQualificationDO>()
                .eq(EnterpriseQualificationDO::getEnterpriseId, enterpriseId)
                .eq(EnterpriseQualificationDO::getQualificationType, qualificationType));
    }

    /**
     * 查询企业的所有资质
     *
     * @param enterpriseId 企业ID
     * @return 资质列表
     */
    default List<EnterpriseQualificationDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(EnterpriseQualificationDO::getEnterpriseId, enterpriseId);
    }

    /**
     * 分页查询企业资质
     *
     * @param reqVO 查询条件
     * @return 企业资质分页结果
     */
    default PageResult<EnterpriseQualificationDO> selectPage(EnterpriseQualificationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnterpriseQualificationDO>()
                .eqIfPresent(EnterpriseQualificationDO::getEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(EnterpriseQualificationDO::getQualificationType, reqVO.getQualificationType())
                .likeIfPresent(EnterpriseQualificationDO::getQualificationName, reqVO.getQualificationName())
                .likeIfPresent(EnterpriseQualificationDO::getQualificationCode, reqVO.getQualificationCode())
                .eqIfPresent(EnterpriseQualificationDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(EnterpriseQualificationDO::getExpiryDate, reqVO.getBeginExpiryDate(), reqVO.getEndExpiryDate())
                .betweenIfPresent(EnterpriseQualificationDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(EnterpriseQualificationDO::getId));
    }
} 