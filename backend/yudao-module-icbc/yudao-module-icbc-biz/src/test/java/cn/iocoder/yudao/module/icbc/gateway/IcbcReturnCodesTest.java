package cn.iocoder.yudao.module.icbc.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link IcbcReturnCodes} 的单元测试类
 *
 * 关键是「代理异常 / 超时 / 未知返回码」被归为 UNKNOWN，而不是失败——
 * 调用方据此走「先查询、不重复提交」。
 */
public class IcbcReturnCodesTest {

    @Test
    public void testClassify_success() {
        assertEquals(IcbcOutcome.SUCCESS, IcbcReturnCodes.classify(0, 0));
        assertEquals(IcbcOutcome.SUCCESS, IcbcReturnCodes.classify(10100000, 10100000));
    }

    @Test
    public void testClassify_businessFailed() {
        assertEquals(IcbcOutcome.BUSINESS_FAILED, IcbcReturnCodes.classify(30601006, 0));
        assertEquals(IcbcOutcome.BUSINESS_FAILED, IcbcReturnCodes.classify(10101907, 10100000));
        assertEquals(IcbcOutcome.BUSINESS_FAILED, IcbcReturnCodes.classify(22080002, 0));
    }

    @Test
    public void testClassify_unknown() {
        assertEquals(IcbcOutcome.UNKNOWN, IcbcReturnCodes.classify(IcbcReturnCodes.PROXY_EXCEPTION, 0));
        assertEquals(IcbcOutcome.UNKNOWN, IcbcReturnCodes.classify(IcbcReturnCodes.PROXY_TIMEOUT, 10100000));
        assertEquals(IcbcOutcome.UNKNOWN, IcbcReturnCodes.classify(IcbcReturnCodes.GATEWAY_INTERNAL_ERROR, 0));
        assertEquals(IcbcOutcome.UNKNOWN, IcbcReturnCodes.classify(IcbcReturnCodes.SYSTEM_ERROR, 0));
        assertEquals(IcbcOutcome.UNKNOWN, IcbcReturnCodes.classify(-1, 0));
    }

    @Test
    public void testIsUnknown() {
        assertEquals(true, IcbcReturnCodes.isUnknown(-500041));
        assertEquals(true, IcbcReturnCodes.isUnknown(-500042));
        assertEquals(true, IcbcReturnCodes.isUnknown(99999999));
        assertEquals(false, IcbcReturnCodes.isUnknown(0));
        assertEquals(false, IcbcReturnCodes.isUnknown(30601006));
    }

}
