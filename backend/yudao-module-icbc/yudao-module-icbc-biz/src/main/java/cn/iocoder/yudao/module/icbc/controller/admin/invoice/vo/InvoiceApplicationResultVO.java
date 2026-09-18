package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 单笔开票申请结果。
 *
 * <p>批量申请里每个收购单一条，成功与失败各自成行，互不影响。成功时带回自然人确认页面
 * 表单（{@code confirmPageHtml}）；失败时带回具体不满足的校验项与补齐方式。
 */
@Schema(description = "管理后台 - 开票申请结果")
@Data
public class InvoiceApplicationResultVO {

    @Schema(description = "收购单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long acquisitionId;

    @Schema(description = "收购单号", example = "ACQ202601011200001234")
    private String acquisitionNo;

    @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean success;

    @Schema(description = "是否为重复发起（同一收购单已申请过，未产生第二笔业务）", example = "false")
    private Boolean duplicate;

    @Schema(description = "合作方订单号", example = "ACQ202601011200001234")
    private String partnerOrderId;

    @Schema(description = "订单号（我方生成）", example = "INV17300000000001234")
    private String orderNo;

    @Schema(description = "自然人确认页面表单 HTML（交给出售者在工行页面上确认）")
    private String confirmPageHtml;

    @Schema(description = "自然人确认状态：0-未确认，1-自然人确认完成，2-全部确认完成", example = "0")
    private Integer confirmStatus;

    @Schema(description = "预开票状态：0-初始，1-预开票中，2-预开票成功，3-预开票失败，4-预开票取消", example = "1")
    private Integer preInvoiceStatus;

    @Schema(description = "订单状态：0-待确认，1-已确认，2-已支付，3-已开票，4-已完成，9-已取消", example = "0")
    private Integer orderStatus;

    @Schema(description = "结果说明")
    private String message;

    @Schema(description = "未通过的校验项（成功时为空）")
    private List<InvoicePreCheckItemVO> failures;
}
