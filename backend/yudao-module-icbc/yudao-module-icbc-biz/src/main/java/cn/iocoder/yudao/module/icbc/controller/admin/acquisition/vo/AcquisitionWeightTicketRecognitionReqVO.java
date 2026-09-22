package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 磅单识别 Request VO（#113）。
 *
 * <p>与车牌识别同一个口径：传 **base64** 而不是照片 URL（识别只该依赖「手里有没有这张图」，
 * 不该依赖它存在哪儿，见 ADR 0013 的修订注记）。照片本身仍走 `/infra/file/upload` 留档。
 */
@Schema(description = "管理后台 - 磅单识别 Request VO")
@Data
public class AcquisitionWeightTicketRecognitionReqVO {

    @Schema(description = "磅单照片的 base64（不带 dataURL 前缀；识别完即弃，不留存）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "磅单照片不能为空")
    private String imageBase64;

}
