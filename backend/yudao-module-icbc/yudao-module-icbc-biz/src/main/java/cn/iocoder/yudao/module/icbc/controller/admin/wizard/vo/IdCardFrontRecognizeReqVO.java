package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 建档向导 - 识别身份证人像面请求。
 *
 * <p>识别是「上传 + 识别」的**无状态**调用：图片随请求进来、识别完即弃，不进文件服务（ADR 0037）。
 * 三个字段是**当前表单里已有的值**（可能是上一次识别回填的、也可能是手工改过的），服务端据此
 * 只把空缺处填上识别结果——人工输入的值优先。
 */
@Schema(description = "管理后台 - 建档向导：识别身份证人像面请求")
@Data
public class IdCardFrontRecognizeReqVO {

    @Schema(description = "压缩后的身份证人像面照片（base64，不含 dataURL 前缀）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "证件影像不能为空")
    private String imageBase64;

    @Schema(description = "当前表单里的姓名（可为空，非空则不覆盖）")
    private String name;

    @Schema(description = "当前表单里的证件号（可为空，非空则不覆盖）")
    private String idCardNo;

    @Schema(description = "当前表单里的住址（可为空，非空则不覆盖）")
    private String address;

}
