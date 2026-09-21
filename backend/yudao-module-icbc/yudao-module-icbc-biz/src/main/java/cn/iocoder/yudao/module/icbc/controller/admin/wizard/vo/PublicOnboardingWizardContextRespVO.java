package cn.iocoder.yudao.module.icbc.controller.admin.wizard.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自填建档链接的上下文（#94）。
 *
 * <p>打开链接时先验令牌，把这个人的手机要看到的**这一笔建档所需信息**返回去：有效期。
 * 它**不返回租户内的任何其它数据**——链接是免注册的，只能显示这一笔建档本身。
 *
 * <p>只回有效期：页面上真的会显示它（#94 复审 ST-4）。用途由入口 URL 决定，不需要后端再回一份
 * 没人用的字段。
 */
@Schema(description = "公开端点 - 本人自填建档：链接上下文")
@Data
@Builder
public class PublicOnboardingWizardContextRespVO {

    @Schema(description = "链接有效期至；到点后需收货员重新生成")
    private LocalDateTime expiresTime;

}
