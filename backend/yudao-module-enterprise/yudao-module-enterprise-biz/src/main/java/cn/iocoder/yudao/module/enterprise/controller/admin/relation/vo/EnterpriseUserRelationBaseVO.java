package cn.iocoder.yudao.module.enterprise.controller.admin.relation.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 用户企业关系 Base VO，提供给添加、修改、详细的子 VO 使用
 *
 * @author 芋道源码
 */
@Data
public class EnterpriseUserRelationBaseVO {

    @Schema(description = "用户ID", required = true, example = "1024")
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Schema(description = "企业ID", required = true, example = "1024")
    @NotNull(message = "企业ID不能为空")
    private Long enterpriseId;

    @Schema(description = "门店ID", example = "2048")
    private Long storeId;

    @Schema(description = "关系类型", required = true, example = "1")
    @NotNull(message = "关系类型不能为空")
    private Integer relationType;

    @Schema(description = "是否企业主联系人", required = true, example = "false")
    private Boolean isPrimaryContact;

    @Schema(description = "是否用户默认操作企业", required = true, example = "false")
    private Boolean isDefaultEnterprise;
} 