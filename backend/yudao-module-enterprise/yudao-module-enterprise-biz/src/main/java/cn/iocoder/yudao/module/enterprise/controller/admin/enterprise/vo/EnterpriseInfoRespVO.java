package cn.iocoder.yudao.module.enterprise.controller.admin.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 管理后台 - 企业信息 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业信息 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseInfoRespVO extends EnterpriseInfoBaseVO {

    @Schema(description = "企业ID", required = true, example = "1024")
    private Long id;

    @Schema(description = "企业状态", required = true, example = "2")
    private Integer status;

    @Schema(description = "最新审核备注", example = "资料齐全，审核通过")
    private String auditRemarks;

    @Schema(description = "创建时间", required = true)
    private LocalDateTime createTime;

} 