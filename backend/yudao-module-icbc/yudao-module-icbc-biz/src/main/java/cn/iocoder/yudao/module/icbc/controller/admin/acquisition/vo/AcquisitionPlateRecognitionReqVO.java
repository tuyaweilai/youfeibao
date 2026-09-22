package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 车牌识别 Request VO（#112）。
 *
 * <p><b>传 base64 而不是照片 URL</b>：识别只该依赖「手里有没有这张图」，不该依赖它存在哪儿。
 * 照片存在本机文件服务里（`infra_file_config.domain` 开发环境是 {@code 127.0.0.1:48080}），
 * 把地址交给厂商必然下载不到；走后端按地址回取字节，等于按客户端给的地址出站下载（SSRF）。
 * 现场弱网下照片上传失败是常事，而识别恰恰最需要在那种时候还能用。理由见 ADR 0013 的修订注记。
 *
 * <p>现场端已按腾讯的 10M 上限压缩后再传（#93 的 {@code compressDataUrl}）。
 */
@Schema(description = "管理后台 - 车牌识别 Request VO")
@Data
public class AcquisitionPlateRecognitionReqVO {

    @Schema(description = "车头 / 车尾照片的 base64（不带 dataURL 前缀；识别完即弃，不留存）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "照片不能为空")
    private String imageBase64;

}
