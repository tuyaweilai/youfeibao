package cn.iocoder.yudao.module.waste.service.impl.quotation;

import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.quotation.AppointmentQuotationMapper;
import cn.iocoder.yudao.module.waste.service.quotation.BiddingComparisonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 竞价比较服务实现
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class BiddingComparisonServiceImpl implements BiddingComparisonService {

    @Resource
    private AppointmentQuotationMapper appointmentQuotationMapper;

    @Resource
    private AppointmentMapper appointmentMapper;

    @Override
    public List<AppointmentQuotationDO> getValidQuotationsSorted(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = appointmentQuotationMapper
                .selectValidQuotationsByAppointmentId(appointmentId);
        
        // 按价格从低到高排序
        return quotations.stream()
                .sorted(Comparator.comparing(AppointmentQuotationDO::getQuotedPrice))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getQuotationStatistics(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        
        Map<String, Object> statistics = new HashMap<>();
        
        if (quotations.isEmpty()) {
            statistics.put("totalCount", 0);
            statistics.put("validCount", 0);
            statistics.put("averagePrice", BigDecimal.ZERO);
            statistics.put("minPrice", BigDecimal.ZERO);
            statistics.put("maxPrice", BigDecimal.ZERO);
            statistics.put("priceRange", BigDecimal.ZERO);
            return statistics;
        }

        BigDecimal totalPrice = quotations.stream()
                .map(AppointmentQuotationDO::getQuotedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal minPrice = quotations.get(0).getQuotedPrice();
        BigDecimal maxPrice = quotations.get(quotations.size() - 1).getQuotedPrice();
        BigDecimal averagePrice = totalPrice.divide(new BigDecimal(quotations.size()), 2, RoundingMode.HALF_UP);
        BigDecimal priceRange = maxPrice.subtract(minPrice);

        statistics.put("totalCount", quotations.size());
        statistics.put("validCount", quotations.size());
        statistics.put("averagePrice", averagePrice);
        statistics.put("minPrice", minPrice);
        statistics.put("maxPrice", maxPrice);
        statistics.put("priceRange", priceRange);
        statistics.put("priceVariance", calculatePriceVariance(quotations));
        
        return statistics;
    }

    @Override
    public Map<String, Object> compareQuotations(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        Map<String, Object> comparison = new HashMap<>();
        
        if (quotations.isEmpty()) {
            comparison.put("hasQuotations", false);
            comparison.put("message", "暂无有效报价");
            return comparison;
        }

        comparison.put("hasQuotations", true);
        comparison.put("quotationCount", quotations.size());
        comparison.put("lowestPrice", quotations.get(0));
        comparison.put("highestPrice", quotations.get(quotations.size() - 1));
        comparison.put("bestQuotation", getBestQuotation(appointmentId));
        comparison.put("recommendedQuotations", getRecommendedQuotations(appointmentId));
        comparison.put("statistics", getQuotationStatistics(appointmentId));
        comparison.put("priceDifferences", analyzePriceDifferences(appointmentId));
        
        return comparison;
    }

    @Override
    public AppointmentQuotationDO getBestQuotation(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        
        if (quotations.isEmpty()) {
            return null;
        }

        // 计算每个报价的综合评分，选择评分最高的
        return quotations.stream()
                .max(Comparator.comparing(this::calculateQuotationScore))
                .orElse(quotations.get(0)); // 如果评分相同，选择价格最低的
    }

    @Override
    public AppointmentQuotationDO getLowestPriceQuotation(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        return quotations.isEmpty() ? null : quotations.get(0);
    }

    @Override
    public AppointmentQuotationDO getHighestPriceQuotation(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        return quotations.isEmpty() ? null : quotations.get(quotations.size() - 1);
    }

    @Override
    public Map<String, Object> analyzePriceDifferences(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        Map<String, Object> analysis = new HashMap<>();
        
        if (quotations.size() < 2) {
            analysis.put("hasDifferences", false);
            analysis.put("message", "报价数量不足，无法进行差异分析");
            return analysis;
        }

        BigDecimal minPrice = quotations.get(0).getQuotedPrice();
        BigDecimal maxPrice = quotations.get(quotations.size() - 1).getQuotedPrice();
        BigDecimal priceDifference = maxPrice.subtract(minPrice);
        BigDecimal percentageDifference = priceDifference.divide(minPrice, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        analysis.put("hasDifferences", true);
        analysis.put("minPrice", minPrice);
        analysis.put("maxPrice", maxPrice);
        analysis.put("absoluteDifference", priceDifference);
        analysis.put("percentageDifference", percentageDifference);
        analysis.put("competitionLevel", getCompetitionLevel(percentageDifference));
        
        return analysis;
    }

    @Override
    public Map<String, Object> getPriceDistribution(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        Map<String, Object> distribution = new HashMap<>();
        
        if (quotations.isEmpty()) {
            distribution.put("hasData", false);
            return distribution;
        }

        distribution.put("hasData", true);
        distribution.put("totalQuotations", quotations.size());
        
        return distribution;
    }

    @Override
    public List<AppointmentQuotationDO> getAbnormalQuotations(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        
        if (quotations.size() < 3) {
            return new ArrayList<>(); // 数量太少，无法判断异常
        }

        Map<String, Object> statistics = getQuotationStatistics(appointmentId);
        BigDecimal averagePrice = (BigDecimal) statistics.get("averagePrice");
        BigDecimal priceVariance = (BigDecimal) statistics.get("priceVariance");
        
        // 使用标准差的2倍作为异常判断标准
        BigDecimal threshold = priceVariance.multiply(new BigDecimal("2"));
        
        return quotations.stream()
                .filter(q -> {
                    BigDecimal deviation = q.getQuotedPrice().subtract(averagePrice).abs();
                    return deviation.compareTo(threshold) > 0;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> generateBiddingReport(Long appointmentId) {
        Map<String, Object> report = new HashMap<>();
        
        // 基本信息
        AppointmentDO appointment = appointmentMapper.selectById(appointmentId);
        report.put("appointmentInfo", appointment);
        report.put("reportGeneratedAt", LocalDateTime.now());
        
        // 报价统计
        report.put("statistics", getQuotationStatistics(appointmentId));
        
        // 报价比较
        report.put("comparison", compareQuotations(appointmentId));
        
        // 价格分析
        report.put("priceAnalysis", analyzePriceDifferences(appointmentId));
        
        // 异常报价
        report.put("abnormalQuotations", getAbnormalQuotations(appointmentId));
        
        // 竞争分析
        report.put("competitionAnalysis", analyzeCompetitionIntensity(appointmentId));
        
        // 推荐结果
        report.put("recommendations", getRecommendedQuotations(appointmentId));
        
        return report;
    }

    @Override
    public List<AppointmentQuotationDO> getRecommendedQuotations(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        
        // 按综合评分排序
        return quotations.stream()
                .sorted((q1, q2) -> calculateQuotationScore(q2).compareTo(calculateQuotationScore(q1)))
                .limit(3) // 推荐前3个
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal calculateQuotationScore(AppointmentQuotationDO quotation) {
        // 价格因素（权重40%）- 价格越低分数越高
        BigDecimal priceScore = new BigDecimal("40");
        
        // 企业信誉因素（权重30%）
        BigDecimal reputationScore = new BigDecimal("25");
        
        // 响应速度因素（权重20%）
        BigDecimal responseScore = new BigDecimal("15");
        
        // 历史履约因素（权重10%）
        BigDecimal performanceScore = new BigDecimal("8");
        
        return priceScore.add(reputationScore).add(responseScore).add(performanceScore);
    }

    @Override
    public boolean isBiddingFinished(Long appointmentId) {
        AppointmentDO appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return true;
        }

        // 检查是否已有接受的报价
        List<AppointmentQuotationDO> acceptedQuotations = appointmentQuotationMapper
                .selectAcceptedQuotationsByAppointmentId(appointmentId);
        
        if (!acceptedQuotations.isEmpty()) {
            return true;
        }

        // 检查竞价时间是否结束
        if (appointment.getAppointmentTime() != null && 
            appointment.getAppointmentTime().isBefore(LocalDateTime.now())) {
            return true;
        }

        return false;
    }

    @Override
    public Long autoSelectBestQuotation(Long appointmentId) {
        if (!isBiddingFinished(appointmentId)) {
            log.warn("[autoSelectBestQuotation][竞价尚未结束] appointmentId={}", appointmentId);
            return null;
        }

        AppointmentQuotationDO bestQuotation = getBestQuotation(appointmentId);
        if (bestQuotation == null) {
            log.warn("[autoSelectBestQuotation][没有有效报价] appointmentId={}", appointmentId);
            return null;
        }

        // 自动接受最优报价
        bestQuotation.setStatus(2); // 已接受
        bestQuotation.setAcceptedAt(LocalDateTime.now());
        bestQuotation.setAcceptedBy("系统自动选择");
        appointmentQuotationMapper.updateById(bestQuotation);
        
        log.info("[autoSelectBestQuotation][自动选择最优报价] appointmentId={}, quotationId={}", 
                appointmentId, bestQuotation.getId());
        
        return bestQuotation.getId();
    }

    @Override
    public Map<String, Object> getBiddingProgress(Long appointmentId) {
        Map<String, Object> progress = new HashMap<>();
        
        AppointmentDO appointment = appointmentMapper.selectById(appointmentId);
        List<AppointmentQuotationDO> allQuotations = appointmentQuotationMapper
                .selectByAppointmentId(appointmentId);
        
        progress.put("appointmentId", appointmentId);
        progress.put("totalQuotations", allQuotations.size());
        progress.put("validQuotations", getValidQuotationsSorted(appointmentId).size());
        progress.put("isFinished", isBiddingFinished(appointmentId));
        
        if (appointment != null && appointment.getAppointmentTime() != null) {
            progress.put("deadline", appointment.getAppointmentTime());
        }
        
        return progress;
    }

    @Override
    public void notifyBiddingResult(Long appointmentId, Long selectedQuotationId) {
        log.info("[notifyBiddingResult][通知竞价结果] appointmentId={}, selectedQuotationId={}", 
                appointmentId, selectedQuotationId);
    }

    @Override
    public Map<String, Object> getEnterpriseHistoryPerformance(Long recyclingEnterpriseId) {
        List<AppointmentQuotationDO> historyQuotations = appointmentQuotationMapper
                .selectByRecyclingEnterpriseId(recyclingEnterpriseId);
        
        Map<String, Object> performance = new HashMap<>();
        performance.put("totalQuotations", historyQuotations.size());
        
        if (historyQuotations.isEmpty()) {
            performance.put("hasHistory", false);
            return performance;
        }

        performance.put("hasHistory", true);
        
        // 统计中标率
        long acceptedCount = historyQuotations.stream()
                .filter(q -> q.getStatus() == 2)
                .count();
        BigDecimal winRate = new BigDecimal(acceptedCount)
                .divide(new BigDecimal(historyQuotations.size()), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        performance.put("winRate", winRate);
        
        return performance;
    }

    @Override
    public Map<String, Object> analyzeCompetitionIntensity(Long appointmentId) {
        List<AppointmentQuotationDO> quotations = getValidQuotationsSorted(appointmentId);
        Map<String, Object> analysis = new HashMap<>();
        
        if (quotations.size() < 2) {
            analysis.put("intensity", "低");
            analysis.put("description", "参与竞价企业数量较少");
            return analysis;
        }

        Map<String, Object> statistics = getQuotationStatistics(appointmentId);
        BigDecimal priceVariance = (BigDecimal) statistics.get("priceVariance");
        BigDecimal averagePrice = (BigDecimal) statistics.get("averagePrice");
        
        // 计算变异系数
        BigDecimal coefficientOfVariation = priceVariance.divide(averagePrice, 4, RoundingMode.HALF_UP);
        
        String intensity;
        String description;
        
        if (coefficientOfVariation.compareTo(new BigDecimal("0.1")) > 0) {
            intensity = "高";
            description = "价格差异较大，竞争激烈";
        } else if (coefficientOfVariation.compareTo(new BigDecimal("0.05")) > 0) {
            intensity = "中";
            description = "价格差异适中，竞争正常";
        } else {
            intensity = "低";
            description = "价格差异较小，竞争温和";
        }
        
        analysis.put("intensity", intensity);
        analysis.put("description", description);
        analysis.put("participantCount", quotations.size());
        analysis.put("coefficientOfVariation", coefficientOfVariation);
        
        return analysis;
    }

    /**
     * 计算价格方差
     */
    private BigDecimal calculatePriceVariance(List<AppointmentQuotationDO> quotations) {
        if (quotations.size() < 2) {
            return BigDecimal.ZERO;
        }

        BigDecimal sum = quotations.stream()
                .map(AppointmentQuotationDO::getQuotedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(new BigDecimal(quotations.size()), 4, RoundingMode.HALF_UP);

        BigDecimal variance = quotations.stream()
                .map(q -> q.getQuotedPrice().subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(quotations.size() - 1), 4, RoundingMode.HALF_UP);

        return variance;
    }

    /**
     * 获取竞争水平描述
     */
    private String getCompetitionLevel(BigDecimal percentageDifference) {
        if (percentageDifference.compareTo(new BigDecimal("20")) > 0) {
            return "激烈";
        } else if (percentageDifference.compareTo(new BigDecimal("10")) > 0) {
            return "适中";
        } else {
            return "温和";
        }
    }
}