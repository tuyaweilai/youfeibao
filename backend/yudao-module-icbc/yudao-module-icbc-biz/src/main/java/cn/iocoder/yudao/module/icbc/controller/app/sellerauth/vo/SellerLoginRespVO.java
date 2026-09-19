package cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自然人出售者 - 登录 Response VO。
 *
 * <p>返回的 {@code subjects} 是当前这个登录凭证名下的自然人主体：一个手机号可以被多个主体复用
 * （子女代老人操作），所以后续每个业务请求都要**显式带上 naturalPersonId**，不做静默推断（ADR 0017）。
 */
@Schema(description = "自然人出售者 - 登录 Response VO")
@Data
public class SellerLoginRespVO {

    @Schema(description = "访问令牌")
    private String accessToken;

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "当前登录名下的自然人主体")
    private List<SellerSubjectRespVO> subjects;

}
