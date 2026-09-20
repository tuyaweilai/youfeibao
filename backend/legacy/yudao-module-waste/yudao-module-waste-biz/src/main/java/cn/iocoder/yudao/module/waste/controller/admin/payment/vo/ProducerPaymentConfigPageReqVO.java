package cn.iocoder.yudao.module.waste.controller.admin.payment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 产废企业付款配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ProducerPaymentConfigPageReqVO extends PageParam {

    @Schema(description = "企业ID", example = "1024")
    private Long enterpriseId;

    @Schema(description = "门店ID", example = "2048")
    private Long storeId;

    @Schema(description = "付款方式", example = "1")
    private Integer paymentMethod;

    @Schema(description = "是否默认配置", example = "false")
    private Boolean isDefault;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 