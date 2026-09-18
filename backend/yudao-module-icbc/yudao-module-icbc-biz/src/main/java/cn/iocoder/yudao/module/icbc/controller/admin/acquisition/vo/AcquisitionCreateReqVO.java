package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

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

    @Schema(description = "品类配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long goodsConfigId;

    @Schema(description = "规格", example = "重型")
    private String specification;

    @Schema(description = "数量", example = "12.5")
    private BigDecimal quantity;

    @Schema(description = "含税单价（元）", example = "2600.00")
    private BigDecimal unitPrice;

    @Schema(description = "金额（元）；不传时按「数量 × 单价」计算", example = "32500.00")
    private BigDecimal amount;

    @Schema(description = "毛重", example = "18000.00")
    private BigDecimal grossWeight;

    @Schema(description = "皮重", example = "5500.00")
    private BigDecimal tareWeight;

    @Schema(description = "净重；不传时按「毛重 − 皮重」计算", example = "12500.00")
    private BigDecimal netWeight;

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
