package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 关联单据查询操作历史（#55 T17，AC3）。
 *
 * <p>口径是**业务单据自身记录的时点**（登记 / 验收 / 入库过账 / 结算生成与确认 / 付款 / 开票 / 红冲），
 * 不是 yudao 的系统操作日志——本平台没有为这些单据写系统操作日志，硬编一条会变成假数据。
 */
@Schema(description = "管理后台 - 关联单据查询操作历史")
@Data
public class TraceHistoryRespVO {

    @Schema(description = "发生时间")
    private LocalDateTime time;

    @Schema(description = "所属阶段编码", example = "STOCK_IN")
    private String stageCode;

    @Schema(description = "所属阶段名称", example = "仓储入库")
    private String stageName;

    @Schema(description = "动作", example = "入库过账")
    private String action;

    @Schema(description = "说明（单号 / 数量 / 状态）")
    private String detail;

}
