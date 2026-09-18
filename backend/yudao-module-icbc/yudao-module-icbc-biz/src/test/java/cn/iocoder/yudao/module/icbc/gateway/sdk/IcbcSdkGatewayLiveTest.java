package cn.iocoder.yudao.module.icbc.gateway.sdk;

import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.config.IcbcProperties;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcConnectivity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实连通性校验（联调用，默认跳过）
 *
 * 设置 {@code ICBC_APP_ID} 等环境变量后才会执行：用一条数据接口打满工行网关，
 * 验证网络可达与签名配置有效。任何来自工行的业务响应（含业务错误码）都算通过。
 *
 * 运行：
 * <pre>
 * ICBC_APP_ID=... ICBC_OUT_VENDOR_ID=... ICBC_PRIVATE_KEY=... ICBC_APIGW_PUBLIC_KEY=... \
 * ICBC_AES_KEY=... mvn -pl yudao-module-icbc/yudao-module-icbc-biz test -Dtest=IcbcSdkGatewayLiveTest
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "ICBC_APP_ID", matches = ".+")
public class IcbcSdkGatewayLiveTest {

    @Test
    public void testCheckConnectivity_live() {
        IcbcProperties properties = new IcbcProperties();
        properties.setAppId(System.getenv("ICBC_APP_ID"));
        properties.setOutVendorId(System.getenv("ICBC_OUT_VENDOR_ID"));
        properties.setPrivateKey(System.getenv("ICBC_PRIVATE_KEY"));
        properties.setApigwPublicKey(System.getenv("ICBC_APIGW_PUBLIC_KEY"));
        properties.setAesKey(System.getenv("ICBC_AES_KEY"));
        properties.setSm2PrivateKey(System.getenv("ICBC_SM2_PRIVATE_KEY"));
        properties.setSm2ApigwPublicKey(System.getenv("ICBC_SM2_APIGW_PUBLIC_KEY"));
        String baseUrl = System.getenv("ICBC_BASE_URL");
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            properties.setBaseUrl(baseUrl);
        }

        IcbcClientFactory clientFactory = new IcbcClientFactory();
        ReflectionTestUtils.setField(clientFactory, "properties", properties);
        IcbcSdkGateway gateway = new IcbcSdkGateway();
        ReflectionTestUtils.setField(gateway, "clientFactory", clientFactory);

        IcbcGatewayResult<IcbcConnectivity> result = gateway.checkConnectivity();

        assertTrue(result.isSuccess(), "连通性校验未通过：" + result.getReturnMsg());
        assertNotNull(result.getData());
        assertTrue(result.getData().isReachable(), "工行网关不可达：" + result.getData().getReturnMsg());
    }

}
