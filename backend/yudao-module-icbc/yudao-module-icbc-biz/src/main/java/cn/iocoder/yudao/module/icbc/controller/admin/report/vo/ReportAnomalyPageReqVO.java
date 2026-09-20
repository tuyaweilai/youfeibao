package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理后台 - 异常表分页 Request VO（#57 T19）。
 */
@Schema(description = "管理后台 - 异常表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReportAnomalyPageReqVO extends PageParam {

    @Schema(description = "异常类型：WEIGHT_DIFF / OVER_PURCHASE_QUANTITY / OVER_STOCK_IN / "
            + "DUPLICATE_LINK / LONG_UNCONFIRMED / MISSING_EVIDENCE；空表示全部类型合并")
    private String type;

    @Schema(description = "出售者档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "来源单号")
    private String bizNo;

    @Schema(description = "发生时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] time;

}
