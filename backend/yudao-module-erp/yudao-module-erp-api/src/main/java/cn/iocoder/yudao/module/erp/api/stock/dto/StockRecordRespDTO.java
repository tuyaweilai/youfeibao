package cn.iocoder.yudao.module.erp.api.stock.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 入出流水（#57 T19 经营报表「库存表」）。
 *
 * <p>一行 = 一条库存变更流水：{@link #count} 为正表示入库、为负表示出库；
 * {@link #totalCount} 是这条流水之后该维度的库存量。
 *
 * @author 芋道源码
 */
@Data
public class StockRecordRespDTO {

    /**
     * 流水编号（erp_stock_record.id）
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
     * 出入库数量（正入负出）
     */
    private BigDecimal count;

    /**
     * 变动后的库存量
     */
    private BigDecimal totalCount;

    /**
     * 业务类型（枚举 {@code ErpStockRecordBizTypeEnum}）
     */
    private Integer bizType;

    /**
     * 业务类型名
     */
    private String bizTypeName;

    /**
     * 业务编号
     */
    private Long bizId;

    /**
     * 业务项编号
     */
    private Long bizItemId;

    /**
     * 业务单号
     */
    private String bizNo;

    /**
     * 流水时间
     */
    private LocalDateTime createTime;

}
