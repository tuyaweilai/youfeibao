package cn.iocoder.yudao.module.waste.dal.dataobject.recycler;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回收企业客户专属价格配置 DO
 *
 * @author 芋道源码
 */
@TableName("waste_recycler_customer_price")
@KeySequence("waste_recycler_customer_price_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecyclerCustomerPriceDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 回收企业ID
     */
    private Long recyclingEnterpriseId;
    
    /**
     * 客户企业ID
     */
    private Long customerEnterpriseId;
    
    /**
     * 废物代码
     */
    private String wasteCode;
    
    /**
     * 废物名称
     */
    private String wasteName;
    
    /**
     * 价格类型：1-固定价格，2-浮动价格，3-阶梯价格
     */
    private Integer priceType;
    
    /**
     * 专属价格
     */
    private BigDecimal specialPrice;
    
    /**
     * 价格单位
     */
    private String priceUnit;
    
    /**
     * 最小数量
     */
    private BigDecimal minQuantity;
    
    /**
     * 最大数量
     */
    private BigDecimal maxQuantity;
    
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    
    /**
     * 失效日期
     */
    private LocalDate expireDate;
    
    /**
     * 关联合同ID
     */
    private Long contractId;
    
    /**
     * 价格优势描述
     */
    private String priceAdvantageDesc;
    
    /**
     * 状态：0-草稿，1-生效中，2-已失效，3-已取消
     */
    private Integer status;
    
    /**
     * 备注
     */
    private String remark;

} 