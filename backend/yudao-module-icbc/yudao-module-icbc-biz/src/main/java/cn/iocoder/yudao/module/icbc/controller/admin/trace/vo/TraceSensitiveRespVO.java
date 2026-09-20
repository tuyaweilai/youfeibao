package cn.iocoder.yudao.module.icbc.controller.admin.trace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 关联单据查询的敏感字段（#55 T17，AC6）。
 *
 * <p>税号 / 身份证 / 手机号 / 银行卡按岗位权限脱敏：没有
 * {@code icbc:trace:sensitive:view} 时返回脱敏值，有则返回原值。导出走同一套判断。
 */
@Schema(description = "管理后台 - 关联单据查询敏感字段")
@Data
public class TraceSensitiveRespVO {

    @Schema(description = "回收企业纳税人识别号（税号）", example = "9111**********78M")
    private String buyerTaxNo;

    @Schema(description = "出售者身份证件号码", example = "110101********1234")
    private String sellerIdCard;

    @Schema(description = "出售者手机号", example = "138****9999")
    private String sellerMobile;

    @Schema(description = "出售者银行卡（脱敏时只给尾号，放开时给全号）", example = "****1234")
    private String sellerBankCard;

}
