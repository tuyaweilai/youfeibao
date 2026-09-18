package cn.iocoder.yudao.module.icbc.controller.admin.callback.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 工行回调通知分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CallbackNotifyPageReqVO extends PageParam {

    @Schema(description = "通知ID", example = "NOTIFY123456")
    private String notifyId;

    @Schema(description = "通知类型", example = "PAYEE_AUDIT")
    private String notifyType;

    @Schema(description = "业务ID", example = "ORDER123456")
    private String businessId;

    @Schema(description = "处理状态", example = "1")
    private Integer processStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 