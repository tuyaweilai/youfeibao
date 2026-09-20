package cn.iocoder.yudao.module.logistics.enums;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link LogisticsTransportAbnormalTypeEnum} 的单元测试（V4 #71）。
 *
 * <p>异常类型是**固定枚举**，不允许自由文本替代（#59 的 Implementation Decisions 第 17 条）。
 * 加类型 / 改码值会动到统计口径，所以这里把八类与码值锁死。
 */
public class LogisticsTransportAbnormalTypeEnumTest {

    @Test
    public void testTypeMapping() {
        assertEquals(LogisticsTransportAbnormalTypeEnum.VEHICLE_BREAKDOWN,
                LogisticsTransportAbnormalTypeEnum.ofType(1).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.TRAFFIC_ACCIDENT,
                LogisticsTransportAbnormalTypeEnum.ofType(2).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.WEATHER_DELAY,
                LogisticsTransportAbnormalTypeEnum.ofType(3).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.ROAD_CLOSED,
                LogisticsTransportAbnormalTypeEnum.ofType(4).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.CARGO_DAMAGED,
                LogisticsTransportAbnormalTypeEnum.ofType(5).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.COUNTERPARTY_ABSENT,
                LogisticsTransportAbnormalTypeEnum.ofType(6).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.WRONG_ADDRESS,
                LogisticsTransportAbnormalTypeEnum.ofType(7).orElseThrow());
        assertEquals(LogisticsTransportAbnormalTypeEnum.OTHER,
                LogisticsTransportAbnormalTypeEnum.ofType(8).orElseThrow());
        assertFalse(LogisticsTransportAbnormalTypeEnum.ofType(99).isPresent());
    }

    @Test
    public void testExactlyEightTypesInOrder() {
        List<LogisticsTransportAbnormalTypeEnum> types =
                Arrays.asList(LogisticsTransportAbnormalTypeEnum.values());

        assertEquals(8, types.size(), "异常类型是固定的八类，加类型要同步改统计与前端");
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), Arrays.asList(
                LogisticsTransportAbnormalTypeEnum.VEHICLE_BREAKDOWN.getType(),
                LogisticsTransportAbnormalTypeEnum.TRAFFIC_ACCIDENT.getType(),
                LogisticsTransportAbnormalTypeEnum.WEATHER_DELAY.getType(),
                LogisticsTransportAbnormalTypeEnum.ROAD_CLOSED.getType(),
                LogisticsTransportAbnormalTypeEnum.CARGO_DAMAGED.getType(),
                LogisticsTransportAbnormalTypeEnum.COUNTERPARTY_ABSENT.getType(),
                LogisticsTransportAbnormalTypeEnum.WRONG_ADDRESS.getType(),
                LogisticsTransportAbnormalTypeEnum.OTHER.getType()));
    }

    @Test
    public void testEveryTypeHasName() {
        for (LogisticsTransportAbnormalTypeEnum type : LogisticsTransportAbnormalTypeEnum.values()) {
            assertNotNull(type.getType());
            assertNotNull(type.getName());
            assertFalse(type.getName().isEmpty(), "异常类型必须有可读名称，页面与凭证上要显示");
        }
        assertNull(LogisticsTransportAbnormalTypeEnum.nameOf(null));
    }

}
