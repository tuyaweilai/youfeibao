package cn.iocoder.yudao.module.waste.service.quotation;

import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 竞价比较服务接口
 *
 * @author 芋道源码
 */
public interface BiddingComparisonService {

    /**
     * 获取预约单的所有有效报价并排序
     *
     * @param appointmentId 预约单ID
     * @return 按价格排序的报价列表（从低到高）
     */
    List<AppointmentQuotationDO> getValidQuotationsSorted(Long appointmentId);

    /**
     * 获取预约单的报价统计信息
     *
     * @param appointmentId 预约单ID
     * @return 报价统计信息
     */
    Map<String, Object> getQuotationStatistics(Long appointmentId);

    /**
     * 比较多个报价的优劣
     *
     * @param appointmentId 预约单ID
     * @return 报价比较结果
     */
    Map<String, Object> compareQuotations(Long appointmentId);

    /**
     * 获取最优报价（综合考虑价格、企业信誉等因素）
     *
     * @param appointmentId 预约单ID
     * @return 最优报价
     */
    AppointmentQuotationDO getBestQuotation(Long appointmentId);

    /**
     * 获取最低价报价
     *
     * @param appointmentId 预约单ID
     * @return 最低价报价
     */
    AppointmentQuotationDO getLowestPriceQuotation(Long appointmentId);

    /**
     * 获取最高价报价
     *
     * @param appointmentId 预约单ID
     * @return 最高价报价
     */
    AppointmentQuotationDO getHighestPriceQuotation(Long appointmentId);

    /**
     * 计算报价的价格差异分析
     *
     * @param appointmentId 预约单ID
     * @return 价格差异分析结果
     */
    Map<String, Object> analyzePriceDifferences(Long appointmentId);

    /**
     * 获取报价的价格分布
     *
     * @param appointmentId 预约单ID
     * @return 价格分布信息
     */
    Map<String, Object> getPriceDistribution(Long appointmentId);

    /**
     * 检查是否有异常报价（价格过高或过低）
     *
     * @param appointmentId 预约单ID
     * @return 异常报价列表
     */
    List<AppointmentQuotationDO> getAbnormalQuotations(Long appointmentId);

    /**
     * 生成竞价报告
     *
     * @param appointmentId 预约单ID
     * @return 竞价报告
     */
    Map<String, Object> generateBiddingReport(Long appointmentId);

    /**
     * 推荐最佳报价（基于多维度评分）
     *
     * @param appointmentId 预约单ID
     * @return 推荐的报价列表（按推荐度排序）
     */
    List<AppointmentQuotationDO> getRecommendedQuotations(Long appointmentId);

    /**
     * 计算报价的综合评分
     *
     * @param quotation 报价记录
     * @return 综合评分（0-100分）
     */
    BigDecimal calculateQuotationScore(AppointmentQuotationDO quotation);

    /**
     * 检查竞价是否结束
     *
     * @param appointmentId 预约单ID
     * @return 是否结束竞价
     */
    boolean isBiddingFinished(Long appointmentId);

    /**
     * 自动选择最优报价（当竞价时间结束时）
     *
     * @param appointmentId 预约单ID
     * @return 自动选择的报价ID
     */
    Long autoSelectBestQuotation(Long appointmentId);

    /**
     * 获取竞价进度信息
     *
     * @param appointmentId 预约单ID
     * @return 竞价进度信息
     */
    Map<String, Object> getBiddingProgress(Long appointmentId);

    /**
     * 通知竞价结果给所有参与企业
     *
     * @param appointmentId 预约单ID
     * @param selectedQuotationId 选中的报价ID
     */
    void notifyBiddingResult(Long appointmentId, Long selectedQuotationId);

    /**
     * 获取企业的历史竞价表现
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 历史竞价表现统计
     */
    Map<String, Object> getEnterpriseHistoryPerformance(Long recyclingEnterpriseId);

    /**
     * 分析市场竞争激烈程度
     *
     * @param appointmentId 预约单ID
     * @return 竞争激烈程度分析
     */
    Map<String, Object> analyzeCompetitionIntensity(Long appointmentId);
} 