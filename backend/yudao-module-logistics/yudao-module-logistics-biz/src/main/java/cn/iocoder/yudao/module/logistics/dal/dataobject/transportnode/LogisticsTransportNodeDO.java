package cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 运输节点 DO（V2b #78）。
 *
 * <p>节点是**事实**：什么时候、什么位置、以什么凭证报告了运输过程中的一步。它同时是
 * **货物流**的凭证——照片、位置与两个时间都挂在节点上（税总 5 号公告第十七条要求保存
 * 「运输发票或凭证」）。
 *
 * <p>两个时间必须分开记：{@code nodeTime} 是事情**实际发生**的时刻，{@code reportTime} 是
 * **上报**的时刻。弱网下先存本地、恢复后补传时两者会差很远，晚到的事件不代表业务倒序。
 *
 * <p>幂等：{@code clientRequestId} 是客户端请求号，同一请求号重复提交只落一条。
 */
@TableName("logistics_transport_node")
@KeySequence("logistics_transport_node_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogisticsTransportNodeDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 运输任务编号 */
    private Long taskId;
    /** 运输任务单号（冗余，便于按单号查询与展示） */
    private String taskNo;

    /**
     * 停靠点编号（V5 #72）
     *
     * <p>到提货点 / 交接完成 / 起运都发生在**某一个停靠点**上；到达场站 / 卸货完成是整趟活的收尾，为空。
     * 集货（一车提三家）时靠它把节点归到各家，进度互不相串。
     */
    private Long stopId;

    /**
     * 节点类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum}
     */
    private Integer nodeType;

    /** 发生时间：事情实际发生的时刻 */
    private LocalDateTime nodeTime;
    /** 上报时间：客户端把这条事实提交上来的时刻 */
    private LocalDateTime reportTime;

    /** 位置描述 */
    private String location;
    /** 纬度 */
    private BigDecimal latitude;
    /** 经度 */
    private BigDecimal longitude;
    /** 凭证照片 URL 列表（JSON 数组文本） */
    private String photos;

    /** 上报人编号（司机或代录的调度） */
    private Long operatorId;
    /** 上报人姓名 */
    private String operatorName;

    /**
     * 异常类型（空 = 正常节点）
     *
     * <p>异常是**独立标记**，不是节点类型也不是任务状态（#59 的 Implementation Decisions 第 17 条）：
     * 它只在这里留痕，不推进也不回退任务状态机。
     *
     * 枚举 {@link cn.iocoder.yudao.module.logistics.enums.LogisticsTransportAbnormalTypeEnum}
     */
    private Integer abnormalType;
    /** 异常说明（上报异常必填） */
    private String abnormalReason;
    /** 异常是否已解决（解决留痕：时间 / 人 / 说明） */
    private Boolean abnormalResolved;
    /** 异常解决时间 */
    private LocalDateTime abnormalResolvedAt;
    /** 异常解决人（系统用户编号） */
    private Long abnormalResolvedBy;
    /** 异常解决人姓名快照 */
    private String abnormalResolvedName;
    /** 异常解决说明 */
    private String abnormalResolvedRemark;

    /**
     * 客户端请求号：同一请求号重复提交只落一条（离线补传的幂等键）
     */
    private String clientRequestId;

    /** 备注 */
    private String remark;

}
