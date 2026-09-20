package cn.iocoder.yudao.module.waste.dal.dataobject.price;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 回收企业价格配置 DO
 *
 * @author 芋道源码
 */
@TableName("waste_recycler_price_config")
@KeySequence("waste_recycler_price_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecyclerPriceConfigDO extends BaseDO {

    /**
     * 价格配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 回收企业ID
     */
    private Long recyclingEnterpriseId;

    // ========== 废物信息 ==========
    /**
     * 危险废物代码
     */
    private String wasteCode;

    /**
     * 危险废物名称
     */
    private String wasteName;

    // ========== 价格信息 ==========
    /**
     * 收购价格
     */
    private BigDecimal purchasePrice;

    /**
     * 价格单位 (如:元/桶,元/吨,元/千克)
     */
    private String priceUnit;

    // ========== 数量限制 ==========
    /**
     * 最小收购数量
     */
    private BigDecimal minQuantity;

    /**
     * 最大收购数量
     */
    private BigDecimal maxQuantity;

    // ========== 地区限制 ==========
    /**
     * 适用地区编码 (空为不限地区)
     */
    private String regionCode;

    /**
     * 适用地区名称
     */
    private String regionName;

    // ========== 有效期信息 ==========
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;

    /**
     * 失效日期
     */
    private LocalDate expireDate;

    // ========== 价格策略 ==========
    /**
     * 是否可议价
     */
    private Boolean isNegotiable;

    /**
     * 配置状态 (0:草稿, 1:生效中, 2:已失效)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.PriceConfigStatusEnum}
     */
    private Integer status;

    /**
     * 配置备注
     */
    private String remark;

} 