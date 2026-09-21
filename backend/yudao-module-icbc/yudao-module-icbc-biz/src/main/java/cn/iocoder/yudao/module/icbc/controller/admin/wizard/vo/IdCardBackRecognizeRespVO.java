package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 建档向导 - 识别身份证国徽面结果（人工值优先，空缺处才回填）。
 */
@Schema(description = "管理后台 - 建档向导：识别身份证国徽面结果")
@Data
@Builder
public class IdCardBackRecognizeRespVO {

    @Schema(description = "证件签发日期 yyyy-MM-dd（人工值优先）")
    private String idSignDate;

    @Schema(description = "证件有效期截止 yyyy-MM-dd，长期为 9999-12-30（人工值优先）")
    private String idValidityPeriod;

    @Schema(description = "图片质量分 0-100；为空表示未识别")
    private Integer qualityScore;

    @Schema(description = "提示类告警（可读文案），不拦继续")
    private List<String> warnings;

    @Schema(description = "硬拦原因（可读文案），非空即不可继续")
    private List<String> blockReasons;

}
