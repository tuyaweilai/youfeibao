package cn.iocoder.yudao.module.icbc.service.payee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.enums.IcbcOccupationEnum;
import cn.iocoder.yudao.module.icbc.enums.IcbcStatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PayeeInfoServiceImpl} 的单元测试类
 *
 * @author 芋道源码
 */
@Import({PayeeInfoServiceImpl.class, cn.iocoder.yudao.module.icbc.UnitTestConfiguration.class})
@Transactional
@Rollback
public class PayeeInfoServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PayeeInfoServiceImpl payeeInfoService;

    @Resource
    private PayeeInfoMapper payeeInfoMapper;
    @Resource
    private NaturalPersonService naturalPersonService;

    @Test
    public void testCreatePayeeInfo_success() {
        // 准备参数
        PayeeInfoSaveReqVO createReqVO = randomPojo(PayeeInfoSaveReqVO.class, o -> {
            o.setIdCardNo("110101199001011234");
            o.setMobile("13800138000");
        });

        // 调用
        Long payeeInfoId = payeeInfoService.createPayeeInfo(createReqVO);
        // 断言
        assertNotNull(payeeInfoId);
        // 校验记录的属性是否正确
        PayeeInfoDO payeeInfo = payeeInfoMapper.selectById(payeeInfoId);
        assertPojoEquals(createReqVO, payeeInfo, "id");
        assertNotNull(payeeInfo.getPartnerPayeeId());
    }

    @Test
    public void testCreatePayeeInfo_idCardNoExists() {
        // mock 数据
        PayeeInfoDO existPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setIdCardNo("110101199001011235");
            o.setMobile("13800138001");
        });
        payeeInfoMapper.insert(existPayeeInfo);
        // 准备参数
        PayeeInfoSaveReqVO createReqVO = randomPojo(PayeeInfoSaveReqVO.class, o -> {
            o.setIdCardNo("110101199001011235");
            o.setMobile("13800138002");
        });

        // 调用，并断言异常
        assertServiceException(() -> payeeInfoService.createPayeeInfo(createReqVO), PAYEE_ID_CARD_EXISTS);
    }

    @Test
    public void testCreatePayeeInfo_mobileExists() {
        // mock 数据
        PayeeInfoDO existPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setIdCardNo("110101199001011236");
            o.setMobile("13800138003");
        });
        payeeInfoMapper.insert(existPayeeInfo);
        // 准备参数
        PayeeInfoSaveReqVO createReqVO = randomPojo(PayeeInfoSaveReqVO.class, o -> {
            o.setIdCardNo("110101199001011237");
            o.setMobile("13800138003");
        });

        // 调用，并断言异常
        assertServiceException(() -> payeeInfoService.createPayeeInfo(createReqVO), PAYEE_MOBILE_EXISTS);
    }

    @Test
    public void testUpdatePayeeInfo_success() {
        // mock 数据
        PayeeInfoDO dbPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setIdCardNo("110101199001011238");
            o.setMobile("13800138004");
        });
        payeeInfoMapper.insert(dbPayeeInfo);// @Sql 执行的 SQL 对应的 PayeeInfo 记录
        // 准备参数
        PayeeInfoSaveReqVO updateReqVO = randomPojo(PayeeInfoSaveReqVO.class, o -> {
            o.setId(dbPayeeInfo.getId()); // 设置更新的 ID
            o.setIdCardNo(dbPayeeInfo.getIdCardNo()); // 保持相同的身份证号
            o.setMobile(dbPayeeInfo.getMobile()); // 保持相同的手机号
            // 银行卡号与职业有格式 / 字典约束（#84）：randomPojo 给这两个字段生成不出合法值，显式给
            o.setBankCardNo("6222021234567890123");
            o.setOccupation(IcbcOccupationEnum.OTHER.getCode());
            // 证件签发 / 截止日期与「是否我行卡」是新加的字段（#91），randomPojo 同样造不出合法值；
            // 而且 updateById 会忽略 null，不给的话库里会留着 dbPayeeInfo 的随机值，令下面的比对失败
            o.setIdSignDate("2020-01-01");
            o.setIdValidityPeriod("2030-01-01");
            o.setAccountCode("1");
        });

        // 调用
        payeeInfoService.updatePayeeInfo(updateReqVO);
        // 校验是否更新正确
        PayeeInfoDO payeeInfo = payeeInfoMapper.selectById(updateReqVO.getId()); // 获取最新的
        assertPojoEquals(updateReqVO, payeeInfo);
    }

    @Test
    public void testUpdatePayeeInfo_notExists() {
        // 准备参数
        PayeeInfoSaveReqVO updateReqVO = randomPojo(PayeeInfoSaveReqVO.class);

        // 调用, 并断言异常
        assertServiceException(() -> payeeInfoService.updatePayeeInfo(updateReqVO), PAYEE_NOT_EXISTS);
    }

    @Test
    public void testDeletePayeeInfo_success() {
        // mock 数据
        PayeeInfoDO dbPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setIdCardNo("110101199001011239");
            o.setMobile("13800138005");
        });
        payeeInfoMapper.insert(dbPayeeInfo);// @Sql 执行的 SQL 对应的 PayeeInfo 记录
        // 准备参数
        Long id = dbPayeeInfo.getId();

        // 调用
        payeeInfoService.deletePayeeInfo(id);
       // 校验数据不存在了
       assertNull(payeeInfoMapper.selectById(id));
    }

    @Test
    public void testDeletePayeeInfo_notExists() {
        // 准备参数
        Long id = randomLongId();

        // 调用, 并断言异常
        assertServiceException(() -> payeeInfoService.deletePayeeInfo(id), PAYEE_NOT_EXISTS);
    }

    @Test
    public void testGetPayeeInfoPage() {
        // mock 数据
        PayeeInfoDO dbPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setName("张三");
            o.setIdCardNo("110101199001011240");
            o.setMobile("13800138006");
            o.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
            o.setBusinessType("RECYCLE");
        });
        payeeInfoMapper.insert(dbPayeeInfo);
        // 测试 name 不匹配
        payeeInfoMapper.insert(cloneIgnoreId(dbPayeeInfo, o -> {
            o.setName("李四");
            o.setIdCardNo("110101199001011241");
            o.setMobile("13800138007");
        }));
        // 测试 idCardNo 不匹配
        payeeInfoMapper.insert(cloneIgnoreId(dbPayeeInfo, o -> {
            o.setIdCardNo("110101199001011242");
            o.setMobile("13800138008");
        }));
        // 测试 mobile 不匹配
        payeeInfoMapper.insert(cloneIgnoreId(dbPayeeInfo, o -> {
            o.setIdCardNo("110101199001011243");
            o.setMobile("13800138009");
        }));
        // 测试 status 不匹配
        payeeInfoMapper.insert(cloneIgnoreId(dbPayeeInfo, o -> {
            o.setIdCardNo("110101199001011244");
            o.setMobile("13800138010");
            o.setStatus(IcbcStatusEnum.AuditStatus.PENDING.getStatus());
        }));
        // 测试 businessType 不匹配
        payeeInfoMapper.insert(cloneIgnoreId(dbPayeeInfo, o -> {
            o.setIdCardNo("110101199001011245");
            o.setMobile("13800138011");
            o.setBusinessType("OTHER");
        }));
        // 准备参数
        PayeeInfoPageReqVO reqVO = new PayeeInfoPageReqVO();
        reqVO.setName("张");
        reqVO.setIdCardNo("110101199001011240");
        reqVO.setMobile("13800138006");
        reqVO.setStatus(IcbcStatusEnum.AuditStatus.APPROVED.getStatus());
        reqVO.setBusinessType("RECYCLE");

        // 调用
        PageResult<PayeeInfoDO> pageResult = payeeInfoService.getPayeeInfoPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbPayeeInfo, pageResult.getList().get(0));
    }

    @Test
    public void testAddPayeeToIcbc_success() {
        // 准备参数
        PayeeAddReqVO reqVO = randomPojo(PayeeAddReqVO.class, o -> {
            o.setOutUserId("USER002");
            o.setReceiverName("张三");
            o.setIdNo("110101199001011246");
            o.setMobile("13800138012");
            o.setReceiverAccount("6222021234567890124");
        });

        // 调用
        Long payeeInfoId = payeeInfoService.addPayeeToIcbc(reqVO);
        
        // 断言
        assertNotNull(payeeInfoId);
        PayeeInfoDO payeeInfo = payeeInfoMapper.selectById(payeeInfoId);
        assertNotNull(payeeInfo);
        assertEquals("张三", payeeInfo.getName());
        assertEquals("110101199001011246", payeeInfo.getIdCardNo());
        assertEquals("13800138012", payeeInfo.getMobile());
        assertEquals("6222021234567890124", payeeInfo.getBankCardNo());
        // partnerPayeeId 已是「收方档案编号」，不再是工行 outUserId（outUserId 归自然人主体）
        assertNotEquals("USER002", payeeInfo.getPartnerPayeeId());
        assertNotNull(payeeInfo.getPartnerPayeeId());
        // 请求里的 outUserId 指向平台级自然人主体；查不到这个人时按身份登记建一个再挂上去
        assertNotNull(payeeInfo.getNaturalPersonId());
        assertEquals("110101199001011246",
                naturalPersonService.getNaturalPerson(payeeInfo.getNaturalPersonId()).getIdCardNo());
        assertEquals("RECYCLE", payeeInfo.getBusinessType());
        assertEquals(Integer.valueOf(0), payeeInfo.getStatus());
        assertNotNull(payeeInfo.getPayeeNo());
        assertEquals("0", payeeInfo.getIcbcReceiverStatus());
    }

    @Test
    public void testAddPayeeToIcbc_reusesExistingNaturalPersonByOutUserId() {
        // 这个人已经在别的回收企业建过档：请求带上他的平台级外部用户编号
        NaturalPersonRegisterReqVO registerReqVO = new NaturalPersonRegisterReqVO();
        registerReqVO.setName("李四");
        registerReqVO.setIdCardNo("110101199001011299");
        registerReqVO.setMobile("13800138099");
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReqVO);

        PayeeAddReqVO reqVO = randomPojo(PayeeAddReqVO.class, o -> {
            o.setOutUserId(person.getOutUserId());
            o.setReceiverName("李四");
            o.setIdNo("110101199001011299");
            o.setMobile("13800138099");
        });
        Long payeeInfoId = payeeInfoService.addPayeeToIcbc(reqVO);

        // 复用的是同一个自然人主体，而不是又建一个身份
        assertEquals(person.getId(), payeeInfoMapper.selectById(payeeInfoId).getNaturalPersonId());
    }

    @Test
    public void testHandlePayeeAuditCallback_approved() {
        PayeeWithPerson fx = insertPayeeWithPerson("USER001", "110101199001011301", "13800138301");

        // 调用：outUserId 是平台级外部用户编号，先定位自然人主体再取本租户的收方档案
        payeeInfoService.handlePayeeAuditCallback(fx.person().getOutUserId(), "1", "审核通过");
        PayeeInfoDO dbPayeeInfo = fx.payee();

        // 断言
        PayeeInfoDO updatedPayeeInfo = payeeInfoMapper.selectById(dbPayeeInfo.getId());
        assertEquals(IcbcStatusEnum.AuditStatus.APPROVED.getStatus(), updatedPayeeInfo.getStatus());
        assertEquals("审核通过", updatedPayeeInfo.getAuditMsg());
        assertEquals("1", updatedPayeeInfo.getIcbcReceiverStatus());
    }

    @Test
    public void testHandlePayeeAuditCallback_rejected() {
        PayeeWithPerson fx = insertPayeeWithPerson("USER001", "110101199001011302", "13800138302");

        // 调用
        payeeInfoService.handlePayeeAuditCallback(fx.person().getOutUserId(), "2", "审核拒绝");
        PayeeInfoDO dbPayeeInfo = fx.payee();

        // 断言
        PayeeInfoDO updatedPayeeInfo = payeeInfoMapper.selectById(dbPayeeInfo.getId());
        assertEquals(IcbcStatusEnum.AuditStatus.REJECTED.getStatus(), updatedPayeeInfo.getStatus());
        assertEquals("审核拒绝", updatedPayeeInfo.getAuditMsg());
        assertEquals("0", updatedPayeeInfo.getIcbcReceiverStatus());
    }

    /** 收方档案 + 它挂着的自然人主体。 */
    private static class PayeeWithPerson {

        private final PayeeInfoDO payee;
        private final IcbcNaturalPersonDO person;

        PayeeWithPerson(PayeeInfoDO payee, IcbcNaturalPersonDO person) {
            this.payee = payee;
            this.person = person;
        }

        PayeeInfoDO payee() {
            return payee;
        }

        IcbcNaturalPersonDO person() {
            return person;
        }

    }

    /**
     * 建一条收方档案并挂到平台级自然人主体上（与 {@code PayeeInfoServiceImpl.createPayeeInfo} 同一口径）。
     */
    private PayeeWithPerson insertPayeeWithPerson(String partnerPayeeId, String idCardNo, String mobile) {
        NaturalPersonRegisterReqVO registerReqVO = new NaturalPersonRegisterReqVO();
        registerReqVO.setName("张三");
        registerReqVO.setIdCardNo(idCardNo);
        registerReqVO.setMobile(mobile);
        IcbcNaturalPersonDO person = naturalPersonService.register(registerReqVO);

        PayeeInfoDO payee = randomPojo(PayeeInfoDO.class, o -> {
            o.setPartnerPayeeId(partnerPayeeId);
            o.setNaturalPersonId(person.getId());
            o.setStatus(IcbcStatusEnum.AuditStatus.PENDING.getStatus());
        });
        payeeInfoMapper.insert(payee);
        return new PayeeWithPerson(payee, person);
    }

}
