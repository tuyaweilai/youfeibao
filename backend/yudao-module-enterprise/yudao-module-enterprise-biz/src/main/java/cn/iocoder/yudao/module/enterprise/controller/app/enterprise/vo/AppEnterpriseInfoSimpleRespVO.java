package cn.iocoder.yudao.module.enterprise.controller.app.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 企业信息 Simple Response VO，适用于APP端下拉选择等简单场景
 *
 * @author 芋道源码
 */
@Schema(description = "APP - 企业信息精简信息 Response VO")
@Data
public class AppEnterpriseInfoSimpleRespVO {

    @Schema(description = "企业ID", required = true, example = "1024")
    private Long id;

    @Schema(description = "企业名称", required = true, example = "芋道源码")
    private String name;

    @Schema(description = "企业类型", required = true, example = "1")
    private Integer enterpriseType;
} 