package cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运输停靠点新增 / 修改（V5 #72）。
 *
 * <p>既用于「建任务时一次带多个停靠点」，也用于「事后给任务补一个停靠点」（那时 {@code taskId} 必填）。
 * 出售者只存**编号 + 姓名 / 手机号快照**：物流不引用 icbc 的类（ADR 0032）。
 */
@Schema(description = "管理后台 - 运输停靠点新增/修改 Request VO")
@Data
public class LogisticsTransportStopSaveReqVO {

    @Schema(description = "任务编号（单独新增停靠点时必填；建任务时由外层带）", example = "1")
    private Long taskId;

    @Schema(description = "停靠点类型：1-提货，2-送货", example = "1")
    private Integer stopType;

    @Schema(description = "出售者编号（icbc 侧编号，可空）", example = "100")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String payeeName;

    @Schema(description = "出售者手机号", example = "13800138000")
    private String payeeMobile;

    @Schema(description = "地址", requiredMode = Schema.RequiredMode.REQUIRED, example = "某某路 1 号")
    @NotEmpty(message = "停靠点地址不能为空")
    private String address;

    @Schema(description = "联系人", example = "张三")
    private String contactName;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "货物名称（计划提示，不是品类权威）", example = "废钢")
    private String cargoName;

    @Schema(description = "约量（计划提示）", example = "5.5")
    private BigDecimal estimatedQuantity;

    @Schema(description = "约量单位", example = "吨")
    private String quantityUnit;

    @Schema(description = "预计到站时间")
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "备注")
    private String remark;

}
