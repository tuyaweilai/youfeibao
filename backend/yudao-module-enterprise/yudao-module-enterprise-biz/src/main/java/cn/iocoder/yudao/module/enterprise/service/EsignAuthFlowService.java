package cn.iocoder.yudao.module.enterprise.service;

import cn.iocoder.yudao.module.enterprise.dal.dataobject.EsignAuthFlowDO;

/**
 * e签宝认证流程 Service 接口
 *
 * @author 芋道源码
 */
public interface EsignAuthFlowService {

    /**
     * 创建认证流程记录
     */
    Long createAuthFlow(EsignAuthFlowDO authFlowDO);

    /**
     * 根据认证流程ID查询
     */
    EsignAuthFlowDO getByAuthFlowId(String authFlowId);

    /**
     * 更新认证状态
     */
    void updateAuthStatus(String authFlowId, Integer authStatus, String errorMessage);

    /**
     * 更新回调状态
     */
    void updateNotifyStatus(String authFlowId, String notifyData, String orgId, String personId);

    /**
     * 根据企业ID查询最新的认证流程
     */
    EsignAuthFlowDO getLatestByEnterpriseId(Long enterpriseId);

    /**
     * 根据用户ID查询最新的认证流程
     */
    EsignAuthFlowDO getLatestByUserId(Long userId);
} 