package cn.iocoder.yudao.module.waste.service.price;

import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceTrendAnalysisReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PriceTrendAnalysisRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.price.vo.PricePredictionRespVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 价格趋势分析 Service 接口
 *
 * @author 芋道源码
 */
public interface PriceTrendAnalysisService {

    /**
     * 获取价格趋势分析
     *
     * @param reqVO 分析请求参数
     * @return 价格趋势分析结果
     */
    List<PriceTrendAnalysisRespVO> getPriceTrendAnalysis(PriceTrendAnalysisReqVO reqVO);

    /**
     * 获取废物代码的价格趋势
     *
     * @param wasteCode 废物代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格趋势数据
     */
    List<PriceTrendAnalysisRespVO> getPriceTrendByWasteCode(String wasteCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取地区价格趋势
     *
     * @param regionCode 地区代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格趋势数据
     */
    List<PriceTrendAnalysisRespVO> getPriceTrendByRegion(String regionCode, LocalDate startDate, LocalDate endDate);

    /**
     * 价格预测
     *
     * @param wasteCode 废物代码
     * @param regionCode 地区代码
     * @param predictDays 预测天数
     * @return 价格预测结果
     */
    PricePredictionRespVO predictPrice(String wasteCode, String regionCode, Integer predictDays);

    /**
     * 获取价格波动分析
     *
     * @param wasteCode 废物代码
     * @param regionCode 地区代码
     * @param days 分析天数
     * @return 价格波动分析
     */
    PriceTrendAnalysisRespVO getPriceVolatilityAnalysis(String wasteCode, String regionCode, Integer days);

    /**
     * 获取价格对比分析
     *
     * @param wasteCodes 废物代码列表
     * @param regionCode 地区代码
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 价格对比分析
     */
    List<PriceTrendAnalysisRespVO> getPriceComparisonAnalysis(List<String> wasteCodes, String regionCode, 
                                                              LocalDate startDate, LocalDate endDate);

    /**
     * 获取热门废物价格排行
     *
     * @param regionCode 地区代码
     * @param limit 排行数量
     * @return 价格排行
     */
    List<PriceTrendAnalysisRespVO> getPopularWastePriceRanking(String regionCode, Integer limit);
} 