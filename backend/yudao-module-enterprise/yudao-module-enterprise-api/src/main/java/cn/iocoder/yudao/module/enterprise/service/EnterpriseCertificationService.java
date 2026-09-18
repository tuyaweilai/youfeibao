package cn.iocoder.yudao.module.enterprise.service;

import java.util.Map;

/**
 * 企业认证 Service 接口
 */
public interface EnterpriseCertificationService {

    /**
     * 发起个人认证
     *
     * @param userId 用户ID
     * @param realName 真实姓名
     * @param idCardNo 身份证号
     * @return 认证URL
     */
    String initiatePersonalAuth(Long userId, String realName, String idCardNo);

    /**
     * 发起企业认证
     *
     * @param enterpriseId 企业ID
     * @param orgName 企业名称
     * @param orgIDCardNum 企业证件号码
     * @param legalRepName 法人姓名
     * @param legalRepIDCardNum 法人身份证号
     * @return 认证URL
     */
    String initiateEnterpriseAuth(Long enterpriseId, String orgName, String orgIDCardNum, 
                                String legalRepName, String legalRepIDCardNum);

    /**
     * 获取认证状态
     *
     * @param authFlowId 认证流程ID
     * @return 认证状态信息
     */
    Map<String, Object> getAuthStatus(String authFlowId);

    /**
     * 发起企业认证
     *
     * @param enterpriseId 企业ID
     * @return 认证URL
     */
    String initiateEnterpriseCertification(Long enterpriseId);
} 