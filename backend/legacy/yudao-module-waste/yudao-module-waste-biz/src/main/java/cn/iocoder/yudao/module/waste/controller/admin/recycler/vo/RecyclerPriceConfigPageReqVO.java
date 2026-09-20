package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 回收企业价格配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecyclerPriceConfigPageReqVO extends PageParam {

    @Schema(description = "回收企业ID", example = "1024")
    private Long recyclingEnterpriseId;

    @Schema(description = "废物代码", example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", example = "医疗废物")
    private String wasteName;

    @Schema(description = "废物类别", example = "危险废物")
    private String wasteCategory;

    @Schema(description = "地区代码", example = "110000")
    private String regionCode;

    @Schema(description = "地区名称", example = "北京市")
    private String regionName;

    @Schema(description = "是否支持议价", example = "true")
    private Boolean negotiable;

    @Schema(description = "价格策略", example = "固定价格")
    private String pricingStrategy;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "生效日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] effectiveDate;

    @Schema(description = "失效日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] expiryDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 