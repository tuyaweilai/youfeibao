package cn.iocoder.yudao.module.icbc.controller.admin.cardrecognition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 卡证识别连通性自检 Response VO（#103）。
 *
 * <p><b>不回密钥、不回厂商原始报文</b>：只回分类与中文名，它是给人看的状态。
 */
@Schema(description = "管理后台 - 卡证识别连通性自检 Response VO")
@Data
public class CardRecognitionCheckRespVO {

    @Schema(description = "是否验证通过", example = "true")
    private Boolean ok;

    @Schema(description = "自检分类：OK / AUTH_FAILED / NETWORK / VENDOR_ERROR", example = "OK")
    private String result;

    @Schema(description = "自检分类的中文名", example = "验证通过")
    private String resultName;

    @Schema(description = "自检时间")
    private LocalDateTime checkTime;

}
