package cn.iocoder.yudao.module.icbc.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link IcbcOccupationEnum} 的单元测试（#84）。
 *
 * <p>这是**工行的字典**：后台只能从这 15 个里选，手敲一个词会被工行驳回。所以条数与
 * 取值都必须钉死——少一个会让合法的职业选不了，多一个会让非法值被放行。
 */
public class IcbcOccupationEnumTest {

    @Test
    public void testHasExactlyFifteenValues() {
        assertEquals(15, IcbcOccupationEnum.values().length);
        assertEquals(15, IcbcOccupationEnum.ARRAYS.length);
    }

    @Test
    public void testCodeAndNameMapping() {
        assertEquals(IcbcOccupationEnum.CIVIL_SERVANT, IcbcOccupationEnum.valueOfCode("1"));
        assertEquals("公务员", IcbcOccupationEnum.nameOf("1"));
        assertEquals(IcbcOccupationEnum.OTHER, IcbcOccupationEnum.valueOfCode("14"));
        assertEquals("其他", IcbcOccupationEnum.nameOf("14"));
        assertEquals(IcbcOccupationEnum.RETIRED, IcbcOccupationEnum.valueOfCode("15"));
    }

    @Test
    public void testUnknownCodeIsRejected() {
        // 手敲的职业 / 越界值都不认
        assertNull(IcbcOccupationEnum.valueOfCode("程序员"));
        assertNull(IcbcOccupationEnum.valueOfCode("16"));
        assertNull(IcbcOccupationEnum.valueOfCode("0"));
        assertNull(IcbcOccupationEnum.valueOfCode(null));
        assertNull(IcbcOccupationEnum.nameOf("16"));
    }

}
