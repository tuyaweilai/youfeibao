package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理后台 - 称量差异清单分页 Request VO（#53 T15，只读）。
 *
 * <p>差异 = 实物量（接收量优先，无则净重）− 结算重量（ADR 0028：结算重量只作计价基准）。
 * 本清单只读，供 #57 的异常表消费；差异**不静默抹平**。
 */
@Schema(description = "管理后台 - 称量差异清单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AcquisitionWeightDiffPageReqVO extends PageParam {

    @Schema(description = "出售者档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "收购单号", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "只看有差异的（差异 ≠ 0）；null-不限", example = "true")
    private Boolean hasDifference;

    @Schema(description = "只看已做接收结论的（有接收量）；null-不限", example = "true")
    private Boolean onlyAccepted;

    @Schema(description = "交易时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] tradeTime;

}
