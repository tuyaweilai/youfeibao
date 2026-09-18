package cn.iocoder.yudao.module.enterprise.service.impl;

import cn.iocoder.yudao.module.enterprise.dal.dataobject.EsignAuthFlowDO;
import cn.iocoder.yudao.module.enterprise.dal.mysql.EsignAuthFlowMapper;
import cn.iocoder.yudao.module.enterprise.enums.EsignAuthStatusEnum;
import cn.iocoder.yudao.module.enterprise.service.EsignAuthFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * e签宝认证流程 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EsignAuthFlowServiceImpl implements EsignAuthFlowService {

    private final EsignAuthFlowMapper authFlowMapper;

    @Override
    public Long createAuthFlow(EsignAuthFlowDO authFlowDO) {
        authFlowMapper.insert(authFlowDO);
        return authFlowDO.getId();
    }

    @Override
    public EsignAuthFlowDO getByAuthFlowId(String authFlowId) {
        return authFlowMapper.selectByAuthFlowId(authFlowId);
    }

    @Override
    public void updateAuthStatus(String authFlowId, Integer authStatus, String errorMessage) {
        EsignAuthFlowDO authFlow = authFlowMapper.selectByAuthFlowId(authFlowId);
        if (authFlow == null) {
            log.warn("认证流程不存在: {}", authFlowId);
            return;
        }
        
        EsignAuthFlowDO updateObj = EsignAuthFlowDO.builder()
                .id(authFlow.getId())
                .authStatus(authStatus)
                .errorMessage(errorMessage)
                .build();
        
        authFlowMapper.updateById(updateObj);
        log.info("更新认证流程状态: authFlowId={}, status={}", authFlowId, authStatus);
    }

    @Override
    public void updateNotifyStatus(String authFlowId, String notifyData, String orgId, String personId) {
        EsignAuthFlowDO authFlow = authFlowMapper.selectByAuthFlowId(authFlowId);
        if (authFlow == null) {
            log.warn("认证流程不存在: {}", authFlowId);
            return;
        }
        
        EsignAuthFlowDO updateObj = EsignAuthFlowDO.builder()
                .id(authFlow.getId())
                .notifyStatus(1) // 已回调
                .notifyTime(LocalDateTime.now())
                .notifyData(notifyData)
                .authStatus(EsignAuthStatusEnum.SUCCESS.getStatus()) // 收到回调表示认证成功
                .orgId(orgId)
                .personId(personId)
                .build();
        
        authFlowMapper.updateById(updateObj);
        log.info("更新认证流程回调状态: authFlowId={}, orgId={}, personId={}", authFlowId, orgId, personId);
    }

    @Override
    public EsignAuthFlowDO getLatestByEnterpriseId(Long enterpriseId) {
        return authFlowMapper.selectLatestByEnterpriseId(enterpriseId);
    }

    @Override
    public EsignAuthFlowDO getLatestByUserId(Long userId) {
        return authFlowMapper.selectLatestByUserId(userId);
    }
} 