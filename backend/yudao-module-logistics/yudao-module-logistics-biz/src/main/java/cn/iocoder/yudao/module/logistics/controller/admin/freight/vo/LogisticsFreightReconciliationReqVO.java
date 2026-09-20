package cn.iocoder.yudao.module.logistics.controller.admin.freight.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 运费对账汇总的过滤条件（V8 #75）：**按合同与趟次汇集**。
 *
 * <p>它把符合条件的运费单全部取出，再在服务层按「承运商 + 合同」汇总——
 * 对账口径只有一处，避免 SQL 与 Java 两套算法漂移。
 */
@Schema(description = "管理后台 - 运费对账汇总 Request VO")
@Data
public class LogisticsFreightReconciliationReqVO {

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
