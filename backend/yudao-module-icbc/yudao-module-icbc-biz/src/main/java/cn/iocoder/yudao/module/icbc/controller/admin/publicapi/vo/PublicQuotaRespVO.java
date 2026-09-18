package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自然人额度查询响应。额度是「连续 12 个月滚动窗口」内的反向开票累计销售额，
 * 上限 500 万（见 #12）。
 */
@Schema(description = "公开端点 - 自然人额度 Response VO")
@Data
public class PublicQuotaRespVO {

    @Schema(description = "出售者姓名", example = "张三")
    private String name;

    @Schema(description = "脱敏身份证号", example = "110101********1234")
    private String idCardMasked;

    @Schema(description = "滚动窗口上限（元）", example = "5000000.00")
    private BigDecimal capAmount;

    @Schema(description = "窗口内已用额度（元）", example = "123456.78")
    private BigDecimal usedAmount;

    @Schema(description = "剩余额度（元）", example = "4876543.22")
    private BigDecimal remainingAmount;

    @Schema(description = "窗口开始时间")
    private LocalDateTime windowStart;

    @Schema(description = "窗口结束时间")
    private LocalDateTime windowEnd;

}
