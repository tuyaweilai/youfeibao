package cn.iocoder.yudao.module.enterprise.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo.EnterpriseUserRelationPageReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseUserRelationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户企业关系 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseUserRelationMapper extends BaseMapperX<EnterpriseUserRelationDO> {

    /**
     * 查询用户的所有企业关系
     *
     * @param userId 用户ID
     * @return 用户企业关系列表
     */
    default List<EnterpriseUserRelationDO> selectListByUserId(Long userId) {
        return selectList(EnterpriseUserRelationDO::getUserId, userId);
    }

    /**
     * 查询用户在特定企业的关系
     *
     * @param userId 用户ID
     * @param enterpriseId 企业ID
     * @return 用户企业关系
     */
    default EnterpriseUserRelationDO selectByUserIdAndEnterpriseId(Long userId, Long enterpriseId) {
        return selectOne(new LambdaQueryWrapperX<EnterpriseUserRelationDO>()
                .eq(EnterpriseUserRelationDO::getUserId, userId)
                .eq(EnterpriseUserRelationDO::getEnterpriseId, enterpriseId));
    }

    /**
     * 查询用户在特定企业和门店的关系
     *
     * @param userId 用户ID
     * @param enterpriseId 企业ID
     * @param storeId 门店ID
     * @return 用户企业关系
     */
    default EnterpriseUserRelationDO selectByUserIdAndEnterpriseIdAndStoreId(Long userId, Long enterpriseId, Long storeId) {
        return selectOne(new LambdaQueryWrapperX<EnterpriseUserRelationDO>()
                .eq(EnterpriseUserRelationDO::getUserId, userId)
                .eq(EnterpriseUserRelationDO::getEnterpriseId, enterpriseId)
                .eqIfPresent(EnterpriseUserRelationDO::getStoreId, storeId));
    }

    /**
     * 查询企业的所有用户关系
     *
     * @param enterpriseId 企业ID
     * @return 用户企业关系列表
     */
    default List<EnterpriseUserRelationDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(EnterpriseUserRelationDO::getEnterpriseId, enterpriseId);
    }

    /**
     * 查询门店的所有用户关系
     *
     * @param storeId 门店ID
     * @return 用户企业关系列表
     */
    default List<EnterpriseUserRelationDO> selectListByStoreId(Long storeId) {
        return selectList(EnterpriseUserRelationDO::getStoreId, storeId);
    }

    /**
     * 分页查询用户企业关系
     *
     * @param reqVO 查询条件
     * @return 用户企业关系分页结果
     */
    default PageResult<EnterpriseUserRelationDO> selectPage(EnterpriseUserRelationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnterpriseUserRelationDO>()
                .eqIfPresent(EnterpriseUserRelationDO::getUserId, reqVO.getUserId())
                .eqIfPresent(EnterpriseUserRelationDO::getEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(EnterpriseUserRelationDO::getStoreId, reqVO.getStoreId())
                .eqIfPresent(EnterpriseUserRelationDO::getRelationType, reqVO.getRelationType())
                .eqIfPresent(EnterpriseUserRelationDO::getIsPrimaryContact, reqVO.getIsPrimaryContact())
                .betweenIfPresent(EnterpriseUserRelationDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(EnterpriseUserRelationDO::getId));
    }
} 