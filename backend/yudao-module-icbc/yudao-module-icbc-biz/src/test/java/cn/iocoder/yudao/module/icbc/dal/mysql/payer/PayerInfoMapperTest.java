package cn.iocoder.yudao.module.icbc.dal.mysql.payer;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.payer.vo.PayerInfoPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.payer.PayerInfoDO;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Arrays;

import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomLongId;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.randomPojo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PayerInfoMapper} 的单元测试类
 */
@Import(UnitTestConfiguration.class)
@Sql("/sql/create_tables.sql") // 初始化表结构
public class PayerInfoMapperTest extends BaseDbUnitTest {

    @Resource
    private PayerInfoMapper payerInfoMapper;

    // 要忽略的字段列表，包括deleted字段
    private static final String[] IGNORE_FIELDS = new String[]{"deleted"};

    @Test
    public void testInsert() {
        // 准备参数
        PayerInfoDO payerInfo = randomPojo(PayerInfoDO.class, o -> {
            o.setId(null); // ID 需要自增
            o.setTransMap(new HashMap<>()); // 设置transMap以避免序列化问题
            o.setDeleted(false); // 确保deleted字段是false
        });

        // 调用
        payerInfoMapper.insert(payerInfo);
        
        // 断言
        assertNotNull(payerInfo.getId()); // 自增ID应该已赋值
        
        // 从数据库查询
        PayerInfoDO dbPayerInfo = payerInfoMapper.selectById(payerInfo.getId());
        assertPojoEquals(payerInfo, dbPayerInfo, IGNORE_FIELDS); // 忽略特定字段比较
    }

    @Test
    public void testSelectByCreditCode() {
        // 准备参数
        PayerInfoDO payerInfo = randomPojo(PayerInfoDO.class, o -> {
            o.setId(null); // ID 需要自增
            o.setCreditCode("91330102MA2B0B6K5N"); // 设置唯一的信用代码
            o.setTransMap(new HashMap<>()); // 设置transMap以避免序列化问题
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(payerInfo);
        
        // 调用
        PayerInfoDO dbPayerInfo = payerInfoMapper.selectByCreditCode("91330102MA2B0B6K5N");
        
        // 断言
        assertNotNull(dbPayerInfo);
        assertPojoEquals(payerInfo, dbPayerInfo, IGNORE_FIELDS); // 忽略特定字段比较
    }

    @Test
    public void testSelectByTaxNo() {
        // 准备参数
        PayerInfoDO payerInfo = randomPojo(PayerInfoDO.class, o -> {
            o.setId(null); // ID 需要自增
            o.setTaxNo("330106201607010002"); // 设置唯一的税号
            o.setTransMap(new HashMap<>()); // 设置transMap以避免序列化问题
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(payerInfo);
        
        // 调用
        PayerInfoDO dbPayerInfo = payerInfoMapper.selectByTaxNo("330106201607010002");
        
        // 断言
        assertNotNull(dbPayerInfo);
        assertPojoEquals(payerInfo, dbPayerInfo, IGNORE_FIELDS); // 忽略特定字段比较
    }

    @Test
    public void testSelectByPartnerPayerId() {
        // 准备参数
        PayerInfoDO payerInfo = randomPojo(PayerInfoDO.class, o -> {
            o.setId(null); // ID 需要自增
            o.setPartnerPayerId("partner123"); // 设置唯一的合作方付款方ID
            o.setTransMap(new HashMap<>()); // 设置transMap以避免序列化问题
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(payerInfo);
        
        // 调用
        PayerInfoDO dbPayerInfo = payerInfoMapper.selectByPartnerPayerId("partner123");
        
        // 断言
        assertNotNull(dbPayerInfo);
        assertPojoEquals(payerInfo, dbPayerInfo, IGNORE_FIELDS); // 忽略特定字段比较
    }

    @Test
    public void testSelectPage() {
        // mock 数据
        PayerInfoDO dbPayerInfo = randomPojo(PayerInfoDO.class, o -> { // 测试数据
            o.setId(null); // ID 需要自增
            o.setName("测试名称");
            o.setCreditCode("test-credit-code"); // 设置唯一的信用代码
            o.setTaxNo("test-tax-no"); // 设置唯一的税号
            o.setStatus(0);
            o.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28)[0]);
            o.setTransMap(new HashMap<>()); // 设置transMap以避免序列化问题
            o.setDeleted(false); // 确保deleted字段是false
        });
        payerInfoMapper.insert(dbPayerInfo);
        
        // 测试 name 不匹配
        payerInfoMapper.insert(cloneIgnoreId(dbPayerInfo, o -> {
            o.setName("不匹配");
            o.setCreditCode("test-credit-code-2");
            o.setTaxNo("test-tax-no-2");
            // 全局唯一键（#100）：cloneIgnoreId 会把原行的 payer_no / partner_payer_id 一起复制，
            // 生产不会出现两个付方共用同一个编号，这里按真实形状各生成一份。
            o.setPayerNo("PAYER_NO_PAGE_2");
            o.setPartnerPayerId("PARTNER_PAYER_PAGE_2");
        }));
        
        // 准备参数
        PayerInfoPageReqVO reqVO = new PayerInfoPageReqVO();
        reqVO.setName("测试名称");
        reqVO.setCreateTime(buildBetweenTime(2023, 2, 1, 2023, 2, 28));

        // 调用
        PageResult<PayerInfoDO> pageResult = payerInfoMapper.selectPage(reqVO);
        
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbPayerInfo, pageResult.getList().get(0), IGNORE_FIELDS); // 忽略特定字段比较
    }
} 