package cn.iocoder.yudao.module.enterprise.service;

import java.util.Map;

public interface EnterpriseCertificationService {
    /**
     * 发起企业三要素认证，返回H5链接/短链接
     */
    String initiateEnterpriseCertification(Long enterpriseId);
    
    /**
     * 发起个人认证
     */
    String initiatePersonalAuth(Long userId, String realName, String idCardNo);
    
    /**
     * 发起企业认证
     */
    String initiateEnterpriseAuth(Long enterpriseId, String orgName, String orgIDCardNum, 
                                String legalRepName, String legalRepIDCardNum);
    
    /**
     * 获取认证状态
     */
    Map<String, Object> getAuthStatus(String authFlowId);
} 