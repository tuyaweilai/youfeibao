package cn.iocoder.yudao.module.icbc.service.payee;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.controller.admin.payee.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payee.PayeeInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payee.PayeeInfoMapper;
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
        assertEquals("USER002", payeeInfo.getPartnerPayeeId());
        assertEquals("RECYCLE", payeeInfo.getBusinessType());
        assertEquals(Integer.valueOf(0), payeeInfo.getStatus());
        assertNotNull(payeeInfo.getPayeeNo());
        assertEquals("0", payeeInfo.getIcbcReceiverStatus());
        assertEquals("01", payeeInfo.getIcbcOpenacctStatus());
    }

    @Test
    public void testHandlePayeeAuditCallback_approved() {
        // mock 数据
        PayeeInfoDO dbPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setPartnerPayeeId("USER001");
            o.setStatus(IcbcStatusEnum.AuditStatus.PENDING.getStatus());
        });
        payeeInfoMapper.insert(dbPayeeInfo);

        // 调用
        payeeInfoService.handlePayeeAuditCallback("USER001", "1", "审核通过", "ICBC123456");

        // 断言
        PayeeInfoDO updatedPayeeInfo = payeeInfoMapper.selectById(dbPayeeInfo.getId());
        assertEquals(IcbcStatusEnum.AuditStatus.APPROVED.getStatus(), updatedPayeeInfo.getStatus());
        assertEquals("审核通过", updatedPayeeInfo.getAuditMsg());
        assertEquals("ICBC123456", updatedPayeeInfo.getIcbcMediumId());
        assertEquals("1", updatedPayeeInfo.getIcbcReceiverStatus());
        assertEquals("02", updatedPayeeInfo.getIcbcOpenacctStatus());
    }

    @Test
    public void testHandlePayeeAuditCallback_rejected() {
        // mock 数据
        PayeeInfoDO dbPayeeInfo = randomPojo(PayeeInfoDO.class, o -> {
            o.setPartnerPayeeId("USER001");
            o.setStatus(IcbcStatusEnum.AuditStatus.PENDING.getStatus());
        });
        payeeInfoMapper.insert(dbPayeeInfo);

        // 调用
        payeeInfoService.handlePayeeAuditCallback("USER001", "2", "审核拒绝", null);

        // 断言
        PayeeInfoDO updatedPayeeInfo = payeeInfoMapper.selectById(dbPayeeInfo.getId());
        assertEquals(IcbcStatusEnum.AuditStatus.REJECTED.getStatus(), updatedPayeeInfo.getStatus());
        assertEquals("审核拒绝", updatedPayeeInfo.getAuditMsg());
        assertEquals("0", updatedPayeeInfo.getIcbcReceiverStatus());
        assertEquals("03", updatedPayeeInfo.getIcbcOpenacctStatus());
    }

} 