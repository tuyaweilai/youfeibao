package cn.iocoder.yudao.module.erp.api.stock.dto;

import lombok.Data;

/**
 * ERP 库存只读查询条件（#57 T19）。
 *
 * <p>维度与 {@code erp_stock} / {@code erp_stock_record} 的余额维度一致：品类 + 仓库 + 库位 + 批次。
 * 库位 / 批次编号为 {@code 0} 表示「未指定」；为 {@code null} 表示不限。
 *
 * @author 芋道源码
 */
@Data
public class StockReportQueryDTO {

    /**
     * 品类编号（icbc_goods_config.id）
     */
    private Long goodsConfigId;

    /**
     * 仓库编号；为空表示不限
     */
    private Long warehouseId;

    /**
     * 库位编号（0 = 未指定）；为空表示不限
     */
    private Long locationId;

    /**
     * 批次编号（0 = 未指定）；为空表示不限
     */
    private Long batchId;

    /**
     * 业务类型（仅入出流水用，枚举 {@code ErpStockRecordBizTypeEnum}）；为空表示不限
     */
    private Integer bizType;

    /**
     * 业务单号（仅入出流水用）；为空表示不限
     */
    private String bizNo;

    /**
     * 页码，从 1 开始
     */
    private Integer pageNo = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

}
