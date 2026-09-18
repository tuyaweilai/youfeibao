package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 五流中某一流的一条证据来源。系统自动取到的与人工补录的都归一成这个形状，
 * 前端不必区分来源。
 */
@Schema(description = "管理后台 - 单条证据来源 Response VO")
@Data
public class EvidenceSourceRespVO {

    @Schema(description = "来源类型", example = "PAYMENT_ORDER")
    private String sourceType;

    @Schema(description = "标题", example = "支付成功流水")
    private String title;

    @Schema(description = "引用凭证", example = "PAY20231201001")
    private String ref;

    @Schema(description = "文件地址")
    private String url;

    @Schema(description = "本地发票文件所属下载记录ID（用于走发票下载接口取原件）")
    private Long downloadId;

    @Schema(description = "本地发票文件类型", example = "PDF")
    private String fileType;

    @Schema(description = "发生时间")
    private LocalDateTime occurredTime;

}
