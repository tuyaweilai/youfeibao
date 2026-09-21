package cn.iocoder.yudao.module.icbc.service.callback;

import org.springframework.boot.test.mock.mockito.MockBean;
import cn.iocoder.yudao.module.erp.api.stock.StockApi;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifySummaryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.callback.CallbackNotifyMapper;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.service.callback.impl.CallbackNotifyServiceImpl;
import cn.iocoder.yudao.module.icbc.service.esign.FrameworkAgreementEsignService;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 平台运营通知监控的测试（#15）。
 *
 * <p>通知是工行推来的全局事件：平台运营要能跨租户看到九类通知的处理结果与时间；
 * 租户管理员在自己的租户里看不到别人的通知。
 */
@Import({UnitTestConfiguration.class, IcbcTenantTestConfiguration.class, CallbackNotifyServiceImpl.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class PlatformCallbackNotifyServiceImplTest extends BaseDbUnitTest {

    /** 库存域只通过 erp-api 的 StockApi 接入（#52）；单元测试不跨模块，用 Mock。 */
    @MockBean
    private StockApi stockApi;

    /** 采购链会经收方建档 service 注入电子签（#95）；本测试不碰出站，置空。 */
    @MockBean
    private FrameworkAgreementEsignService frameworkAgreementEsignService;

    @Resource
    private CallbackNotifyServiceImpl callbackNotifyService;

    @Resource
    private CallbackNotifyMapper callbackNotifyMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testPlatformPageSpansTenants() {
        insertNotify(1L, "NOTIFY_T1", "02", "ORDER_T1", CallbackProcessStatusEnum.SUCCESS);
        insertNotify(2L, "NOTIFY_T2", "03", "ORDER_T2", CallbackProcessStatusEnum.FAILURE);

        // 平台运营即使在租户 1 的上下文里，也应看到两个租户的通知
        PageResult<CallbackNotifyDO> page = TenantUtils.execute(1L,
                () -> callbackNotifyService.getPlatformCallbackNotifyPage(new CallbackNotifyPageReqVO()));
        assertEquals(2, page.getTotal());

        // 租户 1 的普通查询只看得到自己的
        PageResult<CallbackNotifyDO> tenantPage = TenantUtils.execute(1L,
                () -> callbackNotifyService.getCallbackNotifyPage(new CallbackNotifyPageReqVO()));
        assertEquals(1, tenantPage.getTotal());
        assertEquals("NOTIFY_T1", tenantPage.getList().get(0).getNotifyId());
    }

    @Test
    public void testPlatformSummaryCoversNineTypes() {
        String[] types = {"01", "02", "03", "04", "05", "06", "07", "08", "09"};
        for (int i = 0; i < types.length; i++) {
            CallbackProcessStatusEnum status = i == 1
                    ? CallbackProcessStatusEnum.FAILURE : CallbackProcessStatusEnum.SUCCESS;
            insertNotify(1L, "NOTIFY_TYPE_" + types[i], types[i], "ORDER_" + i, status);
        }

        CallbackNotifySummaryRespVO summary = callbackNotifyService.getPlatformCallbackNotifySummary();

        assertEquals(9L, summary.getTotal());
        assertEquals(8L, summary.getSuccessCount());
        assertEquals(1L, summary.getFailureCount());
        assertEquals(0L, summary.getPendingCount());
        // 九类都要在概览里出现，即使某一类没有失败也要显示
        assertEquals(9, summary.getTypes().size());
        CallbackNotifySummaryRespVO.TypeStat paymentStat = summary.getTypes().stream()
                .filter(stat -> "02".equals(stat.getNotifyType())).findFirst().orElseThrow();
        assertEquals("b2b 支付", paymentStat.getNotifyTypeName());
        assertEquals("付款", paymentStat.getBusinessName());
        assertEquals(1L, paymentStat.getFailureCount());
    }

    @Test
    public void testListByBusinessIdSpansTenants() {
        insertNotify(1L, "NOTIFY_BIZ_1", "03", "ORDER_SHARED", CallbackProcessStatusEnum.SUCCESS);
        insertNotify(2L, "NOTIFY_BIZ_2", "04", "ORDER_SHARED", CallbackProcessStatusEnum.SUCCESS);
        insertNotify(1L, "NOTIFY_BIZ_OTHER", "03", "ORDER_OTHER", CallbackProcessStatusEnum.SUCCESS);

        List<CallbackNotifyDO> list = callbackNotifyService.getCallbackNotifyListByBusinessId("ORDER_SHARED");

        assertEquals(2, list.size());
        assertTrue(list.stream().allMatch(item -> "ORDER_SHARED".equals(item.getBusinessId())));
    }

    @Test
    public void testBusinessNameMapping() {
        assertEquals("开票", CallbackNotifyTypeEnum.businessNameOf("03"));
        assertEquals("红冲", CallbackNotifyTypeEnum.businessNameOf("08"));
        assertNull(CallbackNotifyTypeEnum.businessNameOf("99"));
    }

    private void insertNotify(Long tenantId, String notifyId, String notifyType, String businessId,
                              CallbackProcessStatusEnum status) {
        TenantUtils.execute(tenantId, () -> callbackNotifyMapper.insert(CallbackNotifyDO.builder()
                .notifyId(notifyId)
                .notifyType(notifyType)
                .businessId(businessId)
                .notifyData("{}")
                .processStatus(status.getStatus())
                .retryCount(0)
                .build()));
    }

}
