package cn.iocoder.yudao.module.icbc.service.log;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.log.ApiLogDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.log.ApiLogMapper;
import cn.iocoder.yudao.module.icbc.enums.ApiLogStatusEnum;
import cn.iocoder.yudao.module.icbc.service.log.impl.ApiLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ApiLogServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import(ApiLogServiceImpl.class)
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class ApiLogServiceImplTest extends BaseDbUnitTest {

    @Resource
    private ApiLogServiceImpl apiLogService;

    @Resource
    private ApiLogMapper apiLogMapper;

    @Test
    public void testLogApiStart() {
        // 准备参数
        String msgId = "MSG123456";
        String apiName = "收方新增接口";
        String apiUrl = "/jft/api/user/edpreceive/add/V1";
        String method = "POST";
        String requestParams = "{\"name\":\"张三\"}";
        String businessId = "ORDER123456";
        String businessType = "PAYEE_ADD";

        // 调用
        Long logId = apiLogService.logApiStart(msgId, apiName, apiUrl, method, requestParams, businessId, businessType);

        // 断言
        assertNotNull(logId);
        ApiLogDO apiLog = apiLogMapper.selectById(logId);
        assertNotNull(apiLog);
        assertEquals(msgId, apiLog.getMsgId());
        assertEquals(apiName, apiLog.getApiName());
        assertEquals(apiUrl, apiLog.getApiUrl());
        assertEquals(method, apiLog.getMethod());
        assertEquals(requestParams, apiLog.getRequestParams());
        assertEquals(businessId, apiLog.getBusinessId());
        assertEquals(businessType, apiLog.getBusinessType());
        assertEquals(ApiLogStatusEnum.SUCCESS.getStatus(), apiLog.getStatus());
    }

    @Test
    public void testLogApiSuccess() {
        // 准备参数
        ApiLogDO apiLog = ApiLogDO.builder()
                .msgId("MSG123456")
                .apiName("收方新增接口")
                .apiUrl("/jft/api/user/edpreceive/add/V1")
                .method("POST")
                .requestParams("{\"name\":\"张三\"}")
                .businessId("ORDER123456")
                .businessType("PAYEE_ADD")
                .status(ApiLogStatusEnum.SUCCESS.getStatus())
                .build();
        apiLogMapper.insert(apiLog);

        String responseData = "{\"return_code\":\"0000\",\"return_msg\":\"成功\"}";
        String returnCode = "0000";
        String returnMsg = "成功";
        Integer costTime = 1500;

        // 调用
        apiLogService.logApiSuccess(apiLog.getId(), responseData, returnCode, returnMsg, costTime);

        // 断言
        ApiLogDO updatedApiLog = apiLogMapper.selectById(apiLog.getId());
        assertNotNull(updatedApiLog);
        assertEquals(responseData, updatedApiLog.getResponseData());
        assertEquals(returnCode, updatedApiLog.getReturnCode());
        assertEquals(returnMsg, updatedApiLog.getReturnMsg());
        assertEquals(costTime, updatedApiLog.getCostTime());
        assertEquals(ApiLogStatusEnum.SUCCESS.getStatus(), updatedApiLog.getStatus());
    }

    @Test
    public void testLogApiFailure() {
        // 准备参数
        ApiLogDO apiLog = ApiLogDO.builder()
                .msgId("MSG123456")
                .apiName("收方新增接口")
                .apiUrl("/jft/api/user/edpreceive/add/V1")
                .method("POST")
                .requestParams("{\"name\":\"张三\"}")
                .businessId("ORDER123456")
                .businessType("PAYEE_ADD")
                .status(ApiLogStatusEnum.SUCCESS.getStatus())
                .build();
        apiLogMapper.insert(apiLog);

        String errorMsg = "网络超时";
        Integer costTime = 5000;

        // 调用
        apiLogService.logApiFailure(apiLog.getId(), errorMsg, costTime);

        // 断言
        ApiLogDO updatedApiLog = apiLogMapper.selectById(apiLog.getId());
        assertNotNull(updatedApiLog);
        assertEquals(errorMsg, updatedApiLog.getErrorMsg());
        assertEquals(costTime, updatedApiLog.getCostTime());
        assertEquals(ApiLogStatusEnum.FAILURE.getStatus(), updatedApiLog.getStatus());
    }

} 