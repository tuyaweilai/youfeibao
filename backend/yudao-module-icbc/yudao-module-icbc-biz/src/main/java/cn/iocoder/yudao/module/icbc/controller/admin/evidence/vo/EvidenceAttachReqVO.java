package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 补录一票一档证据 Request VO")
@Data
public class EvidenceAttachReqVO {

    @Schema(description = "合作方订单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "ORDER_20231201_001")
    @NotBlank(message = "合作方订单号不能为空")
    private String partnerOrderId;

    @Schema(description = "证据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "WEIGHBRIDGE_TICKET")
    @NotBlank(message = "证据类型不能为空")
    private String evidenceType;

    @Schema(description = "证据标题（留空则取证据类型名称）", example = "2024-12-01 过磅单")
    private String title;

    @Schema(description = "证据文件地址", example = "https://example.com/ticket.png")
    private String fileUrl;

    @Schema(description = "证据文件名称", example = "ticket.png")
    private String fileName;

    @Schema(description = "证据对应业务发生时间", example = "1733011200000")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime occurredTime;

    @Schema(description = "备注", example = "现场补录")
    private String remark;

}
