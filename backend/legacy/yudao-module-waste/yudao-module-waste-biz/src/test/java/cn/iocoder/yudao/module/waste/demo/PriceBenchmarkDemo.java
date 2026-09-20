package cn.iocoder.yudao.module.waste.demo;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkPageReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.dal.mysql.price.PriceBenchmarkMapper;
import cn.iocoder.yudao.module.waste.service.impl.price.PriceBenchmarkServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格基准管理功能演示
 *
 * @author 系统
 */
@Import(PriceBenchmarkServiceImpl.class)
public class PriceBenchmarkDemo extends BaseDbUnitTest {

    @Resource
    private PriceBenchmarkServiceImpl priceBenchmarkService;

    @Resource
    private PriceBenchmarkMapper priceBenchmarkMapper;

    @Test
    @Sql("classpath:sql/create-tables.sql")
    public void demonstratePriceBenchmarkFeatures() {
        System.out.println("=== 危废转移管理系统 - 价格基准管理功能演示 ===");
        
        // 1. 创建价格基准数据
        System.out.println("\n1. 创建价格基准数据");
        Long benchmarkId1 = createPriceBenchmark("HW01", "医疗废物", "北京市", "110000", new BigDecimal("1000.00"));
        Long benchmarkId2 = createPriceBenchmark("HW02", "化学废物", "上海市", "310000", new BigDecimal("1200.00"));
        Long benchmarkId3 = createPriceBenchmark("HW01", "医疗废物", "广东省", "440000", new BigDecimal("950.00"));
        
        // 2. 查询价格基准列表
        System.out.println("\n2. 查询价格基准列表");
        queryPriceBenchmarkList();
        
        // 3. 按废物代码查询
        System.out.println("\n3. 按废物代码查询价格基准");
        queryByWasteCode("HW01");
        
        // 4. 按地区查询
        System.out.println("\n4. 按地区查询价格基准");
        queryByRegion("110000");
        
        // 5. 更新价格基准
        System.out.println("\n5. 更新价格基准");
        updatePriceBenchmark(benchmarkId1, new BigDecimal("1100.00"));
        
        // 6. 查看更新后的数据
        System.out.println("\n6. 查看更新后的数据");
        viewPriceBenchmark(benchmarkId1);
        
        // 7. 删除价格基准
        System.out.println("\n7. 删除价格基准");
        deletePriceBenchmark(benchmarkId3);
        
        // 8. 最终数据状态
        System.out.println("\n8. 最终数据状态");
        queryPriceBenchmarkList();
        
        System.out.println("\n=== 演示完成 ===");
    }

    private Long createPriceBenchmark(String wasteCode, String wasteName, String regionName, String regionCode, BigDecimal price) {
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

        Long id = priceBenchmarkService.createPriceBenchmark(createReqVO);
        System.out.printf("创建价格基准: %s - %s (%s) - %s元/吨, ID: %d%n", 
                wasteCode, wasteName, regionName, price, id);
        return id;
    }

    private void queryPriceBenchmarkList() {
        PriceBenchmarkPageReqVO pageReqVO = new PriceBenchmarkPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        
        PageResult<PriceBenchmarkRespVO> pageResult = priceBenchmarkService.getPriceBenchmarkPage(pageReqVO);
        System.out.printf("查询到 %d 条价格基准记录:%n", pageResult.getTotal());
        
        for (PriceBenchmarkRespVO item : pageResult.getList()) {
            System.out.printf("  ID: %d, %s - %s (%s) - %s%s%n", 
                    item.getId(), item.getWasteCode(), item.getWasteName(), 
                    item.getRegionName(), item.getBenchmarkPrice(), item.getPriceUnit());
        }
    }

