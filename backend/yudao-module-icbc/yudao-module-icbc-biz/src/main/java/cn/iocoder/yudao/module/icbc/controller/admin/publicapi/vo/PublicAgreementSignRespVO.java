package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 公开端点 - 「去签署」Response VO。
 *
 * <p>自然人在自己的页面上点「去签署」才拿到这枚链接：现生成现用、不存不复用
 * （第三方默认 30 分钟有效，ADR 0036 决策 19）。
 */
@Schema(description = "公开端点 - 「去签署」签署链接")
@Data
public class PublicAgreementSignRespVO {

    @Schema(description = "第三方签署链接（一次性，现生成现用；请立即跳转，不要转发）")
    private String signUrl;

}
