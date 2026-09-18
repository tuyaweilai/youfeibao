package cn.iocoder.yudao.module.contract.controller.admin.template.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 合同模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractTemplatePageReqVO extends PageParam {

    @Schema(description = "模板名称", example = "危废处置合同模板")
    private String templateName;

    @Schema(description = "模板编码", example = "TEMPLATE_001")
    private String templateCode;

    @Schema(description = "合同类型ID", example = "1")
    private Long contractTypeId;

    @Schema(description = "是否启用", example = "true")
    private Boolean isActive;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 