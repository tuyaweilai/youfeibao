package cn.iocoder.yudao.module.icbc.controller.app.seller.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 自然人端 - 我的资料。
 *
 * <p>收款账户只显示尾号；变更银行卡属 S7；客服电话是兜底出口。
 */
@Schema(description = "自然人端 - 我的资料")
@Data
public class SellerProfileRespVO {

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "脱敏手机号", example = "138****8000")
    private String mobileMasked;

    @Schema(description = "脱敏身份证号", example = "110101********1234")
    private String idCardMasked;

    @Schema(description = "实人认证状态名", example = "已实名")
    private String realNameStatusName;

    @Schema(description = "客服电话（公开兜底出口）", example = "400-000-0000")
    private String serviceMobile;

    @Schema(description = "各回收企业登记的收款账户（只显示尾号）")
    private List<SellerBankCardVO> bankCards;

    @Schema(description = "注销说明：注销账号不等于删除交易记录")
    private String logoutNote;

    @Schema(description = "自然人端 - 收款账户（只显示尾号）")
    @Data
    public static class SellerBankCardVO {

        @Schema(description = "收方（出售者）档案编号", example = "1024")
        private Long payeeId;

        @Schema(description = "租户编号", example = "1")
        private Long tenantId;

        @Schema(description = "回收企业名称", example = "某某再生资源有限公司")
        private String enterpriseName;

        @Schema(description = "开户行", example = "中国工商银行")
        private String bankName;

        @Schema(description = "银行卡尾号", example = "1234")
        private String cardTail;

        @Schema(description = "收款账户变更状态：0-银行审核中，1-已生效，2-已拒绝，9-已取消（无变更时为空）")
        private Integer changeStatus;

        @Schema(description = "收款账户变更状态名", example = "银行审核中")
        private String changeStatusName;

        @Schema(description = "变更发起时间")
        private LocalDateTime changeRequestedAt;

    }

}
