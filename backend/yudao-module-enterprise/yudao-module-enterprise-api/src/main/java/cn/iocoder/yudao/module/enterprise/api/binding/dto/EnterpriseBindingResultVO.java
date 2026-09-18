package cn.iocoder.yudao.module.enterprise.api.binding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "企业绑定结果 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnterpriseBindingResultVO {

    @Schema(description = "企业绑定状态编码。见 EnterpriseBindingStatusEnum", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer enterpriseBindingStatus;

    @Schema(description = "绑定的企业ID (如果状态表示已绑定或新绑定)", example = "2048")
    private Long boundEnterpriseId;

    @Schema(description = "绑定的企业名称 (如果状态表示已绑定或新绑定)", example = "某某科技有限公司")
    private String boundEnterpriseName;

    @Schema(description = "企业绑定相关的提示信息", example = "已为您自动关联到某某科技有限公司")
    private String enterpriseBindingMessage;
} 