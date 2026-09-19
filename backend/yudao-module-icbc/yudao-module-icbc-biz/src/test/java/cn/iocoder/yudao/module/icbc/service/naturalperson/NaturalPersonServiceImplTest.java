package cn.iocoder.yudao.module.icbc.service.naturalperson;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson.IcbcNaturalPersonMapper;
import cn.iocoder.yudao.module.icbc.enums.NaturalPersonStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NaturalPersonServiceImpl} 的单元测试。
 *
 * <p>断言的是 ADR 0017 的对外行为：一个人只有一个身份锚点、身份登记不覆盖不合并、
 * 登录凭证与身份是多对多、解绑不删数据。
 */
@Import({NaturalPersonServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class NaturalPersonServiceImplTest extends BaseDbUnitTest {

    private static final String ID_CARD = "110101199001011234";
    private static final String MOBILE = "13800138000";

    @Resource
    private NaturalPersonService naturalPersonService;

    @Resource
    private IcbcNaturalPersonMapper naturalPersonMapper;

    // ==================== 身份登记 ====================

    @Test
    public void testRegister_createsPersonWithOutUserId() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        assertNotNull(person.getId());
        assertNotNull(person.getOutUserId());
        assertEquals(PayeeRealNameStatusEnum.NOT_STARTED.getStatus(), person.getRealNameStatus());
        // 身份证是唯一锚点：按号码能再查回来
        assertEquals(person.getId(), naturalPersonService.getByIdCardNo(ID_CARD).getId());
        // outUserId 是平台级的，回调按它找人
        assertEquals(person.getId(), naturalPersonService.getByOutUserId(person.getOutUserId()).getId());
    }

    @Test
    public void testRegister_sameIdentityIsIdempotent() {
        IcbcNaturalPersonDO first = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));
        IcbcNaturalPersonDO second = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        // 同一个人不会产生第二条主体，outUserId 也不会换
        assertEquals(first.getId(), second.getId());
        assertEquals(first.getOutUserId(), second.getOutUserId());
        assertEquals(1, naturalPersonMapper.selectList().size());
    }

    @Test
    public void testRegister_differentMobileIsRejectedNotMerged() {
        naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        assertServiceException(() -> naturalPersonService.register(registerReq("张三", ID_CARD, "13900139000")),
                NATURAL_PERSON_IDENTITY_TAKEN);
        // 原有主体的手机号没有被覆盖
        assertEquals(MOBILE, naturalPersonService.getByIdCardNo(ID_CARD).getMobile());
    }

    @Test
    public void testRegister_differentNameIsRejectedNotMerged() {
        naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        assertServiceException(() -> naturalPersonService.register(registerReq("李四", ID_CARD, MOBILE)),
                NATURAL_PERSON_IDENTITY_TAKEN);
        assertEquals("张三", naturalPersonService.getByIdCardNo(ID_CARD).getName());
    }

    @Test
    public void testRegister_disabledIdentityIsRejected() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));
        naturalPersonService.updateStatus(person.getId(), NaturalPersonStatusEnum.DISABLED.getStatus(), "冒用核查");

        assertServiceException(() -> naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE)),
                NATURAL_PERSON_DISABLED);
    }

    // ==================== 实人认证 ====================

    @Test
    public void testApplyRealNameResult_passedRecordsTime() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        naturalPersonService.applyRealNameResult(person.getId(), true, null);

        IcbcNaturalPersonDO refreshed = naturalPersonService.getNaturalPerson(person.getId());
        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(), refreshed.getRealNameStatus());
        assertNotNull(refreshed.getRealNameTime());
        assertNull(refreshed.getRealNameMsg());
    }

    @Test
    public void testApplyRealNameResult_failedRecordsReason() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        naturalPersonService.applyRealNameResult(person.getId(), false, "活体检测未通过");

        IcbcNaturalPersonDO refreshed = naturalPersonService.getNaturalPerson(person.getId());
        assertEquals(PayeeRealNameStatusEnum.FAILED.getStatus(), refreshed.getRealNameStatus());
        assertEquals("活体检测未通过", refreshed.getRealNameMsg());
    }

    @Test
    public void testApplyRealNameResult_pendingKeepsStatus() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));
        naturalPersonService.applyRealNameResult(person.getId(), true, null);
        // 认证中：未通过又没有失败原因
        naturalPersonService.applyRealNameResult(person.getId(), false, null);

        assertEquals(PayeeRealNameStatusEnum.PASSED.getStatus(),
                naturalPersonService.getNaturalPerson(person.getId()).getRealNameStatus());
    }

    // ==================== 登录凭证 ====================

    @Test
    public void testBindLogin_oneCredentialCanHoldTwoSubjects() {
        IcbcNaturalPersonDO elderly = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));
        IcbcNaturalPersonDO child = naturalPersonService.register(
                registerReq("张小三", "110101199001011235", "13800138001"));

        // 子女用同一个手机号（登录凭证）代两位老人操作
        naturalPersonService.bindLogin(elderly.getId(), 9001L, "REGISTER", null);
        naturalPersonService.bindLogin(child.getId(), 9001L, "REGISTER", null);

        List<IcbcNaturalPersonDO> subjects = naturalPersonService.getNaturalPersonListByMemberUserId(9001L);
        assertEquals(2, subjects.size());
        assertTrue(naturalPersonService.isBoundToLogin(elderly.getId(), 9001L));
        assertTrue(naturalPersonService.isBoundToLogin(child.getId(), 9001L));
        // 换个凭证就不认
        assertFalse(naturalPersonService.isBoundToLogin(elderly.getId(), 9002L));
    }

    @Test
    public void testBindLogin_isIdempotent() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        naturalPersonService.bindLogin(person.getId(), 9001L, "REGISTER", null);
        naturalPersonService.bindLogin(person.getId(), 9001L, "REGISTER", null);

        assertEquals(1, naturalPersonService.getNaturalPersonListByMemberUserId(9001L).size());
    }

    @Test
    public void testUnbindLogin_keepsIdentityAndHistory() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));
        naturalPersonService.bindLogin(person.getId(), 9001L, "REGISTER", null);

        naturalPersonService.unbindLogin(person.getId(), 9001L);

        // 凭证没了
        assertTrue(naturalPersonService.getNaturalPersonListByMemberUserId(9001L).isEmpty());
        assertFalse(naturalPersonService.isBoundToLogin(person.getId(), 9001L));
        // 主体还在（注销账号不等于删除交易）
        assertNotNull(naturalPersonService.getNaturalPerson(person.getId()));
    }

    // ==================== 平台运营 ====================

    @Test
    public void testUpdateStatus_recordsRemark() {
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReq("张三", ID_CARD, MOBILE));

        naturalPersonService.updateStatus(person.getId(), NaturalPersonStatusEnum.DISABLED.getStatus(), "身份证被他人冒用");

        IcbcNaturalPersonDO refreshed = naturalPersonService.getNaturalPerson(person.getId());
        assertEquals(NaturalPersonStatusEnum.DISABLED.getStatus(), refreshed.getStatus());
        assertEquals("身份证被他人冒用", refreshed.getRemark());
    }

    private NaturalPersonRegisterReqVO registerReq(String name, String idCardNo, String mobile) {
        NaturalPersonRegisterReqVO reqVO = new NaturalPersonRegisterReqVO();
        reqVO.setName(name);
        reqVO.setIdCardNo(idCardNo);
        reqVO.setMobile(mobile);
        return reqVO;
    }

}
