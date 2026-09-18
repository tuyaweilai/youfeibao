package cn.iocoder.yudao.module.waste.demo;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.module.waste.convert.price.PriceBenchmarkConvert;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.dal.mysql.price.PriceBenchmarkMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格基准管理完整功能演示
 */
@Import(PriceBenchmarkMapper.class)
public class PriceBenchmarkCompleteDemo extends BaseDbUnitTest {

    @Resource
    private PriceBenchmarkMapper priceBenchmarkMapper;

    @Test
    @Sql("classpath:sql/create-tables.sql")
    public void demonstrateCompletePriceBenchmarkFeatures() {
        System.out.println("=== 危废转移管理系统 - 价格基准管理功能演示 ===");
        
        // 1. 创建多个价格基准数据
        System.out.println("\n1. 创建价格基准数据");
        Long id1 = createPriceBenchmark("HW01", "医疗废物", "北京市", "110000", new BigDecimal("1000.00"));
        Long id2 = createPriceBenchmark("HW02", "化学废物", "北京市", "110000", new BigDecimal("1200.00"));
        Long id3 = createPriceBenchmark("HW01", "医疗废物", "上海市", "310000", new BigDecimal("1100.00"));
        Long id4 = createPriceBenchmark("HW03", "废油", "广东省", "440000", new BigDecimal("800.00"));
        
        // 2. 查询所有价格基准
        System.out.println("\n2. 查询所有价格基准");
        List<PriceBenchmarkDO> allPrices = priceBenchmarkMapper.selectList();
        System.out.printf("共有 %d 条价格基准记录:%n", allPrices.size());
        for (PriceBenchmarkDO item : allPrices) {
            System.out.printf("  ID: %d, %s - %s (%s) - %s%s%n", 
                    item.getId(), item.getWasteCode(), item.getWasteName(), 
                    item.getRegionName(), item.getPrice(), item.getPriceUnit());
        }
        
        // 3. 按废物代码查询
        System.out.println("\n3. 按废物代码查询 (HW01)");
        demonstrateQueryByWasteCode("HW01");
        
        // 4. 按地区查询
        System.out.println("\n4. 按地区查询 (北京市)");
        demonstrateQueryByRegion("110000");
        
        // 5. 智能报价功能演示
        System.out.println("\n5. 智能报价功能演示");
        demonstrateSmartQuotation("HW01", "110000");
        demonstrateSmartQuotation("HW02", "310000");
        
        // 6. 价格趋势分析
        System.out.println("\n6. 价格趋势分析");
        demonstratePriceTrendAnalysis("HW01");
        
        // 7. 更新价格基准
        System.out.println("\n7. 更新价格基准");
        updatePriceBenchmark(id1, new BigDecimal("1050.00"));
        
        // 8. 删除价格基准
        System.out.println("\n8. 删除价格基准");
        priceBenchmarkMapper.deleteById(id4);
        System.out.printf("删除价格基准 ID %d%n", id4);
        
        // 9. 最终状态查询
        System.out.println("\n9. 最终状态查询");
        List<PriceBenchmarkDO> finalPrices = priceBenchmarkMapper.selectList();
        System.out.printf("最终共有 %d 条价格基准记录%n", finalPrices.size());
        
        System.out.println("\n=== 价格基准管理功能演示完成 ===");
    }

    private Long createPriceBenchmark(String wasteCode, String wasteName, String regionName, String regionCode, BigDecimal price) {
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
        createReqVO.setRemark("演示数据");

        PriceBenchmarkDO priceBenchmark = PriceBenchmarkConvert.INSTANCE.convert(createReqVO);
        
        // 设置必要的审计字段
        priceBenchmark.setCreateTime(LocalDateTime.now());
        priceBenchmark.setUpdateTime(LocalDateTime.now());
        priceBenchmark.setStatus(1); // 生效中
        
        priceBenchmarkMapper.insert(priceBenchmark);
        System.out.printf("创建价格基准: %s - %s (%s) - %s%s, ID: %d%n", 
                wasteCode, wasteName, regionName, price, "元/吨", priceBenchmark.getId());
        
        return priceBenchmark.getId();
    }

