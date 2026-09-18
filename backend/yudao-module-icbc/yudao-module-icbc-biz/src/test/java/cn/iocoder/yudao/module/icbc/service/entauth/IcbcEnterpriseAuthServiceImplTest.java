package cn.iocoder.yudao.module.icbc.service.entauth;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthInitReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.entauth.vo.IcbcEnterpriseAuthUpdateReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.entauth.IcbcEnterpriseAuthDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.entauth.IcbcEnterpriseAuthMapper;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGateway;
import cn.iocoder.yudao.module.icbc.gateway.IcbcGatewayResult;
import cn.iocoder.yudao.module.icbc.gateway.model.IcbcPage;
import cn.iocoder.yudao.module.icbc.service.entauth.impl.IcbcEnterpriseAuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.ENTERPRISE_AUTH_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link IcbcEnterpriseAuthServiceImpl} 的单元测试类。
 */
@Import({IcbcEnterpriseAuthServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class IcbcEnterpriseAuthServiceImplTest extends BaseDbUnitTest {

    @Resource
    private IcbcEnterpriseAuthServiceImpl enterpriseAuthService;

    @Resource
    private IcbcEnterpriseAuthMapper enterpriseAuthMapper;

    @MockBean
    private IcbcGateway icbcGateway;

    @Test
    public void testInitEnterpriseAuth_createsPendingRecord() {
        when(icbcGateway.submitEnterpriseAuthorization(any())).thenReturn(
                IcbcGatewayResult.success(IcbcPage.builder().formHtml("<form/>").build(), 0, "成功"));

        IcbcEnterpriseAuthInitReqVO reqVO = new IcbcEnterpriseAuthInitReqVO();
        reqVO.setOutVendorId("010020200513111111");
        reqVO.setSiteType("01");
        reqVO.setUserType("01");

        String formHtml = enterpriseAuthService.initEnterpriseAuth(reqVO);

        assertEquals("<form/>", formHtml);
        IcbcEnterpriseAuthDO record = enterpriseAuthMapper.selectByOutVendorId("010020200513111111");
        assertNotNull(record);
        assertEquals(0, record.getAuthStatus());
    }

    @Test
    public void testUpdateAuthResult_recordsValidity() {
        IcbcEnterpriseAuthDO record = insertRecord("010020200513111111");

        LocalDateTime expireTime = LocalDateTime.now().plusYears(1);
        IcbcEnterpriseAuthUpdateReqVO reqVO = new IcbcEnterpriseAuthUpdateReqVO();
        reqVO.setId(record.getId());
        reqVO.setAuthStatus(1);
        reqVO.setExpireTime(expireTime);

        enterpriseAuthService.updateAuthResult(reqVO);

        IcbcEnterpriseAuthDO updated = enterpriseAuthMapper.selectById(record.getId());
        assertEquals(1, updated.getAuthStatus());
        assertNotNull(updated.getAuthTime(), "已授权时授权时间应被补齐");
        assertTrue(updated.getExpireTime().isEqual(expireTime));
    }

    @Test
    public void testUpdateAuthResult_expiredForcedInvalid() {
        IcbcEnterpriseAuthDO record = insertRecord("010020200513111111");

        IcbcEnterpriseAuthUpdateReqVO reqVO = new IcbcEnterpriseAuthUpdateReqVO();
        reqVO.setId(record.getId());
        reqVO.setAuthStatus(1);
        reqVO.setExpireTime(LocalDateTime.now().minusDays(1));

        enterpriseAuthService.updateAuthResult(reqVO);

        assertEquals(2, enterpriseAuthMapper.selectById(record.getId()).getAuthStatus());
    }

    @Test
    public void testUpdateAuthResult_respectsExplicitStatus() {
        IcbcEnterpriseAuthDO record = insertRecord("010020200513111111");

        // 显式标记未授权时，即使有效期已过也尊重管理员的选择，不强改为已失效
        IcbcEnterpriseAuthUpdateReqVO reqVO = new IcbcEnterpriseAuthUpdateReqVO();
        reqVO.setId(record.getId());
        reqVO.setAuthStatus(0);
        reqVO.setExpireTime(LocalDateTime.now().minusDays(1));

        enterpriseAuthService.updateAuthResult(reqVO);

        assertEquals(0, enterpriseAuthMapper.selectById(record.getId()).getAuthStatus());
    }

    @Test
    public void testUpdateAuthResult_canClearExpireTime() {
        IcbcEnterpriseAuthDO record = insertRecord("010020200513111111");
        IcbcEnterpriseAuthUpdateReqVO first = new IcbcEnterpriseAuthUpdateReqVO();
        first.setId(record.getId());
        first.setAuthStatus(1);
        first.setExpireTime(LocalDateTime.now().plusYears(1));
        enterpriseAuthService.updateAuthResult(first);

        // 再次回填时不传有效期，应能把有效期置空
        IcbcEnterpriseAuthUpdateReqVO second = new IcbcEnterpriseAuthUpdateReqVO();
        second.setId(record.getId());
        second.setAuthStatus(1);
        enterpriseAuthService.updateAuthResult(second);

        assertNull(enterpriseAuthMapper.selectById(record.getId()).getExpireTime());
    }

    @Test
    public void testUpdateAuthResult_notExists() {
        IcbcEnterpriseAuthUpdateReqVO reqVO = new IcbcEnterpriseAuthUpdateReqVO();
        reqVO.setId(999L);
        reqVO.setAuthStatus(1);
        assertServiceException(() -> enterpriseAuthService.updateAuthResult(reqVO),
                ENTERPRISE_AUTH_NOT_EXISTS);
    }

    private IcbcEnterpriseAuthDO insertRecord(String outVendorId) {
        IcbcEnterpriseAuthDO record = new IcbcEnterpriseAuthDO();
        record.setOutVendorId(outVendorId);
        record.setAuthStatus(0);
        enterpriseAuthMapper.insert(record);
        return record;
    }

}
