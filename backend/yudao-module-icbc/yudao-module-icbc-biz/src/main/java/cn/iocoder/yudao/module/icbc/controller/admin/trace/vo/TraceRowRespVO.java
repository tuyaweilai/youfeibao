package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台 - 关联单据查询的一行：一张收购单的完整链路（#55 T17）。
 *
 * <p>一行 = 一次物理交接里的一个品类（一张收购单）。四栏 + 付款 / 发票在 {@link #stages} 里按固定顺序铺开，
 * 一对多的环节在阶段内展开全部明细；差异 / 缺失关联在 {@link #differences} 里逐条说明（AC4）。
 */
@Schema(description = "管理后台 - 关联单据查询行（一张收购单）")
@Data
public class TraceRowRespVO {

    // ==================== 追溯锚点 ====================

    @Schema(description = "追溯锚点类型：HANDOVER_BATCH-有交接批次，ACQUISITION-无批次（历史数据 / 直接登记）", example = "HANDOVER_BATCH")
    private String anchorType;

    @Schema(description = "交接批次编号")
    private Long handoverBatchId;

    @Schema(description = "交接批次号")
    private String handoverBatchNo;

    @Schema(description = "收购单编号")
    private Long acquisitionId;

    @Schema(description = "收购单号")
    private String acquisitionNo;

    // ==================== 主体与货物 ====================

    @Schema(description = "出售者档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "品类名称")
    private String categoryName;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "计量单位")
    private String unit;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "场站名称")
    private String stationName;

    @Schema(description = "交易地点")
    private String tradeAddress;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "是否直接收购（未关联采购订单），采购订单环节显示为「无需该环节」", example = "false")
    private Boolean directAcquisition;

    // ==================== 关键事实（口径见各阶段） ====================

    @Schema(description = "实物量（接收量优先，无则净重；ADR 0028）", example = "14.7400")
    private BigDecimal physicalWeight;

    @Schema(description = "结算重量（唯一计价基准；ADR 0019）", example = "28.3300")
    private BigDecimal settlementWeight;

    @Schema(description = "已过账入库量累计", example = "14.7400")
    private BigDecimal stockedWeight;

    @Schema(description = "收购金额（元）", example = "12345.67")
    private BigDecimal amount;

    @Schema(description = "收购单状态，枚举 AcquisitionStatusEnum")
    private Integer acquisitionStatus;

    @Schema(description = "收购单状态名", example = "已付款")
    private String acquisitionStatusName;

    // ==================== 脱敏 ====================

    @Schema(description = "本次响应是否已按岗位权限放开敏感字段（false 表示已脱敏）", example = "false")
    private Boolean sensitiveUnmasked;

    @Schema(description = "敏感字段（税号 / 身份证 / 手机号 / 银行卡，按岗位权限脱敏）")
    private TraceSensitiveRespVO sensitive;

    // ==================== 四栏 + 付款 / 发票 ====================

    @Schema(description = "六个阶段，固定顺序：采购订单 / 现场收货 / 仓储入库 / 结算确认 / 付款 / 发票")
    private List<TraceStageRespVO> stages;

    @Schema(description = "差异 / 缺失关联提示（AC4：入库量与结算量不默认一对一）")
    private List<TraceDifferenceRespVO> differences;

    @Schema(description = "操作历史（按业务单据的时间线重建；不是系统操作日志）")
    private List<TraceHistoryRespVO> histories;

    @Schema(description = "附件（磅单 / 车头车尾照片 / 回单 / 发票原件 / 线下签字件等）")
    private List<TraceAttachmentRespVO> attachments;

    @Schema(description = "本行被截断未展示的明细条数（详情接口不截断，恒为 0）", example = "0")
    private Integer hiddenNodeCount;

}
