package cn.iocoder.yudao.module.icbc.controller.app.appointment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自然人出售者 - 发起到站预约 Request VO（ADR 0020）。
 *
 * <p>他只声明「什么时候、去哪个场站、卖什么、大约多少、车牌多少」；这不是下单，
 * 平台不做撮合，也没有「企业接受 / 拒绝」。
 */
@Schema(description = "自然人出售者端 - 发起到站预约 Request VO")
@Data
public class AppointmentCreateReqVO {

    @Schema(description = "自然人主体编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "自然人主体编号不能为空")
    private Long naturalPersonId;

    @Schema(description = "场站码（场站二维码里那个码）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String stationCode;

    @Schema(description = "品类配置编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "品类不能为空")
    private Long goodsConfigId;

    @Schema(description = "预计数量（可空；界面一律以「约」标注）")
    private BigDecimal expectedQuantity;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "预计到站时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expectedArrivalTime;

    @Schema(description = "备注")
    private String remark;

}
