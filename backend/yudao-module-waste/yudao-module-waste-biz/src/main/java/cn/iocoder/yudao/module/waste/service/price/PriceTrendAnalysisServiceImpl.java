package cn.iocoder.yudao.module.waste.service.price;

import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceTrendAnalysisReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceTrendAnalysisRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PricePredictionRespVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.PriceBenchmarkDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.price.RecyclerPriceConfigDO;
import cn.iocoder.yudao.module.waste.dal.mysql.price.PriceBenchmarkMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.price.RecyclerPriceConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 价格趋势分析 Service 实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class PriceTrendAnalysisServiceImpl implements PriceTrendAnalysisService {

    @Resource
    private PriceBenchmarkMapper priceBenchmarkMapper;
    
    @Resource
    private RecyclerPriceConfigMapper recyclerPriceConfigMapper;

    @Override
    public List<PriceTrendAnalysisRespVO> getPriceTrendAnalysis(PriceTrendAnalysisReqVO reqVO) {
        List<PriceTrendAnalysisRespVO> result = new ArrayList<>();
        
        // 根据分析类型执行不同的分析逻辑
        switch (reqVO.getAnalysisType()) {
            case 1: // 趋势分析
                if (reqVO.getWasteCode() != null) {
                    result = getPriceTrendByWasteCode(reqVO.getWasteCode(), reqVO.getStartDate(), reqVO.getEndDate());
                } else if (reqVO.getRegionCode() != null) {
                    result = getPriceTrendByRegion(reqVO.getRegionCode(), reqVO.getStartDate(), reqVO.getEndDate());
                }
                break;
            case 2: // 波动分析
                if (reqVO.getWasteCode() != null && reqVO.getRegionCode() != null) {
                    int days = (int) ChronoUnit.DAYS.between(reqVO.getStartDate(), reqVO.getEndDate());
                    PriceTrendAnalysisRespVO volatility = getPriceVolatilityAnalysis(reqVO.getWasteCode(), reqVO.getRegionCode(), days);
                    if (volatility != null) {
                        result.add(volatility);
                    }
                }
                break;
            case 3: // 对比分析
                if (reqVO.getWasteCodes() != null && !reqVO.getWasteCodes().isEmpty()) {
                    result = getPriceComparisonAnalysis(reqVO.getWasteCodes(), reqVO.getRegionCode(), 
                                                       reqVO.getStartDate(), reqVO.getEndDate());
                }
                break;
        }
        
        // 如果需要包含预测
        if (Boolean.TRUE.equals(reqVO.getIncludePrediction()) && reqVO.getPredictDays() != null) {
            for (PriceTrendAnalysisRespVO item : result) {
                PricePredictionRespVO prediction = predictPrice(item.getWasteCode(), item.getRegionCode(), reqVO.getPredictDays());
                if (prediction != null) {
                    item.setPredictedPrice(prediction.getPredictedPrice());
                    item.setConfidence(prediction.getConfidence());
                }
            }
        }
        
        return result;
    }

    @Override
    public List<PriceTrendAnalysisRespVO> getPriceTrendByWasteCode(String wasteCode, LocalDate startDate, LocalDate endDate) {
        // 查询价格基准数据
        List<PriceBenchmarkDO> priceDataList = priceBenchmarkMapper.selectPriceTrendByWasteCode(wasteCode, startDate, endDate);
        
        return priceDataList.stream().map(data -> {
            PriceTrendAnalysisRespVO vo = new PriceTrendAnalysisRespVO();
            vo.setDate(data.getEffectiveDate());
            vo.setWasteCode(wasteCode);
            vo.setWasteName(data.getWasteName());
            vo.setRegionCode(data.getRegionCode());
            vo.setRegionName(data.getRegionName());
            vo.setAvgPrice(data.getPrice());
            vo.setMaxPrice(data.getPrice());
            vo.setMinPrice(data.getPrice());
            vo.setTransactionCount(1); // 基准价格数据，设为1
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<PriceTrendAnalysisRespVO> getPriceTrendByRegion(String regionCode, LocalDate startDate, LocalDate endDate) {
        // 查询地区价格趋势数据
        List<PriceBenchmarkDO> priceDataList = priceBenchmarkMapper.selectPriceTrendByRegion(regionCode, startDate, endDate);
        
        return priceDataList.stream().map(data -> {
            PriceTrendAnalysisRespVO vo = new PriceTrendAnalysisRespVO();
            vo.setDate(data.getEffectiveDate());
            vo.setWasteCode(data.getWasteCode());
            vo.setWasteName(data.getWasteName());
            vo.setRegionCode(regionCode);
            vo.setRegionName(data.getRegionName());
            vo.setAvgPrice(data.getPrice());
            vo.setMaxPrice(data.getPrice());
            vo.setMinPrice(data.getPrice());
            vo.setTransactionCount(1); // 基准价格数据，设为1
            
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PricePredictionRespVO predictPrice(String wasteCode, String regionCode, Integer predictDays) {
        // 获取历史价格数据（最近90天）
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(90);
        
        List<PriceTrendAnalysisRespVO> historicalData = getPriceTrendByWasteCode(wasteCode, startDate, endDate);
        if (historicalData.isEmpty()) {
            return null;
        }
        
        // 简单线性回归预测（实际项目中可以使用更复杂的算法）
        List<BigDecimal> prices = historicalData.stream()
                .map(PriceTrendAnalysisRespVO::getAvgPrice)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (prices.size() < 2) {
            return null;
        }
        
        // 计算趋势斜率
        BigDecimal slope = calculateSlope(prices);
        BigDecimal currentPrice = prices.get(prices.size() - 1);
        BigDecimal predictedPrice = currentPrice.add(slope.multiply(new BigDecimal(predictDays)));
        
        // 计算置信度（基于历史数据的稳定性）
        BigDecimal volatility = calculateVolatility(prices);
        BigDecimal confidence = BigDecimal.valueOf(Math.max(50, 100 - volatility.doubleValue() * 2));
        
        PricePredictionRespVO prediction = new PricePredictionRespVO();
        prediction.setWasteCode(wasteCode);
        prediction.setRegionCode(regionCode);
        prediction.setCurrentPrice(currentPrice);
        prediction.setPredictedPrice(predictedPrice);
        prediction.setConfidence(confidence);
        prediction.setPredictDate(endDate.plusDays(predictDays));
        prediction.setPredictDays(predictDays);
        prediction.setPredictionModel("线性回归");
        
        // 计算预测变化率
        if (currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal changeRate = predictedPrice.subtract(currentPrice)
                    .divide(currentPrice, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            prediction.setPredictedChangeRate(changeRate);
        }
        
        // 设置价格区间
        BigDecimal margin = predictedPrice.multiply(new BigDecimal("0.1")); // 10%误差范围
        PricePredictionRespVO.PriceRange priceRange = new PricePredictionRespVO.PriceRange();
        priceRange.setMinPrice(predictedPrice.subtract(margin));
        priceRange.setMaxPrice(predictedPrice.add(margin));
        priceRange.setConfidenceInterval("90%");
        prediction.setPriceRange(priceRange);
        
        return prediction;
    }

    @Override
    public PriceTrendAnalysisRespVO getPriceVolatilityAnalysis(String wasteCode, String regionCode, Integer days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        
        List<PriceTrendAnalysisRespVO> data = getPriceTrendByWasteCode(wasteCode, startDate, endDate);
        if (data.isEmpty()) {
            return null;
        }
        
        List<BigDecimal> prices = data.stream()
                .map(PriceTrendAnalysisRespVO::getAvgPrice)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (prices.isEmpty()) {
            return null;
        }
        
        BigDecimal volatility = calculateVolatility(prices);
        BigDecimal avgPrice = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(prices.size()), 2, RoundingMode.HALF_UP);
        
        PriceTrendAnalysisRespVO result = new PriceTrendAnalysisRespVO();
        result.setWasteCode(wasteCode);
        result.setRegionCode(regionCode);
        result.setAvgPrice(avgPrice);
        result.setVolatility(volatility);
        result.setMaxPrice(prices.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
        result.setMinPrice(prices.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
        
        return result;
    }

    @Override
    public List<PriceTrendAnalysisRespVO> getPriceComparisonAnalysis(List<String> wasteCodes, String regionCode, 
                                                                    LocalDate startDate, LocalDate endDate) {
        List<PriceTrendAnalysisRespVO> result = new ArrayList<>();
        
        for (String wasteCode : wasteCodes) {
            List<PriceTrendAnalysisRespVO> trendData = getPriceTrendByWasteCode(wasteCode, startDate, endDate);
            if (!trendData.isEmpty()) {
                // 计算平均价格
                BigDecimal avgPrice = trendData.stream()
                        .map(PriceTrendAnalysisRespVO::getAvgPrice)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(trendData.size()), 2, RoundingMode.HALF_UP);
                
                PriceTrendAnalysisRespVO comparison = new PriceTrendAnalysisRespVO();
                comparison.setWasteCode(wasteCode);
                comparison.setRegionCode(regionCode);
                comparison.setAvgPrice(avgPrice);
                comparison.setMaxPrice(trendData.stream()
                        .map(PriceTrendAnalysisRespVO::getMaxPrice)
                        .filter(Objects::nonNull)
                        .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                comparison.setMinPrice(trendData.stream()
                        .map(PriceTrendAnalysisRespVO::getMinPrice)
                        .filter(Objects::nonNull)
                        .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO));
                
                result.add(comparison);
            }
        }
        
        return result;
    }

    @Override
    public List<PriceTrendAnalysisRespVO> getPopularWastePriceRanking(String regionCode, Integer limit) {
        // 查询热门废物价格排行
        List<PriceBenchmarkDO> rankingDataList = priceBenchmarkMapper.selectPopularWastePriceRanking(regionCode, limit);
        
        return rankingDataList.stream().map(data -> {
            PriceTrendAnalysisRespVO vo = new PriceTrendAnalysisRespVO();
            vo.setWasteCode(data.getWasteCode());
            vo.setWasteName(data.getWasteName());
            vo.setRegionCode(regionCode);
            vo.setAvgPrice(data.getPrice());
            vo.setTransactionCount(1); // 基准价格数据，设为1
            vo.setTotalVolume(BigDecimal.ONE); // 基准价格数据，设为1
            
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 计算价格趋势斜率
     */
    private BigDecimal calculateSlope(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        int n = prices.size();
        BigDecimal sumX = BigDecimal.ZERO;
        BigDecimal sumY = BigDecimal.ZERO;
        BigDecimal sumXY = BigDecimal.ZERO;
        BigDecimal sumX2 = BigDecimal.ZERO;
        
        for (int i = 0; i < n; i++) {
            BigDecimal x = new BigDecimal(i);
            BigDecimal y = prices.get(i);
            
            sumX = sumX.add(x);
            sumY = sumY.add(y);
            sumXY = sumXY.add(x.multiply(y));
            sumX2 = sumX2.add(x.multiply(x));
        }
        
        BigDecimal nBig = new BigDecimal(n);
        BigDecimal numerator = nBig.multiply(sumXY).subtract(sumX.multiply(sumY));
        BigDecimal denominator = nBig.multiply(sumX2).subtract(sumX.multiply(sumX));
        
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP);
    }

    /**
     * 计算价格波动率
     */
    private BigDecimal calculateVolatility(List<BigDecimal> prices) {
        if (prices.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        // 计算平均价格
        BigDecimal mean = prices.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(prices.size()), 6, RoundingMode.HALF_UP);
        
        // 计算方差
        BigDecimal variance = prices.stream()
                .map(price -> price.subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(prices.size()), 6, RoundingMode.HALF_UP);
        
        // 计算标准差
        double stdDev = Math.sqrt(variance.doubleValue());
        
        // 计算变异系数（标准差/平均值）作为波动率
        if (mean.compareTo(BigDecimal.ZERO) > 0) {
            return new BigDecimal(stdDev).divide(mean, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
        }
        
        return BigDecimal.ZERO;
    }
} 