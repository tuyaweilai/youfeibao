package cn.iocoder.yudao.module.icbc.dal.dataobject.invoice;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 工行订单商品明细 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_order_item")
@KeySequence("icbc_order_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDO extends TenantBaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单号
     */
    private String orderNo;
    
    /**
     * 商品名称
     */
    private String itemName;
    
    /**
     * 商品编码
     */
    private String itemCode;
    
    /**
     * 规格型号
     */
    private String specification;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 数量
     */
    private BigDecimal quantity;
    
    /**
     * 单价（元）
     */
    private BigDecimal unitPrice;
    
    /**
     * 金额（元）
     */
    private BigDecimal amount;
    
    /**
     * 税率
     */
    private BigDecimal taxRate;
    
    /**
     * 税额（元）
     */
    private BigDecimal taxAmount;
    
    /**
     * 数量口径说明：结算重量计价后，发票「数量」与磅单「净重」不再相等（差一个扣杂，ADR 0019）。
     */
    private String quantityNote;

    /**
     * 商品分类
     */
    private String category;

} 