package cn.iocoder.yudao.module.contract.controller.admin.link.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 合同关联对象分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractLinkedObjectPageReqVO extends PageParam {

    @Schema(description = "合同ID", example = "1")
    private Long contractId;

    @Schema(description = "合同版本ID", example = "1")
    private Long versionId;

    @Schema(description = "关联对象类型", example = "waste_transfer")
    private String objectType;

    @Schema(description = "关联类型", example = "0")
    private Integer linkType;

    @Schema(description = "关联状态", example = "0")
    private Integer linkStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 