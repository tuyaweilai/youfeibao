package cn.iocoder.yudao.module.enterprise.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.enterprise.dal.dataobject.EsignAuthFlowDO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * e签宝认证流程 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface EsignAuthFlowMapper extends BaseMapperX<EsignAuthFlowDO> {

    /**
     * 根据认证流程ID查询
     */
    default EsignAuthFlowDO selectByAuthFlowId(String authFlowId) {
        return selectOne("auth_flow_id", authFlowId);
    }

    /**
     * 根据企业ID查询最新的认证流程
     */
    default EsignAuthFlowDO selectLatestByEnterpriseId(Long enterpriseId) {
        LambdaQueryWrapper<EsignAuthFlowDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsignAuthFlowDO::getEnterpriseId, enterpriseId)
                .eq(EsignAuthFlowDO::getAuthType, 1) // 企业认证
                .orderByDesc(EsignAuthFlowDO::getCreateTime)
                .last("LIMIT 1");
        return selectOne(queryWrapper);
    }

    /**
     * 根据用户ID查询最新的认证流程
     */
    default EsignAuthFlowDO selectLatestByUserId(Long userId) {
        LambdaQueryWrapper<EsignAuthFlowDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EsignAuthFlowDO::getUserId, userId)
                .eq(EsignAuthFlowDO::getAuthType, 2) // 个人认证
                .orderByDesc(EsignAuthFlowDO::getCreateTime)
                .last("LIMIT 1");
        return selectOne(queryWrapper);
    }
} 