package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 待入库分页 Request VO（#52 T14）。
 *
 * <p>待入库 = 已验收（已归入结算单）、未作废、且累计入库尚未达到可入库实物量的收购单。
 */
@Schema(description = "管理后台 - 待入库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StockInPendingPageReqVO extends PageParam {

    @Schema(description = "收购单号", example = "ACQ2026")
    private String acquisitionNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

}
