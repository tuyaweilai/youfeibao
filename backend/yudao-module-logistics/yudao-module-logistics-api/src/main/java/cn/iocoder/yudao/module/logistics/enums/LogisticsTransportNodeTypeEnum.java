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
 * @author 芋道源码
 */
@AllArgsConstructor
@Getter
public enum LogisticsTransportNodeTypeEnum implements IntArrayValuable {

    ARRIVED_PICKUP(1, "到达提货点"),
    HANDOVER_CONFIRMED(2, "交接完成"),
    DEPARTED(3, "起运"),
    ARRIVED_STATION(4, "到达场站"),
    UNLOADED(5, "卸货完成");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(LogisticsTransportNodeTypeEnum::getType).toArray();

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public int[] array() {
        return ARRAYS;
    }

    public static Optional<LogisticsTransportNodeTypeEnum> ofType(Integer type) {
        return Arrays.stream(values()).filter(item -> item.type.equals(type)).findFirst();
    }

}
