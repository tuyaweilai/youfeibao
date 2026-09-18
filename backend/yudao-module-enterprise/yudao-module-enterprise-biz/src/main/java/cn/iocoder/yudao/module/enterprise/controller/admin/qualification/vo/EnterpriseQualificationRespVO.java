package cn.iocoder.yudao.module.enterprise.controller.admin.qualification.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 管理后台 - 企业资质 Response VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 企业资质 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EnterpriseQualificationRespVO extends EnterpriseQualificationBaseVO {

    @Schema(description = "资质ID", required = true, example = "1024")
    private Long id;

    @Schema(description = "资质状态", required = true, example = "1")
    private Integer status;

    @Schema(description = "审核备注", example = "资料真实有效，审核通过")
    private String auditRemarks;

    @Schema(description = "创建时间", required = true)
    private LocalDateTime createTime;

} 