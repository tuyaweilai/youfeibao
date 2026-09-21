package cn.iocoder.yudao.module.icbc.controller.admin.payer.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工行付方信息 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PayerInfoRespVO extends PayerInfoSaveReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    // 只读：工行付方编号由工行分配 / addPayerToIcbc 写入，不接受客户端在 create/update 里设置
    // （#100 复审 BLOCK-1）。读出来仍要保留：运营要拿它与工行回调对得上。
    @Schema(description = "工行付方编号（只读，由工行分配）", example = "P1770000000000123456")
    private String payerNo;

    @Schema(description = "审核消息", example = "审核通过")
    private String auditMsg;

    @Schema(description = "工行付方状态：0-不可用，1-可用", example = "1")
    private String icbcPayerStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime updateTime;

} 