package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 运输费用（内部成本）分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsTransportCostPageReqVO extends PageParam {

    @Schema(description = "运输任务编号", example = "1")
    private Long taskId;

    @Schema(description = "任务单号（模糊）", example = "TT2026")
    private String taskNo;

    @Schema(description = "费用类型：1-路桥费，2-燃油费，3-其他", example = "1")
    private Integer costType;

    @Schema(description = "承担方：1-承运商承担，2-本企业承担", example = "2")
    private Integer bearer;

}
