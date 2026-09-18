package cn.iocoder.yudao.module.icbc.controller.admin.evidence.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 批量证据操作的取数范围：给了一组订单号就按订单号取，否则按创建时间范围取。
 */
@Schema(description = "管理后台 - 证据批量范围 Request VO")
@Data
public class EvidenceScopeReqVO {

    @Schema(description = "合作方订单号集合；为空则按时间范围取", example = "[\"ORDER_20231201_001\"]")
    private List<String> partnerOrderIds;

    @Schema(description = "创建时间范围（partnerOrderIds 为空时生效）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
