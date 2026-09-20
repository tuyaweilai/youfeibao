package cn.iocoder.yudao.module.icbc.controller.admin.handover.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 交接批次分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HandoverBatchPageReqVO extends PageParam {

    @Schema(description = "出售者（交易对方）档案编号")
    private Long payeeId;

    @Schema(description = "场站编号")
    private Long stationId;

    @Schema(description = "来源方式：APPOINTMENT / WALK_IN / ON_SITE")
    private String sourceType;

    @Schema(description = "车牌号")
    private String plateNo;

    @Schema(description = "交接时间起")
    private LocalDateTime occurTimeStart;

    @Schema(description = "交接时间止")
    private LocalDateTime occurTimeEnd;

}