    private void demonstrateQueryByWasteCode(String wasteCode) {
        LambdaQueryWrapper<PriceBenchmarkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceBenchmarkDO::getWasteCode, wasteCode);
        List<PriceBenchmarkDO> results = priceBenchmarkMapper.selectList(queryWrapper);
        System.out.printf("废物代码 %s 的价格基准 (%d条):%n", wasteCode, results.size());
        
        for (PriceBenchmarkDO item : results) {
            System.out.printf("  %s (%s) - %s%s%n", 
                    item.getWasteName(), item.getRegionName(), 
                    item.getPrice(), item.getPriceUnit());
        }
    }

    private void demonstrateQueryByRegion(String regionCode) {
        LambdaQueryWrapper<PriceBenchmarkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceBenchmarkDO::getRegionCode, regionCode);
        List<PriceBenchmarkDO> results = priceBenchmarkMapper.selectList(queryWrapper);
        System.out.printf("地区代码 %s 的价格基准 (%d条):%n", regionCode, results.size());
        
        for (PriceBenchmarkDO item : results) {
            System.out.printf("  %s - %s - %s%s%n", 
                    item.getWasteCode(), item.getWasteName(), 
                    item.getPrice(), item.getPriceUnit());
        }
    }

    private void demonstrateSmartQuotation(String wasteCode, String regionCode) {
        // 查找匹配的价格基准
        LambdaQueryWrapper<PriceBenchmarkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceBenchmarkDO::getWasteCode, wasteCode)
                   .eq(PriceBenchmarkDO::getRegionCode, regionCode);
        List<PriceBenchmarkDO> matches = priceBenchmarkMapper.selectList(queryWrapper);
        
        if (!matches.isEmpty()) {
            PriceBenchmarkDO benchmark = matches.get(0);
            BigDecimal basePrice = benchmark.getPrice();
            
            // 模拟智能报价算法
            BigDecimal minPrice = basePrice.multiply(new BigDecimal("0.9")); // 基准价的90%
            BigDecimal maxPrice = basePrice.multiply(new BigDecimal("1.1")); // 基准价的110%
            
            System.out.printf("智能报价 - %s (%s):%n", wasteCode, benchmark.getRegionName());
            System.out.printf("  基准价格: %s%s%n", basePrice, benchmark.getPriceUnit());
            System.out.printf("  建议价格区间: %s - %s%s%n", minPrice, maxPrice, benchmark.getPriceUnit());
            System.out.printf("  推荐报价: %s%s%n", basePrice, benchmark.getPriceUnit());
        } else {
            System.out.printf("智能报价 - %s: 未找到匹配的价格基准%n", wasteCode);
        }
    }

    private void demonstratePriceTrendAnalysis(String wasteCode) {
        LambdaQueryWrapper<PriceBenchmarkDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PriceBenchmarkDO::getWasteCode, wasteCode);
        List<PriceBenchmarkDO> prices = priceBenchmarkMapper.selectList(queryWrapper);
        
        System.out.printf("价格趋势分析 - %s:%n", wasteCode);
        
        if (prices.size() >= 2) {
            BigDecimal minPrice = prices.stream()
                    .map(PriceBenchmarkDO::getPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal maxPrice = prices.stream()
                    .map(PriceBenchmarkDO::getPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal avgPrice = prices.stream()
                    .map(PriceBenchmarkDO::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(prices.size()), 2, BigDecimal.ROUND_HALF_UP);
            
            System.out.printf("  样本数量: %d 个地区%n", prices.size());
            System.out.printf("  最低价格: %s元/吨%n", minPrice);
            System.out.printf("  最高价格: %s元/吨%n", maxPrice);
            System.out.printf("  平均价格: %s元/吨%n", avgPrice);
            System.out.printf("  价格波动: %s元/吨%n", maxPrice.subtract(minPrice));
        } else {
            System.out.printf("  样本数量不足，无法进行趋势分析%n");
        }
    }

    private void updatePriceBenchmark(Long id, BigDecimal newPrice) {
        PriceBenchmarkDO existing = priceBenchmarkMapper.selectById(id);
        if (existing != null) {
            BigDecimal oldPrice = existing.getPrice();
            existing.setPrice(newPrice);
            existing.setRemark("价格已更新");
            existing.setUpdateTime(LocalDateTime.now());
            
            priceBenchmarkMapper.updateById(existing);
            System.out.printf("更新价格基准 ID %d: 价格从 %s 更新为 %s%n", 
                    id, oldPrice, newPrice);
        }
    }
} 