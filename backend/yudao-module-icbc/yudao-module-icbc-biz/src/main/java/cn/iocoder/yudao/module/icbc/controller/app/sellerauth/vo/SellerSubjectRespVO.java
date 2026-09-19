package cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 自然人出售者 - 当前登录名下的自然人主体 Response VO。
 *
 * <p>身份证与手机号脱敏展示：他看的是「我操作的是谁」，不是可以复制走的证件号。
 */
@Schema(description = "自然人出售者 - 自然人主体 Response VO")
@Data
public class SellerSubjectRespVO {

    @Schema(description = "自然人主体编号", example = "1024")
    private Long naturalPersonId;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "身份证件号码（脱敏）", example = "110101********1234")
    private String idCardNo;

    @Schema(description = "手机号（脱敏）", example = "138****8000")
    private String mobile;

    @Schema(description = "实人认证状态：0-未认证，1-认证中，2-认证通过，3-认证未通过", example = "2")
    private Integer realNameStatus;

    @Schema(description = "实人认证状态名", example = "认证通过")
    private String realNameStatusName;

}
