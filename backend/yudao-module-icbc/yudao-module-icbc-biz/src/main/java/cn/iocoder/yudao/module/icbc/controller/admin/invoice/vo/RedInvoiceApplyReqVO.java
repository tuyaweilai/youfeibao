package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

/**
 * 红字冲销开票请求 VO
 */
@Schema(description = "管理后台 - 红字冲销开票请求 VO")
@Data
public class RedInvoiceApplyReqVO {

    @Schema(description = "原蓝字合作方订单ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "ACQ202312010001")
    @NotEmpty(message = "原蓝字合作方订单ID不能为空")
    @Size(max = 35, message = "合作方订单ID长度不能超过35个字符")
    private String partnerOrderId;

    @Schema(description = "红冲原因：01 开票有误，02 销货退回，03 服务中止，04 销售折让",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "01")
    @NotEmpty(message = "红冲原因不能为空")
    private String reason;

    @Schema(description = "红冲金额；开票有误必须等于蓝票金额（留空时按蓝票金额全额红冲）", example = "1000.00")
    private BigDecimal amount;

    @Schema(description = "红冲明细；开票有误时留空，平台按蓝票明细自动全额生成", example = "[]")
    @Valid
    private List<Goods> goods;

    @Schema(description = "工行页面回跳地址前缀", example = "https://example.com")
    private String jumpUrlBase;

    @Schema(description = "是否重试（对失败的红冲重新发起时置 Y）", example = "N")
    private String isRedo;

    @Schema(description = "备注", example = "开票金额写错")
    private String remark;

    @Schema(description = "红冲明细")
    @Data
    public static class Goods {

        @Schema(description = "红冲明细序号", example = "1")
        private String goodsSeqno;

        @Schema(description = "对应蓝票明细序号", example = "1")
        private String blueGoodsSeqno;

        @Schema(description = "项目名称", example = "废钢铁")
        private String projectName;

        @Schema(description = "数量", example = "1")
        private String goodsNum;

        @Schema(description = "金额", example = "1000.00")
        private String goodsAmt;

        @Schema(description = "重量", example = "1.00")
        private String weight;

        @Schema(description = "单价", example = "1000.00")
        private String price;

        @Schema(description = "单位", example = "吨")
        private String units;
    }
}
