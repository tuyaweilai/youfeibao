package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 承运商运费单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsFreightPageReqVO extends PageParam {

    @Schema(description = "运费单号（模糊）", example = "FR2026")
    private String freightNo;

    @Schema(description = "任务单号（模糊）", example = "TT2026")
    private String taskNo;

    @Schema(description = "承运商编号", example = "1")
    private Long carrierId;

    @Schema(description = "承运商名称（模糊）")
    private String carrierName;

    @Schema(description = "承运合同编号", example = "1")
    private Long contractId;

    @Schema(description = "状态：0-待确认应付，1-已确认应付，2-已登记付款凭证", example = "0")
    private Integer status;

    @Schema(description = "汇集时间（范围）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    public LocalDateTime getCreateTimeBegin() {
        return createTime != null && createTime.length > 0 ? createTime[0] : null;
    }

    public LocalDateTime getCreateTimeEnd() {
        return createTime != null && createTime.length > 1 ? createTime[1] : null;
    }

}
