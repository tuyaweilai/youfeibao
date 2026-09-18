package cn.iocoder.yudao.module.icbc.gateway.sdk;

import cn.iocoder.yudao.module.icbc.gateway.config.IcbcProperties;
import com.icbc.api.DefaultIcbcClient;
import com.icbc.api.IcbcConstants;
import com.icbc.api.UiIcbcClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 工行 SDK 客户端工厂
 *
 * 客户端按需创建并缓存：配置缺失时不会在 Spring 启动阶段抛错，便于无密钥的本地 / 测试环境
 * 只加载假适配层。
 */
@Component
public class IcbcClientFactory {

    @Resource
    private IcbcProperties properties;

    private volatile DefaultIcbcClient defaultClient;
    private volatile UiIcbcClient uiClient;

    /**
     * 数据接口客户端
     */
    public DefaultIcbcClient defaultClient() {
        if (defaultClient == null) {
            synchronized (this) {
                if (defaultClient == null) {
                    defaultClient = new DefaultIcbcClient(properties.getAppId(), properties.getSignType(),
                            properties.getPrivateKey(), IcbcConstants.CHARSET_UTF8, properties.getFormat(),
                            properties.getApigwPublicKey(), properties.getEncryptType(), properties.getAesKey(),
                            properties.getSm2PrivateKey(), properties.getSm2ApigwPublicKey());
                }
            }
        }
        return defaultClient;
    }

    /**
     * UI 页面接口客户端
     */
    public UiIcbcClient uiClient() {
        if (uiClient == null) {
            synchronized (this) {
                if (uiClient == null) {
                    uiClient = new UiIcbcClient(properties.getAppId(), properties.getSignType(),
                            properties.getPrivateKey(), IcbcConstants.CHARSET_UTF8, properties.getEncryptType(),
                            properties.getAesKey());
                }
            }
        }
        return uiClient;
    }

    public String url(String path) {
        return properties.getBaseUrl() + path;
    }

    public String appId() {
        return properties.getAppId();
    }

    public String outVendorId() {
        return properties.getOutVendorId();
    }

}
