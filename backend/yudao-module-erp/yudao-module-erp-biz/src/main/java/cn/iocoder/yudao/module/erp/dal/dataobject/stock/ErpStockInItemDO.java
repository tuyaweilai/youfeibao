package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 其它入库单项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_stock_in_item")
@KeySequence("erp_stock_in_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockInItemDO extends BaseDO {

    /**
     * 入库项编号
     */
    @TableId
    private Long id;
    /**
     * 入库编号
     *
     * 关联 {@link ErpStockInDO#getId()}
     */
    private Long inId;
    /**
     * 仓库编号
     *
     * 关联 {@link ErpWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 品类编号
     *
     * 关联 icbc_goods_config.id
     */
    private Long goodsConfigId;
    /**
     * 品类单价
     */
    private BigDecimal productPrice;
    /**
     * 品类数量
     */
    private BigDecimal count;
    /**
     * 合计金额，单位：元
     */
    private BigDecimal totalPrice;
    /**
     * 备注
     */
    private String remark;

}