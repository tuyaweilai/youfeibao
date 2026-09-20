package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入库单 Response VO（#52 T14）。
 */
@Schema(description = "管理后台 - 入库单 Response VO")
@Data
public class StockInRespVO {

    @Schema(description = "入库单编号", example = "2048")
    private Long id;

    @Schema(description = "入库单号", example = "SI202609201200001234")
    private String stockInNo;

    @Schema(description = "收购单编号", example = "1024")
    private Long acquisitionId;

    @Schema(description = "收购单号", example = "ACQ2026092000001")
    private String acquisitionNo;

    @Schema(description = "出售者档案编号", example = "1")
    private Long payeeId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "品类名称", example = "废钢")
    private String categoryName;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "可入库实物量（实物口径，确认时快照）", example = "12500")
    private BigDecimal availableQuantity;

    @Schema(description = "本次入库合计", example = "12500")
    private BigDecimal totalQuantity;

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

    @Schema(description = "入库明细（含仓库 / 库位 / 批次）")
    private List<StockInItemRespVO> items;

}
