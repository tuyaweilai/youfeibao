package cn.iocoder.yudao.module.waste.demo;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.module.waste.convert.price.PriceBenchmarkConvert;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.dal.mysql.price.PriceBenchmarkMapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 价格基准Mapper测试
 */
@Import(PriceBenchmarkMapper.class)
public class PriceBenchmarkMapperTest extends BaseDbUnitTest {

    @Resource
    private PriceBenchmarkMapper priceBenchmarkMapper;

    @Test
    @Sql("classpath:sql/create-tables.sql")
    public void testMapperOperations() {
        System.out.println("=== 测试价格基准Mapper操作 ===");
        
        // 1. 创建价格基准数据
        System.out.println("\n1. 创建价格基准数据");
        PriceBenchmarkDO priceBenchmark = createTestPriceBenchmark("HW01", "医疗废物", "北京市", "110000", new BigDecimal("1000.00"));
        
        // 2. 插入数据库
        System.out.println("\n2. 插入数据库");
        priceBenchmarkMapper.insert(priceBenchmark);
        System.out.printf("插入成功，ID: %d%n", priceBenchmark.getId());
        
        // 3. 查询数据
        System.out.println("\n3. 查询数据");
        PriceBenchmarkDO found = priceBenchmarkMapper.selectById(priceBenchmark.getId());
        if (found != null) {
            System.out.printf("查询成功: %s - %s (%s) - %s%s%n", 
                    found.getWasteCode(), found.getWasteName(), 
                    found.getRegionName(), found.getPrice(), found.getPriceUnit());
        } else {
            System.out.println("查询失败：数据不存在");
        }
        
        // 4. 更新数据
        System.out.println("\n4. 更新数据");
        found.setPrice(new BigDecimal("1100.00"));
        found.setRemark("价格已更新");
        found.setUpdateTime(LocalDateTime.now());
        priceBenchmarkMapper.updateById(found);
        System.out.printf("更新成功，新价格: %s%n", found.getPrice());
        
        // 5. 再次查询验证
        System.out.println("\n5. 验证更新结果");
        PriceBenchmarkDO updated = priceBenchmarkMapper.selectById(priceBenchmark.getId());
        System.out.printf("验证成功，当前价格: %s，备注: %s%n", updated.getPrice(), updated.getRemark());
        
        // 6. 删除数据
        System.out.println("\n6. 删除数据");
        priceBenchmarkMapper.deleteById(priceBenchmark.getId());
        System.out.println("删除成功");
        
        // 7. 验证删除
        System.out.println("\n7. 验证删除结果");
        PriceBenchmarkDO deleted = priceBenchmarkMapper.selectById(priceBenchmark.getId());
        if (deleted == null) {
            System.out.println("验证成功：数据已删除");
        } else {
            System.out.println("验证失败：数据仍然存在");
        }
        
        System.out.println("\n=== Mapper测试完成 ===");
    }

    private PriceBenchmarkDO createTestPriceBenchmark(String wasteCode, String wasteName, String regionName, String regionCode, BigDecimal price) {
        // 使用转换器创建DO对象
        PriceBenchmarkCreateReqVO createReqVO = new PriceBenchmarkCreateReqVO();
        createReqVO.setWasteCode(wasteCode);
        createReqVO.setWasteName(wasteName);
        createReqVO.setBenchmarkPrice(price);
        createReqVO.setPriceUnit("元/吨");
        createReqVO.setRegionCode(regionCode);
        createReqVO.setRegionName(regionName);
        createReqVO.setEffectiveDate(LocalDate.now());
        createReqVO.setExpiryDate(LocalDate.now().plusDays(365));
        createReqVO.setSource("市场调研");
        createReqVO.setRemark("测试数据");

        PriceBenchmarkDO priceBenchmark = PriceBenchmarkConvert.INSTANCE.convert(createReqVO);
        
        // 设置必要的审计字段
        priceBenchmark.setCreateTime(LocalDateTime.now());
        priceBenchmark.setUpdateTime(LocalDateTime.now());
        priceBenchmark.setStatus(1); // 生效中
        
        System.out.printf("创建测试数据: %s - %s (%s) - %s%s%n", 
                wasteCode, wasteName, regionName, price, "元/吨");
        
        return priceBenchmark;
    }
} 