package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 关联单据查询分页 Response VO（#55 T17）。
 *
 * <p>{@link #scopeNote} 说明这次筛选覆盖了什么（单号 / 车牌 / 主体与时间范围），
 * {@link #hiddenDetailCount} 说明有多少命中明细没有在本页展示——汇总照实报，未展示的也说清楚（AC5）。
 */
@Schema(description = "管理后台 - 关联单据查询 Response VO")
@Data
public class TraceSearchRespVO {

    @Schema(description = "当前页的收购单链路")
    private List<TraceRowRespVO> list;

    @Schema(description = "命中的收购单总张数", example = "42")
    private Long total;

    @Schema(description = "汇总（每张收购单只计一次）")
    private TraceSummaryRespVO summary;

    @Schema(description = "筛选范围说明")
    private String scopeNote;

    @Schema(description = "本次命中但未在当前页展示的明细张数", example = "30")
    private Long hiddenDetailCount;

}
