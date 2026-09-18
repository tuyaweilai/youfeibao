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
 * 危险废物市场价格基准 DO
 *
 * @author 芋道源码
 */
@TableName("waste_price_benchmark")
@KeySequence("waste_price_benchmark_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceBenchmarkDO extends BaseDO {

    /**
     * 价格基准ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

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
     * 基准价格
     */
    private BigDecimal price;

    /**
     * 价格单位 (如:元/桶,元/吨,元/千克)
     */
    private String priceUnit;

    // ========== 地区信息 ==========
    /**
     * 适用地区编码 (空为全国通用)
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

    // ========== 来源信息 ==========
    /**
     * 价格来源 (如:市场调研,政府指导价等)
     */
    private String priceSource;

    /**
     * 状态 (0:草稿, 1:生效中, 2:已失效)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.PriceBenchmarkStatusEnum}
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remark;

} 