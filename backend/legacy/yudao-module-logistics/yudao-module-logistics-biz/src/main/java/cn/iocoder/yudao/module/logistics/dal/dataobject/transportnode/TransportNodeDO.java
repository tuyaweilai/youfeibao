package cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流运输节点记录 DO
 *
 * @author 芋道源码
 */
@TableName("logistics_transport_node")
@KeySequence("logistics_transport_node_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportNodeDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 运输任务ID
     */
    private Long taskId;
    /**
     * 节点类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.TransportNodeTypeEnum}
     */
    private Integer nodeType;
    /**
     * 节点时间
     */
    private LocalDateTime nodeTime;
    /**
     * 节点位置
     */
    private String nodeLocation;
    /**
     * 纬度
     */
    private BigDecimal latitude;
    /**
     * 经度
     */
    private BigDecimal longitude;
    /**
     * 操作员ID
     */
    private Long operatorId;
    /**
     * 操作员姓名
     */
    private String operatorName;
    /**
     * 照片URLs(JSON数组)
     */
    private String photos;
    /**
     * 附加数据(JSON格式)
     */
    private String additionalData;
    /**
     * 备注
     */
    private String remark;
    /**
     * 租户ID
     */
    private Long tenantId;

} 