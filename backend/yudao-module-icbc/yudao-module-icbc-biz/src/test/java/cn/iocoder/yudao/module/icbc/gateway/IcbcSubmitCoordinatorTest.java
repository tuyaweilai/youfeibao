package cn.iocoder.yudao.module.icbc.gateway;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IcbcSubmitCoordinator} 的单元测试类
 *
 * 固化 issue #3 的验收项：遇到代理异常 / 超时 / 未知返回码时不重复提交，
 * 先调用查询接口确认指令状态。
 */
public class IcbcSubmitCoordinatorTest {

    @Test
    public void testExecute_submitSuccess_doesNotQuery() {
        StubCommand command = new StubCommand();
        command.submitResult = IcbcGatewayResult.success("ok", 0, "成功");

        IcbcGatewayResult<String> result = IcbcSubmitCoordinator.execute(command);

        assertTrue(result.isSuccess());
        assertEquals(1, command.submitCount.get());
        assertEquals(0, command.queryCount.get());
        assertEquals(0, command.confirmCount.get());
    }

    @Test
    public void testExecute_submitBusinessFailed_doesNotQuery() {
        StubCommand command = new StubCommand();
        command.submitResult = IcbcGatewayResult.businessFailed(10101907, "发票未查询到");

        IcbcGatewayResult<String> result = IcbcSubmitCoordinator.execute(command);

        assertEquals(IcbcOutcome.BUSINESS_FAILED, result.getOutcome());
        assertEquals(1, command.submitCount.get());
        assertEquals(0, command.queryCount.get());
    }

    @Test
    public void testExecute_submitUnknown_queriesAndConfirms() {
        StubCommand command = new StubCommand();
        command.submitResult = IcbcGatewayResult.unknown(-500042, "代理超时");
        command.queryResult = IcbcGatewayResult.success("QUERIED", 0, "成功");

        IcbcGatewayResult<String> result = IcbcSubmitCoordinator.execute(command);

        // 关键：提交只发生一次，未知后走查询
        assertEquals(1, command.submitCount.get());
        assertEquals(1, command.queryCount.get());
        assertEquals(1, command.confirmCount.get());
        assertTrue(result.isSuccess());
        assertEquals("CONFIRMED:QUERIED", result.getData());
    }

    @Test
    public void testExecute_submitUnknown_queryAlsoUnknown_keepsUnknown() {
        StubCommand command = new StubCommand();
        command.submitResult = IcbcGatewayResult.unknown(-500041, "代理异常");
        command.queryResult = IcbcGatewayResult.unknown(-500042, "代理超时");

        IcbcGatewayResult<String> result = IcbcSubmitCoordinator.execute(command);

        assertEquals(IcbcOutcome.UNKNOWN, result.getOutcome());
        assertEquals(1, command.submitCount.get());
        assertEquals(1, command.queryCount.get());
        assertEquals(0, command.confirmCount.get());
    }

    @Test
    public void testExecute_submitUnknown_queryBusinessFailed_keepsUnknown() {
        StubCommand command = new StubCommand();
        command.submitResult = IcbcGatewayResult.unknown(-500041, "代理异常");
        command.queryResult = IcbcGatewayResult.businessFailed(10101910, "未查询到订单");

        IcbcGatewayResult<String> result = IcbcSubmitCoordinator.execute(command);

        assertEquals(IcbcOutcome.UNKNOWN, result.getOutcome());
        assertEquals(1, command.submitCount.get());
        assertEquals(1, command.queryCount.get());
    }

    /**
     * 简易命令桩，记录提交 / 查询 / 确认次数
     */
    private static class StubCommand implements IcbcSubmitCommand<String, String> {

        private final AtomicInteger submitCount = new AtomicInteger();
        private final AtomicInteger queryCount = new AtomicInteger();
        private final AtomicInteger confirmCount = new AtomicInteger();
        private IcbcGatewayResult<String> submitResult;
        private IcbcGatewayResult<String> queryResult;

        @Override
        public IcbcGatewayResult<String> submit() {
            submitCount.incrementAndGet();
            return submitResult;
        }

        @Override
        public IcbcGatewayResult<String> query() {
            queryCount.incrementAndGet();
            return queryResult;
        }

        @Override
        public IcbcGatewayResult<String> confirm(String queryData) {
            confirmCount.incrementAndGet();
            return IcbcGatewayResult.success("CONFIRMED:" + queryData, 0, "成功");
        }

    }

}
