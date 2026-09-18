package cn.iocoder.yudao.module.icbc.job;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.icbc.service.warning.IcbcExpiryWarningService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * {@link QualificationExpiryReminderJob} 的单元测试类。
 */
public class QualificationExpiryReminderJobTest extends BaseMockitoUnitTest {

    @InjectMocks
    private QualificationExpiryReminderJob job;

    @Mock
    private IcbcExpiryWarningService expiryWarningService;

    @Test
    public void testExecute_defaultDays() {
        when(expiryWarningService.scan(30)).thenReturn(2);

        String result = job.execute(null);

        verify(expiryWarningService).scan(30);
        assertEquals("扫描完成，新增 2 条到期预警", result);
    }

    @Test
    public void testExecute_customDays() {
        when(expiryWarningService.scan(15)).thenReturn(0);

        String result = job.execute("15");

        verify(expiryWarningService).scan(15);
        assertEquals("扫描完成，新增 0 条到期预警", result);
    }

    @Test
    public void testExecute_invalidParamFallsBackToDefault() {
        when(expiryWarningService.scan(30)).thenReturn(1);

        job.execute("abc");

        verify(expiryWarningService).scan(30);
    }

}
