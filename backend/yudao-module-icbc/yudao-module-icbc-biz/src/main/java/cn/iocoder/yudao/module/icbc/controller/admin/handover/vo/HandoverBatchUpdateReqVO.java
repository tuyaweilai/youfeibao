package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 管理后台 / 收货员现场端 - 补录交接批次 Request VO（#50 T12）。
 *
 * <p>只补录「现场当时没填全」的那几项（司机、车牌、地点、时间、来源方式）；交易对方不在这里改
 * （它决定这笔货是谁的，改了会让已挂的收购单对不上人）。
 */
@Schema(description = "管理后台 - 交接批次补录 Request VO")
@Data
public class HandoverBatchUpdateReqVO {

    @Schema(description = "批次编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "批次编号不能为空")
    private Long id;

    @Schema(description = "场站编号（到场收货填；上门回收留空并填上门地址）")
    private Long stationId;

    @Schema(description = "上门地址")
    private String visitAddress;

    @Schema(description = "交接时间")
    private LocalDateTime occurTime;

    @Schema(description = "来源方式：APPOINTMENT / WALK_IN / ON_SITE")
    private String sourceType;

    @Schema(description = "司机姓名")
    private String driverName;

    @Schema(description = "司机手机号")
    private String driverMobile;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "备注")
    private String remark;

}
