package cn.iocoder.yudao.module.icbc.service;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;

/**
 * 工商银行接口测试服务
 *
 * @author 芋道源码
 */
public interface IcbcTestService {

    /**
     * 测试工行SDK连接
     *
     * @return 测试结果
     */
    CommonResult<String> testConnection();

    /**
     * 测试签名验证
     *
     * @return 签名验证结果
     */
    CommonResult<String> testSignature();

    /**
     * 测试发票查询接口
     *
     * @param outOrderId 订单ID
     * @param outUserId 用户ID
     * @return 查询结果
     */
    CommonResult<Object> testInvoiceQuery(String outOrderId, String outUserId);

    /**
     * 生成支付表单HTML
     *
     * @param outOrderId 订单ID
     * @param outUserId 用户ID
     * @return 支付表单HTML
     */
    CommonResult<String> generatePaymentForm(String outOrderId, String outUserId);

    /**
     * 测试配置信息
     *
     * @return 配置信息
     */
    CommonResult<Object> testConfig();

    /**
     * 测试聚富通智慧清分收方查询接口
     *
     * @param outUserId 外部用户编号
     * @param receiverAccount 收方账号
     * @param businessType 业务类型
     * @return 查询结果
     */
    CommonResult<Object> testUserQuery(String outUserId, String receiverAccount, String businessType);

    /**
     * 手动HTTP调用聚富通智慧清分收方查询接口（绕开官方SDK）
     *
     * @param outUserId 外部用户编号
     * @param receiverAccount 收方账号
     * @param businessType 业务类型
     * @return 查询结果
     */
    CommonResult<Object> testUserQueryManual(String outUserId, String receiverAccount, String businessType);
} 