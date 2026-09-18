package cn.iocoder.yudao.module.enterprise.dal.mysql;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.enterprise.controller.admin.store.vo.EnterpriseStorePageReqVO;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EnterpriseStoreDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 企业门店 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EnterpriseStoreMapper extends BaseMapperX<EnterpriseStoreDO> {

    /**
     * 按照企业ID查询门店列表
     *
     * @param enterpriseId 企业ID
     * @return 门店列表
     */
    default List<EnterpriseStoreDO> selectListByEnterpriseId(Long enterpriseId) {
        return selectList(EnterpriseStoreDO::getEnterpriseId, enterpriseId);
    }

    /**
     * 按照父ID查询门店列表
     *
     * @param parentId 父门店ID
     * @return 门店列表
     */
    default List<EnterpriseStoreDO> selectListByParentId(Long parentId) {
        return selectList(EnterpriseStoreDO::getParentId, parentId);
    }

    /**
     * 根据父门店ID和门店名称，查询门店信息
     *
     * @param parentId 父门店ID
     * @param name 门店名称
     * @return 门店信息
     */
    default EnterpriseStoreDO selectByParentIdAndName(Long parentId, String name) {
        return selectOne(new LambdaQueryWrapperX<EnterpriseStoreDO>()
                .eq(EnterpriseStoreDO::getParentId, parentId)
                .eq(EnterpriseStoreDO::getName, name));
    }

    /**
     * 根据门店编码，查询门店信息
     *
     * @param code 门店编码
     * @return 门店信息
     */
    default EnterpriseStoreDO selectByCode(String code) {
        return selectOne(EnterpriseStoreDO::getStoreCode, code);
    }

    /**
     * 分页查询企业门店
     *
     * @param reqVO 查询条件
     * @return 企业门店分页结果
     */
    default PageResult<EnterpriseStoreDO> selectPage(EnterpriseStorePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<EnterpriseStoreDO>()
                .eqIfPresent(EnterpriseStoreDO::getEnterpriseId, reqVO.getEnterpriseId())
                .eqIfPresent(EnterpriseStoreDO::getParentId, reqVO.getParentId())
                .likeIfPresent(EnterpriseStoreDO::getName, reqVO.getName())
                .likeIfPresent(EnterpriseStoreDO::getStoreCode, reqVO.getStoreCode())
                .eqIfPresent(EnterpriseStoreDO::getStatus, reqVO.getStatus())
                .likeIfPresent(EnterpriseStoreDO::getContactPhone, reqVO.getContactPhone())
                .betweenIfPresent(EnterpriseStoreDO::getCreateTime, reqVO.getBeginCreateTime(), reqVO.getEndCreateTime())
                .orderByDesc(EnterpriseStoreDO::getId));
    }
} 