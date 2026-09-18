package cn.iocoder.yudao.module.icbc.service.callback;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CallbackNotifyServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(CallbackNotifyServiceImpl.class)
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CallbackNotifyServiceImplTest extends BaseDbUnitTest {

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @Test
    public void testProcessCallback_Success() {
        // 准备参数
        String notifyId = "NOTIFY123456";
        String notifyType = CallbackNotifyTypeEnum.PAYEE_AUDIT.getType();
        String businessId = "ORDER123456";
        String notifyData = "{\"status\":\"APPROVED\"}";
        String sign = "ABC123";

        // 调用
        String result = callbackNotifyService.processCallback(notifyId, notifyType, businessId, notifyData, sign);

        // 断言
        assertEquals("SUCCESS", result);
        
        CallbackNotifyDO callbackNotify = callbackNotifyMapper.selectByNotifyId(notifyId);
        assertNotNull(callbackNotify);
        assertEquals(notifyId, callbackNotify.getNotifyId());
        assertEquals(notifyType, callbackNotify.getNotifyType());
        assertEquals(businessId, callbackNotify.getBusinessId());
        assertEquals(notifyData, callbackNotify.getNotifyData());
        assertEquals(sign, callbackNotify.getSign());
        assertEquals(CallbackProcessStatusEnum.SUCCESS.getStatus(), callbackNotify.getProcessStatus());
        assertEquals("处理成功", callbackNotify.getProcessMsg());
        assertNotNull(callbackNotify.getProcessTime());
    }

    @Test
    public void testProcessCallback_AlreadyProcessed() {
        // 准备数据 - 已处理的回调通知
        String notifyId = "NOTIFY123456";
        CallbackNotifyDO existingNotify = CallbackNotifyDO.builder()
                .notifyId(notifyId)
                .notifyType(CallbackNotifyTypeEnum.PAYEE_AUDIT.getType())
                .businessId("ORDER123456")
                .notifyData("{\"status\":\"APPROVED\"}")
                .sign("ABC123")
                .processStatus(CallbackProcessStatusEnum.SUCCESS.getStatus())
                .processMsg("处理成功")
                .retryCount(0)
                .build();
        callbackNotifyMapper.insert(existingNotify);

        // 调用
        String result = callbackNotifyService.processCallback(notifyId, 
                CallbackNotifyTypeEnum.PAYEE_AUDIT.getType(), 
                "ORDER123456", 
                "{\"status\":\"APPROVED\"}", 
                "ABC123");

        // 断言
        assertEquals("SUCCESS", result);
    }

    @Test
    public void testRetryCallback() {
        // 准备数据
        CallbackNotifyDO callbackNotify = CallbackNotifyDO.builder()
                .notifyId("NOTIFY123456")
                .notifyType(CallbackNotifyTypeEnum.INVOICE_STATUS.getType())
                .businessId("ORDER123456")
                .notifyData("{\"status\":\"COMPLETED\"}")
                .sign("ABC123")
                .processStatus(CallbackProcessStatusEnum.FAILURE.getStatus())
                .processMsg("处理失败")
                .retryCount(0)
                .build();
        callbackNotifyMapper.insert(callbackNotify);

        // 调用
        callbackNotifyService.retryCallback(callbackNotify.getId());

        // 断言
        CallbackNotifyDO updatedNotify = callbackNotifyMapper.selectById(callbackNotify.getId());
        assertNotNull(updatedNotify);
        assertEquals(1, updatedNotify.getRetryCount());
    }

} 