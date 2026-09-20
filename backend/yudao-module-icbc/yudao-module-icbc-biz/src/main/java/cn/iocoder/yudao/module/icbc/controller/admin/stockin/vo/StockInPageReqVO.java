package cn.iocoder.yudao.module.icbc.controller.admin.stockin.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 入库单分页 Request VO（#52 T14）。
 */
@Schema(description = "管理后台 - 入库单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class StockInPageReqVO extends PageParam {

    @Schema(description = "入库单号", example = "SI2026")
    private String stockInNo;

    @Schema(description = "收购单号", example = "ACQ2026")
    private String acquisitionNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "品类编号", example = "1")
    private Long goodsConfigId;

    @Schema(description = "状态：0-待过账，1-已过账，2-已作废", example = "1")
    private Integer status;

}
