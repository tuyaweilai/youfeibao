package cn.iocoder.yudao.module.logistics.enums;

import cn.iocoder.yudao.framework.common.core.IntArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 运输节点类型枚举。
 *
 * <p>节点是**事实**：司机在什么时候、什么位置、以什么凭证报告了运输过程中的一步。
 * 五类是固定枚举，不允许用自由文本替代——统计、门禁与「断点」判定都依赖它（见 #59 的 Implementation Decisions）。
 *
 * <p>轨迹不是节点：节点是事件，轨迹是路径。一期只有上报时的一次性位置快照，不承诺连续采集。
 *
 * <p>**照片必填策略**（V4 #71）写在枚举里：
 * 「交接完成」与「卸货完成」是货物流的关键凭证（税总 5 号公告第十七条要求保存运输凭证），
 * 必须有照片；其余三类允许没有。这是固定规则，不靠现场自觉。
 *
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportNodeTypeEnum implements IntArrayValuable {

    ARRIVED_PICKUP(1, "到达提货点", false, true),
    HANDOVER_CONFIRMED(2, "交接完成", true, true),
    DEPARTED(3, "起运", false, true),
    ARRIVED_STATION(4, "到达场站", false, false),
    UNLOADED(5, "卸货完成", true, false);

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(LogisticsTransportNodeTypeEnum::getType).toArray();

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;
    /**
     * 是否必须有照片（货物流关键凭证）
     */
    private final boolean photoRequired;
    /**
     * 是否**按停靠点**上报（V5 #72）
     *
     * <p>到提货点 / 交接完成 / 起运都发生在**某一个停靠点**上（一车提三家时，到达的是 B 家而不是 A 家）；
     * 到达场站 / 卸货完成是**整趟活**的收尾，不属于任何单个停靠点。
     */
    private final boolean stopScoped;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportNodeTypeEnum> ofType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

}
