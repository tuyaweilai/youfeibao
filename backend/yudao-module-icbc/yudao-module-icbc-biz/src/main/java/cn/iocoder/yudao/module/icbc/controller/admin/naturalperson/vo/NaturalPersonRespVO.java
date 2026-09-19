package cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自然人主体 Response VO（平台运营）。
 *
 * <p>身份证件号码与手机号按脱敏展示：平台运营看到的是「是谁」，不是可以复制走的原始证件号。
 */
@Schema(description = "平台运营 - 自然人主体 Response VO")
@Data
public class NaturalPersonRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "平台级外部用户编号（工行 outUserId）", example = "NP0f1e2d3c")
    private String outUserId;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "身份证件号码（脱敏）", example = "1101**********1234")
    private String idCardNo;

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    private String mobile;

    @Schema(description = "实人认证状态", example = "2")
    private Integer realNameStatus;

    @Schema(description = "实人认证状态名", example = "认证通过")
    private String realNameStatusName;

    @Schema(description = "实人认证时间")
    private LocalDateTime realNameTime;

    @Schema(description = "状态：0-正常，1-已停用", example = "0")
    private Integer status;

    @Schema(description = "备注（平台运营处置原因）")
    private String remark;

    @Schema(description = "已绑定的登录凭证（会员用户编号）")
    private List<Long> memberUserIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
