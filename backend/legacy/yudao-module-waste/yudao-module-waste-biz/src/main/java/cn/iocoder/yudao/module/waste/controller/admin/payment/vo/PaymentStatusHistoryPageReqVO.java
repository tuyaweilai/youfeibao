package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 付款状态变更历史分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PaymentStatusHistoryPageReqVO extends PageParam {

    @Schema(description = "订单ID", example = "1024")
    private Long orderId;

    @Schema(description = "付款记录ID", example = "2048")
    private Long paymentRecordId;

    @Schema(description = "原状态", example = "1")
    private Integer fromStatus;

    @Schema(description = "新状态", example = "2")
    private Integer toStatus;

    @Schema(description = "操作人ID", example = "1001")
    private Long operatorId;

    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    @Schema(description = "操作人类型", example = "1")
    private Integer operatorType;

    @Schema(description = "是否系统自动变更", example = "false")
    private Boolean isSystemChange;

    @Schema(description = "变更时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] changeTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 