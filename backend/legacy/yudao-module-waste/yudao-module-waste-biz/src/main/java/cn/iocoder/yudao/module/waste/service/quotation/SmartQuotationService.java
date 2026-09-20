package cn.iocoder.yudao.module.waste.service.quotation;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能报价生成服务接口
 *
 * @author 芋道源码
 */
public interface SmartQuotationService {

    /**
     * 基于价格策略生成智能报价
     *
     * @param appointmentId 预约单ID
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @param quantity 数量
     * @param customerEnterpriseId 客户企业ID
     * @param region 地区
     * @return 计算出的报价金额
     */
    BigDecimal generateSmartQuotation(Long appointmentId, Long recyclingEnterpriseId, 
                                     String wasteCode, BigDecimal quantity, 
                                     Long customerEnterpriseId, String region);

    /**
     * 应用价格策略计算报价
     * 优先级：客户专属价格 > 区域价格 > 基础价格
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @param quantity 数量
     * @param customerEnterpriseId 客户企业ID
     * @param region 地区
     * @return 计算出的单价
     */
    BigDecimal calculatePriceByStrategy(Long recyclingEnterpriseId, String wasteCode, 
                                       BigDecimal quantity, Long customerEnterpriseId, String region);

    /**
     * 获取客户专属价格
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param customerEnterpriseId 客户企业ID
     * @param wasteCode 废物代码
     * @return 客户专属价格，如果不存在返回null
     */
    BigDecimal getCustomerSpecificPrice(Long recyclingEnterpriseId, Long customerEnterpriseId, String wasteCode);

    /**
     * 获取区域价格
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @param region 地区
     * @return 区域价格，如果不存在返回null
     */
    BigDecimal getRegionalPrice(Long recyclingEnterpriseId, String wasteCode, String region);

    /**
     * 获取基础价格
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @return 基础价格，如果不存在返回null
     */
    BigDecimal getBasePrice(Long recyclingEnterpriseId, String wasteCode);

    /**
     * 批量生成智能报价
     *
     * @param appointmentId 预约单ID
     * @param recyclingEnterpriseIds 回收企业ID列表
     * @param wasteCode 废物代码
     * @param quantity 数量
     * @param customerEnterpriseId 客户企业ID
     * @param region 地区
     * @return 报价记录ID列表
     */
    List<Long> batchGenerateSmartQuotations(Long appointmentId, List<Long> recyclingEnterpriseIds,
                                           String wasteCode, BigDecimal quantity,
                                           Long customerEnterpriseId, String region);

    /**
     * 根据业务模式自动生成报价
     *
     * @param appointmentId 预约单ID
     * @param wasteCode 废物代码
     * @param quantity 数量
     * @param customerEnterpriseId 客户企业ID
     * @param region 地区
     * @return 生成的报价记录数量
     */
    int autoGenerateQuotationsByBusinessMode(Long appointmentId, String wasteCode, 
                                           BigDecimal quantity, Long customerEnterpriseId, String region);

    /**
     * 价格调整
     *
     * @param quotationId 报价ID
     * @param adjustmentFactor 调整系数（如0.95表示95折）
     * @param adjustmentReason 调整原因
     * @return 调整后的价格
     */
    BigDecimal adjustQuotationPrice(Long quotationId, BigDecimal adjustmentFactor, String adjustmentReason);

    /**
     * 验证报价合理性
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @param wasteCode 废物代码
     * @param quotedPrice 报价
     * @param quantity 数量
     * @return 是否合理
     */
    boolean validateQuotationReasonableness(Long recyclingEnterpriseId, String wasteCode, 
                                          BigDecimal quotedPrice, BigDecimal quantity);
} 