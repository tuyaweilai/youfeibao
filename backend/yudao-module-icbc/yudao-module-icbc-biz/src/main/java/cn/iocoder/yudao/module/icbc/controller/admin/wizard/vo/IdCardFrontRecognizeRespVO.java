package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 建档向导 - 识别身份证人像面结果。
 *
 * <p>三个字段是**合并后**的值：人工已填的保持原样，空缺处才用识别结果回填（#91）。
 * 质量分与告警一并透到确认页，让收货员当场判断这张能不能用——告警只提示、不硬拦，
 * 硬拦原因在 {@link #blockReasons} 里（ADR 0037 / #93）。
 */
@Schema(description = "管理后台 - 建档向导：识别身份证人像面结果")
@Data
@Builder
public class IdCardFrontRecognizeRespVO {

    @Schema(description = "姓名（人工值优先，空缺处才回填识别结果）")
    private String name;

    @Schema(description = "证件号（人工值优先）")
    private String idCardNo;

    @Schema(description = "住址（人工值优先）")
    private String address;

    @Schema(description = "图片质量分 0-100；为空表示未识别")
    private Integer qualityScore;

    @Schema(description = "提示类告警（可读文案），不拦继续")
    private List<String> warnings;

    @Schema(description = "硬拦原因（可读文案），非空即不可继续")
    private List<String> blockReasons;

}
