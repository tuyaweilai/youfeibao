package cn.iocoder.yudao.module.enterprise.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo.EnterpriseInfoPageReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseInfoDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 企业信息 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseInfoMapper extends BaseMapperX<EnterpriseInfoDO> {

    /**
     * 根据统一社会信用代码，查询企业信息
     *
     * @param creditCode 统一社会信用代码
     * @return 企业信息
     */
    default EnterpriseInfoDO selectByCreditCode(String creditCode) {
        return selectOne(EnterpriseInfoDO::getCreditCode, creditCode);
    }

    /**
     * 根据企业名称，查询企业信息
     *
     * @param name 企业名称
     * @return 企业信息
     */
    default EnterpriseInfoDO selectByName(String name) {
        return selectOne(EnterpriseInfoDO::getName, name);
    }

    /**
     * 分页查询企业信息
     *
     * @param reqVO 查询条件
     * @return 企业信息分页结果
     */
    default PageResult<EnterpriseInfoDO> selectPage(EnterpriseInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnterpriseInfoDO>()
                .likeIfPresent(EnterpriseInfoDO::getName, reqVO.getName())
                .likeIfPresent(EnterpriseInfoDO::getCreditCode, reqVO.getCreditCode())
                .eqIfPresent(EnterpriseInfoDO::getEnterpriseType, reqVO.getEnterpriseType())
                .eqIfPresent(EnterpriseInfoDO::getStatus, reqVO.getStatus())
                .likeIfPresent(EnterpriseInfoDO::getLegalPersonName, reqVO.getLegalPersonName())
                .likeIfPresent(EnterpriseInfoDO::getContactPhone, reqVO.getContactPhone())
                .betweenIfPresent(EnterpriseInfoDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(EnterpriseInfoDO::getId));
    }
    
    /**
     * 查询企业信息简易列表
     *
     * @param name 企业名称，模糊匹配，选填
     * @param status 企业状态，选填
     * @param enterpriseType 企业类型，选填
     * @return 企业信息列表
     */
    default List<EnterpriseInfoDO> selectSimpleList(String name, Integer status, Integer enterpriseType) {
        return selectList(new LambdaQueryWrapperX<EnterpriseInfoDO>()
                .likeIfPresent(EnterpriseInfoDO::getName, name)
                .eqIfPresent(EnterpriseInfoDO::getStatus, status)
                .eqIfPresent(EnterpriseInfoDO::getEnterpriseType, enterpriseType)
                .orderByDesc(EnterpriseInfoDO::getId));
    }
} 