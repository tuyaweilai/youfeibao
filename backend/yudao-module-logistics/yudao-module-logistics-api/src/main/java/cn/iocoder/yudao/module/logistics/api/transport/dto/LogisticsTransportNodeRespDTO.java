package cn.iocoder.yudao.module.logistics.api.transport.dto;

import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运输节点响应 DTO：一次上报的运输事实，以及它作为**货物流**凭证所需的全部信息。
 *
 * <p>节点既是「过程」，也是「凭证」——照片、位置与两个时间都挂在节点上，所以按任务编号取节点、
 * 按交接批次编号取凭证，用的是同一个形状。
 *
 * <p>车牌与司机是**快照**：档案改名或删档都不影响历史单据的凭证。
 *
 * @author 芋道源码
 */
@Data
public class LogisticsTransportNodeRespDTO {

    /**
     * 运输节点编号
     */
    private Long id;
    /**
     * 运输任务编号
     */
    private Long transportTaskId;
    /**
     * 运输任务编号（对外可见的单号）
     */
    private String taskNo;
    /**
     * 节点类型
     *
     * 枚举 {@link LogisticsTransportNodeTypeEnum}
     */
    private Integer nodeType;
    /**
     * 发生时间：事情实际发生的时刻（补录时可能早于上报时间）
     */
    private LocalDateTime nodeTime;
    /**
     * 上报时间：客户端把这条事实提交上来的时刻
     */
    private LocalDateTime reportTime;
    /**
     * 位置描述
     */
    private String location;
    /**
     * 纬度
     */
    private BigDecimal latitude;
    /**
     * 经度
     */
    private BigDecimal longitude;
    /**
     * 凭证照片 URL 列表
     */
    private List<String> photos;
    /**
     * 上报人姓名
     */
    private String operatorName;

    /**
     * 车牌号（快照）
     */
    private String plateNo;
    /**
     * 司机姓名（快照）
     */
    private String driverName;
    /**
     * 交接批次编号（icbc 侧编号）
     *
     * <p>物流只存编号、不存活引用：本字段是「这批货的运输凭证」与收购事实之间唯一的挂接点，
     * 也是 icbc 追溯页按交接批次取凭证的依据。
     */
    private Long handoverBatchId;

}
