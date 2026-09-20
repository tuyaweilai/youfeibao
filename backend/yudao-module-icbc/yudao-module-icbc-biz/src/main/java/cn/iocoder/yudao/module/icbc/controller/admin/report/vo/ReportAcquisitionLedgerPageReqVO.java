package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理后台 - 收购台账分页 Request VO（#57 T19）。
 */
@Schema(description = "管理后台 - 收购台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReportAcquisitionLedgerPageReqVO extends PageParam {

    @Schema(description = "收购单号")
    private String acquisitionNo;

    @Schema(description = "出售者档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "场站编号")
    private Long stationId;

    @Schema(description = "品类名称")
    private String categoryName;

    @Schema(description = "回收方式：true-直接收购，false-关联采购订单，null-不限")
    private Boolean directAcquisition;

    @Schema(description = "交易时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] tradeTime;

}
