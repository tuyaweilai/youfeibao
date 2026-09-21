package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 建档向导 - 识别银行卡结果（人工值优先，空缺处才回填）。
 */
@Schema(description = "管理后台 - 建档向导：识别银行卡结果")
@Data
@Builder
public class BankCardRecognizeRespVO {

    @Schema(description = "银行卡号（人工值优先）")
    private String bankCardNo;

    @Schema(description = "开户银行（人工值优先）")
    private String bankName;

    @Schema(description = "是否我行用户：1-我行用户，0-非我行用户（人工值优先；识别结果详见 #93）")
    private String accountCode;

    @Schema(description = "图片质量分 0-100；为空表示未识别")
    private Integer qualityScore;

    @Schema(description = "提示类告警（可读文案），不拦继续")
    private List<String> warnings;

    @Schema(description = "硬拦原因（可读文案），非空即不可继续（如电子银行卡截图）")
    private List<String> blockReasons;

}
