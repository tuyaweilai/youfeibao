package cn.iocoder.yudao.module.icbc.service;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcConnectivity;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;

import java.util.Map;

/**
 * 工行接口连通性与调试服务
 *
 * 所有方法都经 {@code IcbcGateway} 端口，测试控制器不直接接触 SDK、密钥或网关地址。
 *
 * @author 芋道源码
 */
public interface IcbcTestService {

    /**
     * 真实连通性校验：打一条数据接口到工行网关，验证网络与签名配置
     */
    CommonResult<IcbcConnectivity> checkConnectivity();

    /**
     * 发票 / 预开票信息查询
     */
    CommonResult<InvoiceInfo> queryInvoiceInfo(String outOrderId, String outUserId);

    /**
     * 生成付方支付页面表单
     */
    CommonResult<String> generatePaymentForm(String outOrderId, String outUserId);

    /**
     * 适配层运行信息（不暴露任何密钥或网关地址）
     */
    CommonResult<Map<String, Object>> testConfig();

}
