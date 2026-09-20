package cn.iocoder.yudao.module.waste.demo;

import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceBenchmarkCreateReqVO;
import cn.iocoder.yudao.module.waste.convert.price.PriceBenchmarkConvert;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格基准转换器测试
 */
public class PriceBenchmarkSimpleTest {

    @Test
    public void testConvert() {
        System.out.println("=== 测试价格基准转换器 ===");
        
        // 创建请求VO
        PriceBenchmarkCreateReqVO createReqVO = new PriceBenchmarkCreateReqVO();
        createReqVO.setWasteCode("HW01");
        createReqVO.setWasteName("医疗废物");
        createReqVO.setBenchmarkPrice(new BigDecimal("1000.00"));
        createReqVO.setPriceUnit("元/吨");
        createReqVO.setRegionCode("110000");
        createReqVO.setRegionName("北京市");
        createReqVO.setEffectiveDate(LocalDate.now());
        createReqVO.setExpiryDate(LocalDate.now().plusDays(365));
        createReqVO.setSource("市场调研");
        createReqVO.setRemark("测试数据");
        
        System.out.println("原始VO数据:");
        System.out.printf("  废物代码: %s%n", createReqVO.getWasteCode());
        System.out.printf("  废物名称: %s%n", createReqVO.getWasteName());
        System.out.printf("  基准价格: %s%n", createReqVO.getBenchmarkPrice());
        System.out.printf("  价格单位: %s%n", createReqVO.getPriceUnit());
        System.out.printf("  地区代码: %s%n", createReqVO.getRegionCode());
        System.out.printf("  地区名称: %s%n", createReqVO.getRegionName());
        System.out.printf("  生效日期: %s%n", createReqVO.getEffectiveDate());
        System.out.printf("  失效日期: %s%n", createReqVO.getExpiryDate());
        System.out.printf("  价格来源: %s%n", createReqVO.getSource());
        System.out.printf("  备注: %s%n", createReqVO.getRemark());
        
        // 转换为DO
        PriceBenchmarkDO priceBenchmarkDO = PriceBenchmarkConvert.INSTANCE.convert(createReqVO);
        
        System.out.println("\n转换后的DO数据:");
        System.out.printf("  废物代码: %s%n", priceBenchmarkDO.getWasteCode());
        System.out.printf("  废物名称: %s%n", priceBenchmarkDO.getWasteName());
        System.out.printf("  价格: %s%n", priceBenchmarkDO.getPrice());
        System.out.printf("  价格单位: %s%n", priceBenchmarkDO.getPriceUnit());
        System.out.printf("  地区代码: %s%n", priceBenchmarkDO.getRegionCode());
        System.out.printf("  地区名称: %s%n", priceBenchmarkDO.getRegionName());
        System.out.printf("  生效日期: %s%n", priceBenchmarkDO.getEffectiveDate());
        System.out.printf("  失效日期: %s%n", priceBenchmarkDO.getExpireDate());
        System.out.printf("  价格来源: %s%n", priceBenchmarkDO.getPriceSource());
        System.out.printf("  备注: %s%n", priceBenchmarkDO.getRemark());
        
        // 验证转换是否正确
        assert priceBenchmarkDO.getPrice() != null : "价格字段不能为空";
        assert priceBenchmarkDO.getPrice().equals(createReqVO.getBenchmarkPrice()) : "价格转换不正确";
        
        System.out.println("\n=== 转换器测试通过 ===");
    }
} 