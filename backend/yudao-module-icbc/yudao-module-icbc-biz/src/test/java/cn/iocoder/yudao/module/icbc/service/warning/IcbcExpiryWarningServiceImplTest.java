package cn.iocoder.yudao.module.icbc.service.warning;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.warning.IcbcExpiryWarningDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.warning.IcbcExpiryWarningMapper;
import cn.iocoder.yudao.module.icbc.service.qualification.IcbcQualificationService;
import cn.iocoder.yudao.module.icbc.service.warning.impl.IcbcExpiryWarningServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.EXPIRY_WARNING_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * {@link IcbcExpiryWarningServiceImpl} 的单元测试类。
 */
@Import({IcbcExpiryWarningServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class IcbcExpiryWarningServiceImplTest extends BaseDbUnitTest {

    @Resource
    private IcbcExpiryWarningServiceImpl expiryWarningService;

    @Resource
    private IcbcExpiryWarningMapper expiryWarningMapper;

    @MockBean
    private IcbcQualificationService qualificationService;

    @BeforeEach
    public void setUp() {
        when(qualificationService.getExpiringList(30)).thenReturn(Collections.emptyList());
    }

    @Test
    public void testScan_createsWarningForExpiringQualification() {
        when(qualificationService.getExpiringList(30)).thenReturn(
                Collections.singletonList(buildQualification(100L, "TAX", "反向开票资格")));

        int created = expiryWarningService.scan(30);

        assertEquals(1, created);
        List<IcbcExpiryWarningDO> open = expiryWarningService.getOpenList();
        assertEquals(1, open.size());
        assertEquals(100L, open.get(0).getQualificationId());
        assertEquals("TAX", open.get(0).getType());
    }

    @Test
    public void testScan_isIdempotent() {
        when(qualificationService.getExpiringList(30)).thenReturn(
                Collections.singletonList(buildQualification(100L, "TAX", "反向开票资格")));

        assertEquals(1, expiryWarningService.scan(30));
        assertEquals(0, expiryWarningService.scan(30), "已有未处理预警时不应重复生成");
        assertEquals(1, expiryWarningService.getOpenList().size());
    }

    @Test
    public void testAcknowledge_closesWarning() {
        when(qualificationService.getExpiringList(30)).thenReturn(
                Collections.singletonList(buildQualification(100L, "TAX", "反向开票资格")));
        expiryWarningService.scan(30);
        Long warningId = expiryWarningService.getOpenList().get(0).getId();

        expiryWarningService.acknowledge(warningId);

        assertTrue(expiryWarningService.getOpenList().isEmpty());
        assertEquals(1, expiryWarningMapper.selectById(warningId).getStatus());
    }

    @Test
    public void testAcknowledge_notExists() {
        assertServiceException(() -> expiryWarningService.acknowledge(999L), EXPIRY_WARNING_NOT_EXISTS);
    }

    private IcbcQualificationDO buildQualification(Long id, String type, String name) {
        IcbcQualificationDO qualification = new IcbcQualificationDO();
        qualification.setId(id);
        qualification.setType(type);
        qualification.setName(name);
        qualification.setStatus(1);
        qualification.setValidTo(LocalDate.now().plusDays(10));
        return qualification;
    }

}
