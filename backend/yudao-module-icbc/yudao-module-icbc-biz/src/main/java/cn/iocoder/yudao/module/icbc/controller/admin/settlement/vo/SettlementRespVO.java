package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结算单 Response VO（企业管理端与自然人端共用；自然人不该看的字段这里没有）。
 */
@Schema(description = "管理后台 - 结算单 Response VO")
@Data
public class SettlementRespVO {

    @Schema(description = "结算单编号", example = "1024")
    private Long id;

    @Schema(description = "结算单号", example = "ST202612010001")
    private String settlementNo;

    @Schema(description = "出售者档案编号", example = "2048")
    private Long payeeId;

    @Schema(description = "自然人主体编号", example = "4096")
    private Long naturalPersonId;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "出售者联系方式", example = "13800138000")
    private String sellerMobile;

    @Schema(description = "场站编号（一次到场批次按「出售者 + 场站」聚合）", example = "3072")
    private Long stationId;

    @Schema(description = "场站名称快照", example = "城东收货点")
    private String stationName;

    @Schema(description = "生成时间")
    private LocalDateTime generateTime;

    @Schema(description = "当前生效版本编号", example = "1")
    private Long currentVersionId;

    @Schema(description = "当前生效版本号", example = "1")
    private Integer currentVersionNo;

    // ==================== 确认与异议 ====================

    @Schema(description = "确认状态：0-待确认，1-已确认，2-有异议，3-需线下签字确认，4-已线下签字确认", example = "0")
    private Integer confirmStatus;

    @Schema(description = "确认状态名", example = "待确认")
    private String confirmStatusName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "确认时快照哈希")
    private String confirmHash;

    @Schema(description = "最近一次异议原因", example = "02")
    private String disputeReason;

    @Schema(description = "最近一次异议原因名", example = "扣杂不符")
    private String disputeReasonName;

    @Schema(description = "异议说明")
    private String disputeNote;

    @Schema(description = "异议时间")
    private LocalDateTime disputeTime;

    @Schema(description = "累计异议次数", example = "1")
    private Integer disputeCount;

    @Schema(description = "企业处理说明（不改但附说明时）")
    private String enterpriseReplyNote;

    @Schema(description = "企业最近一次处理时间")
    private LocalDateTime enterpriseReplyTime;

    @Schema(description = "企业是否尚未回复（异议后超过处理时限）", example = "false")
    private Boolean enterpriseNotReplied;

    @Schema(description = "连续异议 3 次以上：建议转线下签字", example = "false")
    private Boolean suggestOffline;

    @Schema(description = "下一步动作截止时间")
    private LocalDateTime deadlineTime;

    // ==================== 线下签字 ====================

    @Schema(description = "线下签字确认书附件地址")
    private String offlineSignFileUrl;

    @Schema(description = "办理人")
    private String offlineSignHandler;

    @Schema(description = "线下签字确认时间")
    private LocalDateTime offlineSignTime;

    // ==================== 汇总（当前版本） ====================

    @Schema(description = "本单收购单条数", example = "2")
    private Integer acquisitionCount;

    @Schema(description = "合计结算重量", example = "24500.00")
    private BigDecimal totalSettlementWeight;

    @Schema(description = "合计金额", example = "49000.00")
    private BigDecimal totalAmount;

    @Schema(description = "是否已结清：其下收购单都已开票（由其下收购单推导，不落库）", example = "false")
    private Boolean settled;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "各版本（倒序）")
    private List<VersionVO> versions;

    @Schema(description = "当前版本的逐条收购单明细")
    private List<LineVO> lines;

    @Schema(description = "管理后台 - 结算单版本")
    @Data
    public static class VersionVO {
        private Long id;
        private Integer versionNo;
        private String snapshotHash;
        private String changeReason;
        private String changedBy;
        private String source;
        private BigDecimal totalSettlementWeight;
        private BigDecimal totalAmount;
        private Integer acquisitionCount;
        private LocalDateTime createTime;
    }

    @Schema(description = "管理后台 - 结算单明细行（收购单口径）")
    @Data
    public static class LineVO {
        private Long acquisitionId;
        private String acquisitionNo;
        private String categoryName;
        private String unit;
        private BigDecimal quantity;
        private BigDecimal grossWeight;
        private BigDecimal tareWeight;
        private BigDecimal deduction;
        private String deductionMethod;
        private BigDecimal settlementWeight;
        private BigDecimal unitPrice;
        private BigDecimal adjustmentAmount;
        private String adjustmentReason;
        private BigDecimal amount;
        private Integer status;
        private String statusName;
        @Schema(description = "作废原因（作废对自然人可见）")
        private String cancelReason;
    }

}
