package cn.iocoder.yudao.module.contract.controller.admin.party.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 合同参与方分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ContractPartyPageReqVO extends PageParam {

    @Schema(description = "合同ID", example = "1")
    private Long contractId;

    @Schema(description = "参与企业ID", example = "2")
    private Long enterpriseId;

    @Schema(description = "企业名称", example = "测试企业")
    private String enterpriseName;

    @Schema(description = "签署状态", example = "0")
    private Integer signStatus;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

} 