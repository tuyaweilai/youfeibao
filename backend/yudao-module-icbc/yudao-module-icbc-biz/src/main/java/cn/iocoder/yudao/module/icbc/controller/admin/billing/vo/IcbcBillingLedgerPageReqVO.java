package cn.iocoder.yudao.module.icbc.controller.admin.billing.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 平台计费计量台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class IcbcBillingLedgerPageReqVO extends PageParam {

    @Schema(description = "租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "计费期间（yyyy-MM）", example = "2026-09")
    private String periodMonth;

}
