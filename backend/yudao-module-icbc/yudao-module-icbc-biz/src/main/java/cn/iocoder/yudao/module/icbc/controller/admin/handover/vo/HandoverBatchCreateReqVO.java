package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 管理后台 / 收货员现场端 - 登记交接批次 Request VO（#50 T12）。
 *
 * <p>交易对方、车牌必填；**场站与上门地址至少填一个**（到场收货填场站、上门回收填地址）。
 * 预约与采购订单都是可选的关联，零散收购不必虚造。
 */
@Schema(description = "管理后台 - 交接批次登记 Request VO")
@Data
public class HandoverBatchCreateReqVO {

    @Schema(description = "出售者（交易对方）档案编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "交易对方不能为空")
    private Long payeeId;

    @Schema(description = "场站编号（到场收货填；上门回收留空并填上门地址）", example = "3072")
    private Long stationId;

    @Schema(description = "上门地址（上门回收填；到场收货留空并用场站）", example = "北京市朝阳区某某路 1 号")
    private String visitAddress;

    @Schema(description = "交接时间；不填取登记时刻")
    private LocalDateTime occurTime;

    @Schema(description = "来源方式：APPOINTMENT-预约到站，WALK_IN-直接到场（默认），ON_SITE-上门回收", example = "WALK_IN")
    private String sourceType;

    @Schema(description = "司机姓名（运输信息，不参与确认与收款）", example = "李师傅")
    private String driverName;

    @Schema(description = "司机手机号（运输信息）", example = "13800138000")
    private String driverMobile;

    @Schema(description = "车牌号", requiredMode = Schema.RequiredMode.REQUIRED, example = "京A12345")
    @NotBlank(message = "车牌号不能为空")
    private String plateNo;

    @Schema(description = "关联的到站预约编号（可空；预约不是订单）", example = "4096")
    private Long appointmentId;

    @Schema(description = "关联的采购订单编号（可空；不填即「直接收购」）", example = "5120")
    private Long purchaseOrderId;

    @Schema(description = "备注")
    private String remark;

}
