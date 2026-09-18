package cn.iocoder.yudao.module.icbc.service.impl;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcConnectivity;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceInfo;
import cn.iocoder.yudao.module.icbc.gateway.model.InvoiceQueryReq;
import cn.iocoder.yudao.module.icbc.gateway.model.PaymentReq;
import cn.iocoder.yudao.module.icbc.service.IcbcTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.error;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 工行接口连通性与调试服务实现
 *
 * 只依赖 {@link IcbcGateway} 端口，不引用工行 SDK / 密钥 / 网关地址。
 */
@Slf4j
@Service
public class IcbcTestServiceImpl implements IcbcTestService {

    @Resource
    private IcbcGateway icbcGateway;

    @Value("${icbc.gateway.mode:sdk}")
    private String gatewayMode;

    @Override
    public CommonResult<IcbcConnectivity> checkConnectivity() {
        IcbcGatewayResult<IcbcConnectivity> result = icbcGateway.checkConnectivity();
        if (!result.isSuccess()) {
            return error(500, "工行连通性校验失败：" + result.getReturnMsg());
        }
        return success(result.getData());
    }

    @Override
    public CommonResult<InvoiceInfo> queryInvoiceInfo(String outOrderId, String outUserId) {
        if (outOrderId == null || outOrderId.trim().isEmpty()) {
            return error(400, "合作方订单编号不能为空");
        }
        IcbcGatewayResult<InvoiceInfo> result = icbcGateway.queryInvoiceInfo(InvoiceQueryReq.builder()
                .outOrderId(outOrderId).outUserId(outUserId).build());
        if (!result.isSuccess()) {
            return error(500, "工行发票查询失败：" + result.getReturnMsg());
        }
        return success(result.getData());
    }

    @Override
    public CommonResult<String> generatePaymentForm(String outOrderId, String outUserId) {
        if (outOrderId == null || outOrderId.trim().isEmpty()) {
            return error(400, "合作方订单编号不能为空");
        }
        IcbcGatewayResult<IcbcPage> result =
                icbcGateway.submitPayment(PaymentReq.builder().outOrderId(outOrderId).outUserId(outUserId).build());
        if (!result.isSuccess()) {
            return error(500, "生成工行支付页面失败：" + result.getReturnMsg());
        }
        return success(result.getData().getFormHtml());
    }

    @Override
    public CommonResult<Map<String, Object>> testConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("gatewayMode", gatewayMode);
        config.put("note", "密钥与网关地址由适配层管理，不在此展示");
        return success(config);
    }

}
