package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 回收企业业务模式配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecyclerBusinessConfigPageReqVO extends PageParam {

    @Schema(description = "回收企业ID", example = "1024")
    private Long recyclingEnterpriseId;

    @Schema(description = "业务模式", example = "1")
    private Integer businessMode;

    @Schema(description = "报价模式", example = "1")
    private Integer quotationMode;

    @Schema(description = "客户模式选择", example = "1")
    private Integer customerModeSelection;

    @Schema(description = "是否启用自动接受", example = "true")
    private Boolean autoAcceptEnabled;

    @Schema(description = "是否允许价格协商", example = "true")
    private Boolean priceNegotiationEnabled;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 