    private void queryByWasteCode(String wasteCode) {
        PriceBenchmarkPageReqVO pageReqVO = new PriceBenchmarkPageReqVO();
        pageReqVO.setWasteCode(wasteCode);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        
        PageResult<PriceBenchmarkRespVO> pageResult = priceBenchmarkService.getPriceBenchmarkPage(pageReqVO);
        System.out.printf("废物代码 %s 的价格基准 (%d条):%n", wasteCode, pageResult.getTotal());
        
        for (PriceBenchmarkRespVO item : pageResult.getList()) {
            System.out.printf("  %s (%s) - %s%s%n", 
                    item.getWasteName(), item.getRegionName(), 
                    item.getBenchmarkPrice(), item.getPriceUnit());
        }
    }

    private void queryByRegion(String regionCode) {
        PriceBenchmarkPageReqVO pageReqVO = new PriceBenchmarkPageReqVO();
        pageReqVO.setRegionCode(regionCode);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        
        PageResult<PriceBenchmarkRespVO> pageResult = priceBenchmarkService.getPriceBenchmarkPage(pageReqVO);
        System.out.printf("地区代码 %s 的价格基准 (%d条):%n", regionCode, pageResult.getTotal());
        
        for (PriceBenchmarkRespVO item : pageResult.getList()) {
            System.out.printf("  %s - %s - %s%s%n", 
                    item.getWasteCode(), item.getWasteName(), 
                    item.getBenchmarkPrice(), item.getPriceUnit());
        }
    }

    private void updatePriceBenchmark(Long id, BigDecimal newPrice) {
        PriceBenchmarkDO existing = priceBenchmarkService.getPriceBenchmark(id);
        if (existing == null) {
            System.out.printf("价格基准 ID %d 不存在%n", id);
            return;
        }

        PriceBenchmarkUpdateReqVO updateReqVO = new PriceBenchmarkUpdateReqVO();
        updateReqVO.setId(id);
        updateReqVO.setWasteCode(existing.getWasteCode());
        updateReqVO.setWasteName(existing.getWasteName());
        updateReqVO.setBenchmarkPrice(newPrice);
        updateReqVO.setPriceUnit(existing.getPriceUnit());
        updateReqVO.setRegionCode(existing.getRegionCode());
        updateReqVO.setRegionName(existing.getRegionName());
        updateReqVO.setEffectiveDate(existing.getEffectiveDate());
        updateReqVO.setExpiryDate(existing.getExpireDate());
        updateReqVO.setSource(existing.getPriceSource());
        updateReqVO.setRemark("价格已更新");

        priceBenchmarkService.updatePriceBenchmark(updateReqVO);
        System.out.printf("更新价格基准 ID %d: 价格从 %s 更新为 %s%n", 
                id, existing.getPrice(), newPrice);
    }

    private void viewPriceBenchmark(Long id) {
        PriceBenchmarkDO benchmark = priceBenchmarkService.getPriceBenchmark(id);
        if (benchmark == null) {
            System.out.printf("价格基准 ID %d 不存在%n", id);
            return;
        }

        System.out.printf("价格基准详情 (ID: %d):%n", id);
        System.out.printf("  废物代码: %s%n", benchmark.getWasteCode());
        System.out.printf("  废物名称: %s%n", benchmark.getWasteName());
        System.out.printf("  适用地区: %s (%s)%n", benchmark.getRegionName(), benchmark.getRegionCode());
        System.out.printf("  基准价格: %s %s%n", benchmark.getPrice(), benchmark.getPriceUnit());
        System.out.printf("  生效日期: %s%n", benchmark.getEffectiveDate());
        System.out.printf("  失效日期: %s%n", benchmark.getExpireDate());
        System.out.printf("  价格来源: %s%n", benchmark.getPriceSource());
        System.out.printf("  备注: %s%n", benchmark.getRemark());
    }

    private void deletePriceBenchmark(Long id) {
        PriceBenchmarkDO existing = priceBenchmarkService.getPriceBenchmark(id);
        if (existing == null) {
            System.out.printf("价格基准 ID %d 不存在%n", id);
            return;
        }

        priceBenchmarkService.deletePriceBenchmark(id);
        System.out.printf("删除价格基准: %s - %s (%s), ID: %d%n", 
                existing.getWasteCode(), existing.getWasteName(), 
                existing.getRegionName(), id);
    }
} 