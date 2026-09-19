package cn.iocoder.yudao.module.icbc.controller.admin.settlement.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 结算单分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SettlementPageReqVO extends PageParam {

    @Schema(description = "出售者档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "自然人主体编号", example = "2048")
    private Long naturalPersonId;

    @Schema(description = "确认状态：0-待确认，1-已确认，2-有异议，3-需线下签字确认，4-已线下签字确认", example = "0")
    private Integer confirmStatus;

    @Schema(description = "结算单号", example = "ST202612010001")
    private String settlementNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "生成时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] generateTime;

}
