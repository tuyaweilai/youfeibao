package cn.iocoder.yudao.module.logistics.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportNodeTypeEnum} 的单元测试。
 *
 * <p>运输节点类型是**固定枚举**（见 #59 的 Implementation Decisions）：它同时是过程记录、
 * 货物流凭证与「断点」判定的依据。一旦有人往里加类型或改码值，统计口径与门禁都会跟着变，
 * 所以这里把五类与码值锁死。
 */
public class LogisticsTransportNodeTypeEnumTest {

    @Test
    public void testTypeMapping() {
        assertEquals(LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP,
                LogisticsTransportNodeTypeEnum.ofType(1).orElseThrow());
        assertEquals(LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED,
                LogisticsTransportNodeTypeEnum.ofType(2).orElseThrow());
        assertEquals(LogisticsTransportNodeTypeEnum.DEPARTED,
                LogisticsTransportNodeTypeEnum.ofType(3).orElseThrow());
        assertEquals(LogisticsTransportNodeTypeEnum.ARRIVED_STATION,
                LogisticsTransportNodeTypeEnum.ofType(4).orElseThrow());
        assertEquals(LogisticsTransportNodeTypeEnum.UNLOADED,
                LogisticsTransportNodeTypeEnum.ofType(5).orElseThrow());
        assertFalse(LogisticsTransportNodeTypeEnum.ofType(99).isPresent());
    }

    @Test
    public void testExactlyFiveNodeTypesInOrder() {
        List<LogisticsTransportNodeTypeEnum> types = Arrays.asList(LogisticsTransportNodeTypeEnum.values());

        assertEquals(5, types.size(), "运输节点类型是固定的五类，加类型要同步改统计、门禁与前端");
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Arrays.asList(
                LogisticsTransportNodeTypeEnum.ARRIVED_PICKUP.getType(),
                LogisticsTransportNodeTypeEnum.HANDOVER_CONFIRMED.getType(),
                LogisticsTransportNodeTypeEnum.DEPARTED.getType(),
                LogisticsTransportNodeTypeEnum.ARRIVED_STATION.getType(),
                LogisticsTransportNodeTypeEnum.UNLOADED.getType()));
    }

    @Test
    public void testArraysForValidation() {
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, LogisticsTransportNodeTypeEnum.ARRAYS);
    }

    @Test
    public void testEveryTypeHasName() {
        for (LogisticsTransportNodeTypeEnum type : LogisticsTransportNodeTypeEnum.values()) {
            assertNotNull(type.getType());
            assertNotNull(type.getName());
            assertFalse(type.getName().isEmpty(), "节点类型必须有可读名称，页面与凭证上要显示");
        }
    }

}
