package cn.iocoder.yudao.module.enterprise.controller.app.enterprise.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 产废企业用户绑定 Request VO
 *
 * @author 芋道源码
 */
@Schema(description = "APP - 产废企业用户绑定 Request VO")
@Data
public class AppEnterpriseBindReqVO {

    @Schema(description = "企业ID", required = true, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "门店ID", example = "2048")
    private Long storeId;

    @Schema(description = "关系类型", example = "1")
    private Integer relationType;

    @Schema(description = "是否企业主联系人", example = "false")
    private Boolean isPrimaryContact;
} 