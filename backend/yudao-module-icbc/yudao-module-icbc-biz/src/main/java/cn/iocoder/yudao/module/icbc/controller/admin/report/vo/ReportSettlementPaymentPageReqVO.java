package cn.iocoder.yudao.module.icbc.controller.admin.report.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 管理后台 - 结算付款表分页 Request VO（#57 T19）。
 */
@Schema(description = "管理后台 - 结算付款表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ReportSettlementPaymentPageReqVO extends PageParam {

    @Schema(description = "结算单号")
    private String settlementNo;

    @Schema(description = "出售者档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "确认状态：0-待确认，1-已确认，2-有异议，3-需线下签字确认，4-已线下签字确认")
    private Integer confirmStatus;

    @Schema(description = "付款办理进度编码：UNPAID-未办理，PROCESSING-办理中，SUCCESS-已支付，FAILED-异常")
    private String paymentProgress;

    @Schema(description = "回单状态编码：RECEIVED-已回单，PENDING-未回单")
    private String receiptStatus;

    @Schema(description = "生成时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] generateTime;

}
