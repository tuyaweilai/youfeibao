package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 登录 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthLoginRespVO {

    @Schema(description = "用户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long userId;

    @Schema(description = "访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "happy")
    private String accessToken;

    @Schema(description = "刷新令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "nice")
    private String refreshToken;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expiresTime;

    @Schema(description = "企业绑定状态。" +
            " 0: 无需处理 (例如超级管理员), " +
            " 1: 自动绑定成功, " +
            " 2: 需要手动认证/引导至企业中心, " +
            " 3: 已绑定且是默认企业 (或唯一企业), " +
            " 4: 已绑定但非默认企业 (存在多个企业且当前绑定的不是默认), " +
            " 5: 用户已有关联企业，但当前登录的部门未直接关联到这些企业, " +
            " 9: 绑定过程发生错误",
            example = "1")
    private Integer enterpriseBindingStatus;

    @Schema(description = "绑定的企业ID (如果 enterpriseBindingStatus 为 1, 3, 4 时)", example = "2048")
    private Long boundEnterpriseId;

    @Schema(description = "绑定的企业名称 (如果 enterpriseBindingStatus 为 1, 3, 4 时)", example = "某某科技有限公司")
    private String boundEnterpriseName;

    @Schema(description = "企业绑定相关的提示信息", example = "已为您自动关联到某某科技有限公司")
    private String enterpriseBindingMessage;
}
