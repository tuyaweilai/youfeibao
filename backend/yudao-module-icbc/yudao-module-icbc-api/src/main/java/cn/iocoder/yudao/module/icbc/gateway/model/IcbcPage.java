package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 工行 UI 页面接口的返回：一段自动提交到工行网关的表单 HTML
 *
 * 预下单、付方支付、收方入驻、企业授权、红字冲销都是 UI 类型接口。平台不直接请求它们，
 * 而是生成表单 HTML 交给浏览器，由浏览器携带签名 POST 到工行页面。
 */
@Data
@Builder
public class IcbcPage {

    /**
     * 自动提交表单 HTML
     */
    private String formHtml;
    /**
     * 合作方单号（回填，便于调用方关联）
     */
    private String outOrderId;

}
