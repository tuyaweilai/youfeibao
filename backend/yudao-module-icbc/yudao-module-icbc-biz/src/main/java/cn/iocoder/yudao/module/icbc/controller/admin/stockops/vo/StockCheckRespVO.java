package cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 盘点单 Response VO（#54 T16）。
 */
@Schema(description = "管理后台 - 盘点单 Response VO")
@Data
public class StockCheckRespVO {

    @Schema(description = "盘点单编号", example = "2048")
    private Long id;

    @Schema(description = "盘点单号", example = "SC202609201200001234")
    private String checkNo;

    @Schema(description = "状态：0-待过账，1-已过账，2-已作废", example = "1")
    private Integer status;

    @Schema(description = "状态名", example = "已过账")
    private String statusName;

    @Schema(description = "过账时间")
    private LocalDateTime postedTime;

    @Schema(description = "作废原因")
    private String cancelReason;

    @Schema(description = "作废时间")
    private LocalDateTime cancelledTime;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "盘点明细（含账面 / 实盘 / 差额）")
    private List<StockCheckItemRespVO> items;

}
