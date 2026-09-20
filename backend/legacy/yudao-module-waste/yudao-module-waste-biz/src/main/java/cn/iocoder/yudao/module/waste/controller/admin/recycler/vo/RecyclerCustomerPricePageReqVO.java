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

@Schema(description = "管理后台 - 回收企业客户专属价格配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecyclerCustomerPricePageReqVO extends PageParam {

    @Schema(description = "回收企业ID", example = "1024")
    private Long recyclingEnterpriseId;

    @Schema(description = "客户企业ID", example = "2048")
    private Long customerEnterpriseId;

    @Schema(description = "废物代码", example = "HW01")
    private String wasteCode;

    @Schema(description = "废物名称", example = "医疗废物")
    private String wasteName;

    @Schema(description = "价格类型", example = "1")
    private Integer priceType;

    @Schema(description = "合同ID", example = "3072")
    private Long contractId;

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