package cn.iocoder.yudao.module.icbc.gateway.sdk;

import com.icbc.api.AbstractIcbcRequest;
import com.icbc.api.BizContent;
import com.icbc.api.IcbcResponse;
import lombok.Data;

/**
 * 实人认证结果查询请求
 *
 * 工行 SDK 只提供了实人认证 H5 页面接口，没有提供结果查询的请求 / 响应类，
 * 因此在适配层内自建一对。响应按字段名反序列化到 {@link FaceH5QueryResponse}。
 */
public class FaceH5QueryRequest extends AbstractIcbcRequest<FaceH5QueryResponse> {

    public FaceH5QueryRequest() {
        super.setBizContent(new Biz());
    }

    @Override
    public Class<FaceH5QueryResponse> getResponseClass() {
        return FaceH5QueryResponse.class;
    }

    @Override
    public boolean isNeedEncrypt() {
        return false;
    }

    @Override
    public Class<? extends BizContent> getBizContentClass() {
        return Biz.class;
    }

    @Override
    public String getMethod() {
        return "POST";
    }

    @Override
    public Biz getBizContent() {
        return (Biz) super.getBizContent();
    }

    @Data
    public static class Biz implements BizContent {
        private String appId;
        private String outUserId;
        private String transNo;
    }

}
