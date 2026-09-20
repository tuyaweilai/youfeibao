package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.purchase.SellerSubjectTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理后台 - 收购登记创建 Request VO。
 *
 * <p>必须要件（出售者、品类、数量、金额、磅单）在 Service 层统一校验，缺哪一项要能说清，
 * 所以这里的 Bean Validation 只兜住「完全为空」的入参，不做逐项阻断。
 */
@Schema(description = "管理后台 - 收购登记创建 Request VO")
@Data
public class AcquisitionCreateReqVO {

    @Schema(description = "客户端幂等键（离线补传去重；建议客户端生成 UUID）", example = "9f1c2a7e-3b4d-4c5e-8f6a-0b1c2d3e4f50")
    private String clientRequestId;

    @Schema(description = "出售者（收方）档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "出售者不能为空")
    private Long payeeId;

    @Schema(description = "卖方主体类型：1-自然人出售者（默认，走反向开票），2~6-个体工商户等非自然人（非自然人一律拒收，走单位供货方与进项收票）", example = "1")
    @InEnum(value = SellerSubjectTypeEnum.class, message = "卖方主体类型不合法")
    private Integer sellerSubjectType;

    @Schema(description = "场站编号（ADR 0018：一次到场批次按「出售者 + 场站」聚合；历史数据可空）", example = "3072")
    private Long stationId;

    @Schema(description = "品类配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long goodsConfigId;

    @Schema(description = "规格", example = "重型")
    private String specification;

    @Schema(description = "数量", example = "12.5")
    private BigDecimal quantity;

    @Schema(description = "含税单价（元）", example = "2600.00")
    private BigDecimal unitPrice;

    @Schema(description = "金额（元）；默认按「结算重量 × 单价 + 调整项」计算", example = "32500.00")
    private BigDecimal amount;

    @Schema(description = "毛重", example = "18000.00")
    private BigDecimal grossWeight;

    @Schema(description = "皮重", example = "5500.00")
    private BigDecimal tareWeight;

    @Schema(description = "净重；不传时按「毛重 − 皮重」计算", example = "12500.00")
    private BigDecimal netWeight;

    @Schema(description = "扣杂原始值：按重量时是重量，按比例时是比例（0~1）", example = "200.00")
    private BigDecimal deduction;

    @Schema(description = "扣杂录法：WEIGHT-按重量（默认），RATIO-按比例", example = "WEIGHT")
    private String deductionMethod;

    @Schema(description = "调整项（元，可正可负；运费 / 补贴 / 折让）", example = "-100.00")
    private BigDecimal adjustmentAmount;

    @Schema(description = "调整原因；调整项非零时必填", example = "扣运费 100 元")
    private String adjustmentReason;

    @Schema(description = "数量口径说明（选填；结算重量计价时解释发票数量与磅单净重的差异）", example = "结算重量计价，含扣杂")
    private String quantityNote;

    @Schema(description = "司机姓名（运输信息，不参与确认与收款）", example = "李师傅")
    private String driverName;

    @Schema(description = "司机手机号（运输信息）", example = "13800138000")
    private String driverMobile;

    @Schema(description = "磅单号", example = "WD20261201001")
    private String weightTicketNo;

    @Schema(description = "磅单照片地址", example = "https://cdn.example.com/weight/1.jpg")
    private String weightTicketImageUrl;

    @Schema(description = "磅单识别出的车牌号", example = "京A12345")
    private String weightTicketPlateNo;

    @Schema(description = "车头车尾照片识别出的车牌号", example = "京A12345")
    private String vehiclePlateNo;

    @Schema(description = "车头照片地址", example = "https://cdn.example.com/vehicle/front.jpg")
    private String vehicleFrontImageUrl;

    @Schema(description = "车尾照片地址", example = "https://cdn.example.com/vehicle/rear.jpg")
    private String vehicleRearImageUrl;

    @Schema(description = "交易地点", example = "北京市朝阳区再生资源回收站")
    private String tradeAddress;

    @Schema(description = "交易时间（时间戳毫秒）", example = "1764547200000")
    private LocalDateTime tradeTime;

    @Schema(description = "结算方式", example = "银行转账，过磅后 3 日内结清")
    private String settlementMethod;

    @Schema(description = "登记来源：ONLINE-在线，OFFLINE_SYNC-离线补传", example = "ONLINE")
    private String source;

    @Schema(description = "备注", example = "现场目测含少量杂质")
    private String remark;

}
