package cn.iocoder.yudao.module.icbc.service.payer;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoExportReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.payer.PayerInfoMapper;
import cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static org.junit.jupiter.api.Assertions.*;

/**
* {@link PayerInfoServiceImpl} 的单元测试类
*/
@Import({UnitTestConfiguration.class, PayerInfoServiceImpl.class})
@Sql("/sql/create_tables.sql")
@Transactional
@Rollback
public class PayerInfoServiceImplTest extends BaseDbUnitTest {

    @Resource
    private PayerInfoServiceImpl payerInfoService;

    @Resource
    private PayerInfoMapper payerInfoMapper;
    
    // 要忽略的字段列表，包括deleted字段和其他不需要比较的字段
    private static final String[] IGNORE_FIELDS = new String[]{"deleted", "id", "createTime", "updateTime", "creator", "updater", "transMap", "payerNo", "partnerPayerId", "status", "auditMsg", "icbcPayerStatus", "icbcMediumId", "icbcOpenacctStatus", "tenantId", "telephone"};

    @Test
    public void testSave_success() {
        // 准备参数
        PayerInfoSaveReqVO saveReqVO = randomPojo(PayerInfoSaveReqVO.class, o -> {
            o.setCreditCode("CREDIT_" + randomString()); // 随机信用代码
            o.setTaxNo("TAX_" + randomString()); // 随机税号
            o.setTelephone("010-12345678"); // 设置固定的电话号码
        });

        // 调用
        Long payerId = payerInfoService.createPayerInfo(saveReqVO);
        
        // 断言
        assertNotNull(payerId);
        
        // 校验记录的属性是否正确
        PayerInfoDO payer = payerInfoMapper.selectById(payerId);
        assertPojoEquals(saveReqVO, payer, IGNORE_FIELDS);
    }

    @Test
    public void testSave_creditCodeExists() {
        // mock 数据
        PayerInfoDO dbPayer = randomPojo(PayerInfoDO.class, o -> {
            o.setCreditCode("test-credit-code-exists");
            o.setTaxNo("TAX_" + randomString()); // 确保税号不重复
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(dbPayer);
        
        // 准备参数
        PayerInfoSaveReqVO saveReqVO = randomPojo(PayerInfoSaveReqVO.class, o -> {
            o.setCreditCode("test-credit-code-exists"); // 与数据库中的重复
            o.setTaxNo("TAX_" + randomString()); // 确保税号不重复
        });

        // 调用，并断言异常
        assertServiceException(() -> payerInfoService.createPayerInfo(saveReqVO), 
                ErrorCodeConstants.PAYER_INFO_CREDIT_CODE_EXISTS); // 使用正确的错误码
    }

    @Test
    public void testSave_taxNoExists() {
        // mock 数据
        PayerInfoDO dbPayer = randomPojo(PayerInfoDO.class, o -> {
            o.setTaxNo("test-tax-no-exists");
            o.setCreditCode("CREDIT_" + randomString()); // 确保信用代码不重复
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(dbPayer);
        
        // 准备参数
        PayerInfoSaveReqVO saveReqVO = randomPojo(PayerInfoSaveReqVO.class, o -> {
            o.setTaxNo("test-tax-no-exists"); // 与数据库中的重复
            o.setCreditCode("CREDIT_" + randomString()); // 确保信用代码不重复
        });

        // 调用，并断言异常
        assertServiceException(() -> payerInfoService.createPayerInfo(saveReqVO), 
                ErrorCodeConstants.PAYER_INFO_TAX_NO_EXISTS); // 使用正确的错误码
    }

    @Test
    public void testUpdatePayer_success() {
        // mock 数据
        PayerInfoDO dbPayer = randomPojo(PayerInfoDO.class, o -> {
            o.setStatus(CommonStatusEnum.DISABLE.getStatus());
            o.setCreditCode("CREDIT_" + randomString()); // 确保信用代码唯一
            o.setTaxNo("TAX_" + randomString()); // 确保税号唯一
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(dbPayer);
        
        // 准备参数
        PayerInfoSaveReqVO updateReqVO = randomPojo(PayerInfoSaveReqVO.class, o -> {
            o.setId(dbPayer.getId());
            o.setCreditCode(dbPayer.getCreditCode()); // 保持相同的信用代码
            o.setTaxNo(dbPayer.getTaxNo()); // 保持相同的税号
            o.setName("更新后的企业名称"); // 更新企业名称
        });
        
        // 调用
        payerInfoService.updatePayerInfo(updateReqVO);
        
        // 校验更新
        PayerInfoDO payer = payerInfoMapper.selectById(dbPayer.getId());
        assertEquals("更新后的企业名称", payer.getName());
    }

    @Test
    public void testGetPayerPage() {
        // mock 数据
        PayerInfoDO dbPayer = randomPojo(PayerInfoDO.class, o -> {
            o.setName("测试名称");
            o.setCreditCode("test-credit-code-page");
            o.setTaxNo("test-tax-no-page");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28)[0]);
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(dbPayer);
        
        // 测试 name 不匹配
        PayerInfoDO dbPayer2 = randomPojo(PayerInfoDO.class, o -> {
            o.setName("不匹配");
            o.setCreditCode("test-credit-code-page-2");
            o.setTaxNo("test-tax-no-page-2");
            o.setDeleted(false);
        });
        payerInfoMapper.insert(dbPayer2);
        
        // 准备参数
        PayerInfoPageReqVO reqVO = new PayerInfoPageReqVO();
        reqVO.setName("测试名称");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28));

        // 调用
        PageResult<PayerInfoDO> pageResult = payerInfoService.getPayerInfoPage(reqVO);
        
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbPayer, pageResult.getList().get(0), IGNORE_FIELDS);
    }

    @Test
    public void testGetPayerList_export() {
        // mock 数据
        PayerInfoDO dbPayer = randomPojo(PayerInfoDO.class, o -> {
            o.setName("测试名称");
            o.setCreditCode("test-credit-code-export");
            o.setTaxNo("test-tax-no-export");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28)[0]);
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(dbPayer);
        
        // 测试 name 不匹配
        PayerInfoDO dbPayer2 = randomPojo(PayerInfoDO.class, o -> {
            o.setName("不匹配");
            o.setCreditCode("test-credit-code-export-2");
            o.setTaxNo("test-tax-no-export-2");
            o.setDeleted(false);
        });
        payerInfoMapper.insert(dbPayer2);
        
        // 准备参数
        PayerInfoExportReqVO reqVO = new PayerInfoExportReqVO();
        reqVO.setName("测试名称");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28));

        // 调用
        List<PayerInfoDO> list = payerInfoService.getPayerInfoList(reqVO);
        
        // 断言
        assertEquals(1, list.size());
        assertPojoEquals(dbPayer, list.get(0), IGNORE_FIELDS);
    }
}
