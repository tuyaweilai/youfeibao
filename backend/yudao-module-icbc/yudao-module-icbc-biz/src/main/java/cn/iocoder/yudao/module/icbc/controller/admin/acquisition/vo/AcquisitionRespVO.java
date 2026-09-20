package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 收购登记 Response VO
 */
@Schema(description = "管理后台 - 收购登记 Response VO")
@Data
public class AcquisitionRespVO {

    @Schema(description = "收购单编号", example = "1024")
    private Long id;

    @Schema(description = "收购单号（合同编号）", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "客户端幂等键")
    private String clientRequestId;

    @Schema(description = "出售者档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "卖方主体类型：1-自然人出售者，2-个体工商户，3-个人独资企业，4-合伙企业，5-企业法人，6-农民专业合作社", example = "1")
    private Integer sellerSubjectType;

    @Schema(description = "卖方主体类型名称", example = "自然人出售者")
    private String sellerSubjectTypeName;

    @Schema(description = "场站编号（一次到场批次按「出售者 + 场站」聚合）", example = "3072")
    private Long stationId;

    @Schema(description = "交接批次编号（#50）；为空表示这笔收购没有经过批次登记", example = "2048")
    private Long handoverBatchId;

    @Schema(description = "关联的采购订单编号（#51）；0 表示未关联", example = "5120")
    private Long purchaseOrderId;

    @Schema(description = "关联的采购订单明细编号（#51）；0 表示未关联", example = "6144")
    private Long purchaseOrderItemId;

    @Schema(description = "是否为「直接收购」：未关联任何采购安排的收购，报表 / 列表按此口径标注，不是失败也不是缺失", example = "true")
    private Boolean directAcquisition;

    @Schema(description = "采购安排口径文案：直接收购 / 采购订单", example = "直接收购")
    private String purchaseArrangementText;

    @Schema(description = "有效磅次编号（计量结果引用的就是它）", example = "4096")
    private Long weighingId;

    @Schema(description = "物流侧交接登记编号（上门提货的现场交接来源；为空表示不是上门提货）", example = "3072")
    private Long logisticsHandoverId;

    @Schema(description = "司机编号（物流侧编号；与姓名快照并存）", example = "77")
    private Long driverId;

    @Schema(description = "车辆编号（物流侧编号；与车牌快照并存）", example = "88")
    private Long vehicleId;

    @Schema(description = "要件状态：COMPLETE-已齐，PENDING-待补档（缺身份证或银行卡，付款与开票被门禁拦住）")
    private String documentStatus;

    @Schema(description = "要件状态名")
    private String documentStatusName;

    @Schema(description = "缺什么（待补档时说明）")
    private String documentGap;

    @Schema(description = "现场参考量快照（不是计量事实：计量取有效磅次）")
    private java.math.BigDecimal referenceQuantity;

    @Schema(description = "现场参考单价快照（未修正时即成交单价）")
    private java.math.BigDecimal referenceUnitPrice;

    @Schema(description = "修正现场参考价 / 参考量的原因（修正必填）")
    private String referenceFixReason;

    @Schema(description = "补档完成时间")
    private LocalDateTime documentCompletedAt;

    @Schema(description = "补档说明")
    private String documentCompleteRemark;

    @Schema(description = "有效磅次是第几次", example = "2")
    private Integer weighingSeqNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "出售者联系方式", example = "13800138000")
    private String sellerMobile;

    @Schema(description = "品类配置编号", example = "2048")
    private Long goodsConfigId;

    @Schema(description = "品类名称", example = "废钢")
    private String categoryName;

    @Schema(description = "计量单位", example = "吨")
    private String unit;

    @Schema(description = "税率", example = "0.01")
    private BigDecimal taxRate;

    @Schema(description = "计税方法：SIMPLE-简易，GENERAL-一般", example = "SIMPLE")
    private String taxMethod;

    @Schema(description = "税收分类合并编码", example = "1090101010000000000")
    private String mergedCode;

    @Schema(description = "规格", example = "重型")
    private String specification;

    @Schema(description = "数量", example = "12.5")
    private BigDecimal quantity;

    @Schema(description = "含税单价", example = "2600.00")
    private BigDecimal unitPrice;

    @Schema(description = "金额", example = "32500.00")
    private BigDecimal amount;

    @Schema(description = "毛重", example = "18000.00")
    private BigDecimal grossWeight;

    @Schema(description = "皮重", example = "5500.00")
    private BigDecimal tareWeight;

    @Schema(description = "净重", example = "12500.00")
    private BigDecimal netWeight;

    @Schema(description = "扣杂原始值", example = "200.00")
    private BigDecimal deduction;

    @Schema(description = "扣杂录法：WEIGHT-按重量，RATIO-按比例", example = "WEIGHT")
    private String deductionMethod;

    @Schema(description = "结算重量 = 毛重 − 皮重 − 扣杂（唯一计价基准）", example = "12300.00")
    private BigDecimal settlementWeight;

    @Schema(description = "调整项（元）", example = "-100.00")
    private BigDecimal adjustmentAmount;

    @Schema(description = "调整原因", example = "扣运费 100 元")
    private String adjustmentReason;

    @Schema(description = "数量口径说明")
    private String quantityNote;

    @Schema(description = "接收量（实际留下 / 进库的重量）；为空表示未做接收结论", example = "11500.00")
    private BigDecimal acceptedWeight;

    @Schema(description = "退回量（拒收部分不进应付、不进库存）", example = "500.00")
    private BigDecimal rejectedWeight;

    @Schema(description = "余货出场量（未接收、带离场站的余货）", example = "500.00")
    private BigDecimal residualWeight;

    @Schema(description = "拒收原因", example = "含水率超标，杂质过多")
    private String rejectReason;

    @Schema(description = "称量差异 = 实物量 − 结算重量（不静默抹平）", example = "-500.00")
    private BigDecimal weightDiff;

    @Schema(description = "司机姓名（运输信息）", example = "李师傅")
    private String driverName;

    @Schema(description = "司机手机号（运输信息）", example = "13800138000")
    private String driverMobile;

    @Schema(description = "磅单号", example = "WD20261201001")
    private String weightTicketNo;

    @Schema(description = "磅单照片地址")
    private String weightTicketImageUrl;

    @Schema(description = "磅单识别车牌", example = "京A12345")
    private String weightTicketPlateNo;

    @Schema(description = "车辆照片识别车牌", example = "京A12345")
    private String vehiclePlateNo;

    @Schema(description = "车牌比对结果：true-一致，false-不一致，null-无法比对")
    private Boolean plateMatched;

    @Schema(description = "车头照片地址")
    private String vehicleFrontImageUrl;

    @Schema(description = "车尾照片地址")
    private String vehicleRearImageUrl;

    @Schema(description = "交易地点", example = "北京市朝阳区再生资源回收站")
    private String tradeAddress;

    @Schema(description = "交易时间")
    private LocalDateTime tradeTime;

    @Schema(description = "结算方式", example = "银行转账，过磅后 3 日内结清")
    private String settlementMethod;

    @Schema(description = "状态：0-已登记，1-待付款，2-已付款，3-已开票，9-已取消", example = "0")
    private Integer status;

    @Schema(description = "状态名称", example = "已登记")
    private String statusName;

    @Schema(description = "关联开票合作方订单号", example = "ORDER_20261201_001")
    private String invoicePartnerOrderId;

    @Schema(description = "登记来源：ONLINE / OFFLINE_SYNC", example = "ONLINE")
    private String source;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
