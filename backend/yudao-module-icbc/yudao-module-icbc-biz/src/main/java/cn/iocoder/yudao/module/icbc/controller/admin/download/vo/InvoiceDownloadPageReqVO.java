package cn.iocoder.yudao.module.icbc.controller.admin.download.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 工行发票下载记录分页 Request VO
 *
 * <p>列表的用途是「把已下载下来的发票原件直接捞出来下载」，所以三个条件都是可选的：
 * 不填就是全部发票。
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - 工行发票下载记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InvoiceDownloadPageReqVO extends PageParam {

    @Schema(description = "合作方订单号（模糊匹配）", example = "ACQ20260922DEMO01")
    private String partnerOrderId;

    @Schema(description = "发票号码（模糊匹配）", example = "25500123456789012341")
    private String invoiceNumber;

    @Schema(description = "下载状态", example = "2")
    private Integer downloadStatus;

}
