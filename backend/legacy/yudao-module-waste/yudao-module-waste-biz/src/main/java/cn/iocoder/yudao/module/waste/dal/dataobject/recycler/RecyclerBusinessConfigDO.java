package cn.iocoder.yudao.module.waste.dal.dataobject.recycler;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 回收企业业务模式配置 DO
 *
 * @author 芋道源码
 */
@TableName("waste_recycler_business_config")
@KeySequence("waste_recycler_business_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecyclerBusinessConfigDO extends BaseDO {

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
     * 业务模式：1-竞价模式，2-议价模式，3-固定价格模式
     */
    private Integer businessMode;
    
    /**
     * 默认报价模式：1-自动报价，2-手动报价
     */
    private Integer defaultQuotationMode;
    
    /**
     * 是否允许客户选择模式
     */
    private Boolean allowClientModeSelection;
    
    /**
     * 报价超时时间（小时）
     */
    private Integer quotationTimeoutHours;
    
    /**
     * 是否自动接受单一报价
     */
    private Boolean autoAcceptSingleQuotation;
    
    /**
     * 是否启用价格协商
     */
    private Boolean enablePriceNegotiation;
    
    /**
     * 是否启用
     */
    private Boolean isEnabled;
    
    /**
     * 备注
     */
    private String remark;

} 