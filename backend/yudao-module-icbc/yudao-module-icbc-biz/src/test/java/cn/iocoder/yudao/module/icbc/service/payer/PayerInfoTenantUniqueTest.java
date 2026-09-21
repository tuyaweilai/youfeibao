package cn.iocoder.yudao.module.icbc.service.payer;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerAddReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.test.icbc.IcbcTenantTestConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;

import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 付方档案的信用代码 / 税号是**全局唯一键**：ADR 0005 补充「一家公司只能是一个租户」。
 *
 * <p>本用例真开租户拦截器（{@link IcbcTenantTestConfiguration}），因为要验的正是「别家企业先用过」
 * 这条跨租户规则——租户过滤关着时，服务层看到的是同一张表，测不到这层。
 *
 * <p>两条口径：
 * <ul>
 *   <li>撞上时给**可读错误**（不是裸 {@code DuplicateKeyException} → 500）；</li>
 *   <li>文案**不点名**是哪一家企业、不回租户名 / 租户编号——跨租户只暴露「这个值被占用了」，
 *       不泄露别家企业的存在（与 #91 把跨租户作废链接做成「当作不存在」同一条透明度口径）。</li>
 * </ul>
 */
@Import({UnitTestConfiguration.class, IcbcTenantTestConfiguration.class, PayerInfoServiceImpl.class})
@Sql(scripts = "/sql/create_tables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class PayerInfoTenantUniqueTest extends BaseDbUnitTest {

    private static final String CREDIT_CODE = "91110105MA01R2278M";
    private static final String TAX_NO = "91110105MA01R2278M";

    @Resource
    private PayerInfoService payerInfoService;

    @Resource
    private PayerInfoMapper payerInfoMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testSecondTenantCannotRegisterSameCreditCode() {
        // 租户 1 先建付方（它就是本企业的开票主体）
        Long payerId = TenantUtils.execute(1L, () -> payerInfoService.createPayerInfo(
                saveReq("北京某某再生资源有限公司", CREDIT_CODE, TAX_NO)));
        assertNotNull(payerId);

        // 租户 2 用同一个信用代码再建：跨租户预检命中 → 可读错误，而不是唯一键 500
        ServiceException ex = assertThrows(ServiceException.class, () -> TenantUtils.execute(2L, () -> {
            payerInfoService.createPayerInfo(
                    saveReq("某某再生资源（第二家）", CREDIT_CODE, "91110105MA01R2278X"));
        }));
        assertEquals(PAYER_CREDIT_CODE_REGISTERED_ELSEWHERE.getCode(), ex.getCode());
        assertEquals(PAYER_CREDIT_CODE_REGISTERED_ELSEWHERE.getMsg(), ex.getMessage());
        // 文案不点名：不出现「租户」字样，也不出现那家企业的名字
        assertFalse(ex.getMessage().contains("租户"), "跨租户错误不许暴露租户维度：" + ex.getMessage());
        assertFalse(ex.getMessage().contains("北京某某再生资源有限公司"),
                "跨租户错误不许点名是哪一家企业：" + ex.getMessage());
        // 租户 2 什么都没落库
        TenantUtils.execute(2L, () -> assertNull(payerInfoMapper.selectByCreditCode(CREDIT_CODE)));
    }

    @Test
    public void testSecondTenantCannotRegisterSameTaxNo() {
        TenantUtils.execute(1L, () -> payerInfoService.createPayerInfo(
                saveReq("北京某某再生资源有限公司", CREDIT_CODE, TAX_NO)));

        // 信用代码不同、税号相同：挡在税号那条键上
        ServiceException ex = assertThrows(ServiceException.class, () -> TenantUtils.execute(2L, () -> {
            payerInfoService.createPayerInfo(
                    saveReq("某某再生资源（第二家）", "91110105MA01R2279N", TAX_NO));
        }));
        assertEquals(PAYER_TAX_NO_REGISTERED_ELSEWHERE.getCode(), ex.getCode());
        assertEquals(PAYER_TAX_NO_REGISTERED_ELSEWHERE.getMsg(), ex.getMessage());
        assertFalse(ex.getMessage().contains("租户"), "跨租户错误不许暴露租户维度：" + ex.getMessage());
    }

    @Test
    public void testSecondTenantCanRegisterADifferentPayer() {
        // 不同企业（不同信用代码 / 税号）互不相干：跨租户预检不能把正常业务也拦掉
        TenantUtils.execute(1L, () -> payerInfoService.createPayerInfo(
                saveReq("北京某某再生资源有限公司", CREDIT_CODE, TAX_NO)));
        Long payerId2 = TenantUtils.execute(2L, () -> payerInfoService.createPayerInfo(
                saveReq("天津某某再生资源有限公司", "91120101MA01R2279P", "91120101MA01R2279P")));
        assertNotNull(payerId2);
    }

    @Test
    public void testSameCreditCodeStillRejectedWithinOneTenant() {
        // 同租户内重复是原有的租户内校验，口径未变（不是「另一家企业」那条文案）
        TenantUtils.execute(1L, () -> payerInfoService.createPayerInfo(
                saveReq("北京某某再生资源有限公司", CREDIT_CODE, TAX_NO)));
        ServiceException ex = assertThrows(ServiceException.class, () -> TenantUtils.execute(1L, () -> {
            payerInfoService.createPayerInfo(
                    saveReq("北京某某再生资源有限公司", CREDIT_CODE, "91110105MA01R2278X"));
        }));
        assertEquals(PAYER_INFO_CREDIT_CODE_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testUpdateToAnotherTenantsCreditCodeIsRejected() {
        TenantUtils.execute(1L, () -> payerInfoService.createPayerInfo(
                saveReq("北京某某再生资源有限公司", CREDIT_CODE, TAX_NO)));
        Long ownPayerId = TenantUtils.execute(2L, () -> payerInfoService.createPayerInfo(
                saveReq("天津某某再生资源有限公司", "91120101MA01R2279P", "91120101MA01R2279P")));

        // 租户 2 把自己的付方改成租户 1 的信用代码：同样要可读错误，不是唯一键 500
        PayerInfoSaveReqVO updateReq = saveReq("天津某某再生资源有限公司", CREDIT_CODE, "91120101MA01R2279P");
        updateReq.setId(ownPayerId);
        ServiceException ex = assertThrows(ServiceException.class, () -> TenantUtils.execute(2L, () -> {
            payerInfoService.updatePayerInfo(updateReq);
        }));
        assertEquals(PAYER_CREDIT_CODE_REGISTERED_ELSEWHERE.getCode(), ex.getCode());
        assertFalse(ex.getMessage().contains("租户"), "跨租户错误不许暴露租户维度：" + ex.getMessage());
    }

    @Test
    public void testCreditCodeCaseAndWhitespaceDoNotBypassTheGlobalKey() {
        // 信用代码规格上是大写字母数字；大小写 / 首尾空白的差异不能让同一个代码绕过预检又不撞键
        TenantUtils.execute(1L, () -> payerInfoService.createPayerInfo(
                saveReq("北京某某再生资源有限公司", CREDIT_CODE, TAX_NO)));

        ServiceException ex = assertThrows(ServiceException.class, () -> TenantUtils.execute(2L, () -> {
            payerInfoService.createPayerInfo(
                    saveReq("某某再生资源（第二家）", " " + CREDIT_CODE.toLowerCase() + " ", "91110105MA01R2278X"));
        }));
        assertEquals(PAYER_CREDIT_CODE_REGISTERED_ELSEWHERE.getCode(), ex.getCode());

        // 归一是落库行为，不只是比对行为：写进去的是规范形状（大写、去首尾空白）
        Long payerId2 = TenantUtils.execute(2L, () -> payerInfoService.createPayerInfo(
                saveReq("某某再生资源（第二家）", " 91110105ma01r2279n ", " 91110105ma01r2279n ")));
        TenantUtils.execute(2L, () -> {
            PayerInfoDO stored = payerInfoMapper.selectById(payerId2);
            assertEquals("91110105MA01R2279N", stored.getCreditCode());
            assertEquals("91110105MA01R2279N", stored.getTaxNo());
        });
    }

    @Test
    public void testAddPayerToIcbcInSecondTenantIsRejected() {
        // 「工行付方新增」是另一条创建路径，同样不能绕过全局唯一键（否则它仍是 500）
        TenantUtils.execute(1L, () -> payerInfoService.addPayerToIcbc(addReq(CREDIT_CODE, TAX_NO)));

        ServiceException ex = assertThrows(ServiceException.class, () -> TenantUtils.execute(2L, () -> {
            payerInfoService.addPayerToIcbc(addReq(CREDIT_CODE, "91110105MA01R2278X"));
        }));
        assertEquals(PAYER_CREDIT_CODE_REGISTERED_ELSEWHERE.getCode(), ex.getCode());
        assertFalse(ex.getMessage().contains("租户"), "跨租户错误不许暴露租户维度：" + ex.getMessage());
    }

    private PayerInfoSaveReqVO saveReq(String name, String creditCode, String taxNo) {
        PayerInfoSaveReqVO reqVO = new PayerInfoSaveReqVO();
        reqVO.setName(name);
        reqVO.setCreditCode(creditCode);
        reqVO.setTaxNo(taxNo);
        reqVO.setStatus(0);
        return reqVO;
    }

    private PayerAddReqVO addReq(String creditCode, String taxNo) {
        PayerAddReqVO reqVO = new PayerAddReqVO();
        reqVO.setName("北京某某再生资源有限公司");
        reqVO.setCreditCode(creditCode);
        reqVO.setTaxNo(taxNo);
        reqVO.setContactName("张三");
        reqVO.setContactMobile("13800138000");
        reqVO.setTaxpayerType("01");
        return reqVO;
    }

}
