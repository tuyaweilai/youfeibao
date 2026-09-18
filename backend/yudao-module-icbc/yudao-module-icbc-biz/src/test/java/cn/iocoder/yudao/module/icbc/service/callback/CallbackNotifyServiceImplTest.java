package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CallbackNotifyServiceImpl} 的单元测试类
 *
 * 覆盖 issue #3 的四条验收：九类通知同一入口、先落表后处理、可重放、重放不产生重复业务。
 */
@Import({CallbackNotifyServiceImpl.class, IcbcNotifyParser.class,
        CallbackNotifyServiceImplTest.TestPaymentHandler.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CallbackNotifyServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @Resource
    private TestPaymentHandler paymentHandler;

    @BeforeEach
    public void setUp() {
        paymentHandler.setCount(0);
        paymentHandler.setFail(false);
    }

    @Test
    public void testReceive_persistsThenProcesses() {
        // 调用
        String result = callbackNotifyService.receive(envelope("PAY_NOTIFY_1", "02", "ORDER_1"));

        // 断言：处理成功
        assertEquals("SUCCESS", result);
        assertEquals(1, paymentHandler.getCount());
        CallbackNotifyDO record = callbackNotifyMapper.selectByNotifyId("PAY_NOTIFY_1");
        assertNotNull(record);
        assertEquals(CallbackNotifyTypeEnum.PAYMENT.getType(), record.getNotifyType());
        assertEquals("ORDER_1", record.getBusinessId());
        assertEquals(CallbackProcessStatusEnum.SUCCESS.getStatus(), record.getProcessStatus());
        assertNotNull(record.getProcessTime());
    }

    @Test
    public void testReceive_duplicateDeliveryProcessesOnce() {
        // 同一报文投递两次
        String body = envelope("PAY_NOTIFY_DUP", "02", "ORDER_DUP");
        callbackNotifyService.receive(body);
        callbackNotifyService.receive(body);

        // 断言：只落一条、只处理一次
        assertEquals(1, paymentHandler.getCount());
        assertEquals(1L, callbackNotifyMapper.selectList().size());
    }

    @Test
    public void testReceive_allNineTypesPersisted() {
        // 银税協同的九类通知各投递一条（只有支付类有处理器，其余落表为待处理失败）
        List<CallbackNotifyTypeEnum> bankTypes = Arrays.stream(CallbackNotifyTypeEnum.values())
                .filter(type -> type.getType().matches("0[1-9]"))
                .collect(Collectors.toList());
        assertEquals(9, bankTypes.size(), "银税協同应有九类通知");
        int index = 0;
        for (CallbackNotifyTypeEnum type : bankTypes) {
            index++;
            callbackNotifyService.receive(envelope("NOTIFY_" + index, type.getType(), "ORDER_" + index));
        }

        // 断言：九条都落表，通知类型齐全
        List<CallbackNotifyDO> records = callbackNotifyMapper.selectList();
        assertEquals(9, records.size());
        assertEquals(9L, records.stream().map(CallbackNotifyDO::getNotifyType).distinct().count());
    }

    @Test
    public void testReceive_noHandlerPersistsAsFailure() {
        // 开票类通知暂无处理器
        callbackNotifyService.receive(envelope("INVOICE_NOTIFY_1", "03", "ORDER_INV_1"));

        CallbackNotifyDO record = callbackNotifyMapper.selectByNotifyId("INVOICE_NOTIFY_1");
        assertNotNull(record);
        assertEquals(CallbackProcessStatusEnum.FAILURE.getStatus(), record.getProcessStatus());
    }

    @Test
    public void testReplay_reprocessesFailedNotification() {
        // 第一次处理失败
        paymentHandler.setFail(true);
        callbackNotifyService.receive(envelope("PAY_NOTIFY_RETRY", "02", "ORDER_RETRY"));
        CallbackNotifyDO failed = callbackNotifyMapper.selectByNotifyId("PAY_NOTIFY_RETRY");
        assertEquals(CallbackProcessStatusEnum.FAILURE.getStatus(), failed.getProcessStatus());

        // 修复后重放
        paymentHandler.setFail(false);
        callbackNotifyService.replay(failed.getId());

        CallbackNotifyDO replayed = callbackNotifyMapper.selectById(failed.getId());
        assertEquals(CallbackProcessStatusEnum.SUCCESS.getStatus(), replayed.getProcessStatus());
        assertEquals(1, replayed.getRetryCount());
        assertEquals(2, paymentHandler.getCount());
    }

    @Test
    public void testReplay_successfulNotificationIsNoOp() {
        callbackNotifyService.receive(envelope("PAY_NOTIFY_NOOP", "02", "ORDER_NOOP"));
        CallbackNotifyDO record = callbackNotifyMapper.selectByNotifyId("PAY_NOTIFY_NOOP");

        // 重放已成功的通知：不再重复处理
        callbackNotifyService.replay(record.getId());

        assertEquals(1, paymentHandler.getCount());
    }

    /**
     * 构造工行外层报文：{ "notifyData": "<base64>", "signData": "..." }
     */
    private String envelope(String notifyId, String notifyType, String businessId) {
        String payload = "{\"notifyId\":\"" + notifyId + "\",\"notifyType\":\"" + notifyType
                + "\",\"outOrderId\":\"" + businessId + "\",\"invoiceStatus\":\"02\"}";
        String notifyData = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return "{\"notifyData\":\"" + notifyData + "\",\"signData\":\"SIGN\"}";
    }

    /**
     * 仅处理支付类通知的测试处理器
     */
    public static class TestPaymentHandler implements IcbcNotifyHandler {

        private int count = 0;
        private boolean fail = false;

        @Override
        public CallbackNotifyTypeEnum supportType() {
            return CallbackNotifyTypeEnum.PAYMENT;
        }

        @Override
        public void handle(IcbcNotifyContext context) {
            count++;
            if (fail) {
                throw new IllegalStateException("boom");
            }
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public void setFail(boolean fail) {
            this.fail = fail;
        }

    }

}
