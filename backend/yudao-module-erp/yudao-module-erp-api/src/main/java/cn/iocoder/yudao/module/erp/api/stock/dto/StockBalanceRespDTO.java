package cn.iocoder.yudao.module.erp.api.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 在库余额（#57 T19 经营报表「库存表」）。
 *
 * <p>一行 = 一个「品类 + 仓库 + 库位 + 批次」的库存数。库龄由批次的入库时间推出；
 * 批次未指定时为空（无法算库龄，不拿 0 冒充）。
 *
 * @author 芋道源码
 */
@Data
public class StockBalanceRespDTO {

    /**
     * 库存行编号（erp_stock.id）
     */
    private Long id;

    /**
     * 品类编号
     */
    private Long goodsConfigId;

    /**
     * 仓库编号
     */
    private Long warehouseId;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 库位编号（0 = 未指定）
     */
    private Long locationId;

    /**
     * 库位名称（未指定时为空）
     */
    private String locationName;

    /**
     * 批次编号（0 = 未指定）
     */
    private Long batchId;

    /**
     * 批次编号文本（未指定时为空）
     */
    private String batchNo;

    /**
     * 批次入库时间（库龄基准；未指定批次时为空）
     */
    private LocalDateTime batchInTime;

    /**
     * 在库数量
     */
    private BigDecimal count;

}
