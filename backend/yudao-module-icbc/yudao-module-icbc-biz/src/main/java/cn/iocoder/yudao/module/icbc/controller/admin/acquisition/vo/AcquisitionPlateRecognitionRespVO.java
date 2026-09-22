package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 车牌识别 Response VO（#112）。
 *
 * <p>只有「读出什么」与「提示什么」，**没有硬拦**（没有 {@code blockReasons}）：车牌读不出来只是
 * 要人手工录入，不该挡住登记。{@code plateNo} 为空即「未识别」，现场端据此去试另一张照片，
 * 再退回手工录入。
 *
 * <p>厂商的车牌颜色与车牌类别（标准实体 / 临牌 / 喷漆「放大号」）**不回**：现场看不懂、下游也没人用，
 * 临牌与放大号同样照常回填。
 */
@Schema(description = "管理后台 - 车牌识别 Response VO")
@Data
public class AcquisitionPlateRecognitionRespVO {

    @Schema(description = "识别出的车牌号；为空表示未识别", example = "京A12345")
    private String plateNo;

    @Schema(description = "置信度（0-100）；为空表示厂商没给", example = "95")
    private Integer confidence;

    @Schema(description = "提示类告警（可读文案，如「识别置信度偏低，请核对车牌」）；不拦继续")
    private List<String> warnings;

}
