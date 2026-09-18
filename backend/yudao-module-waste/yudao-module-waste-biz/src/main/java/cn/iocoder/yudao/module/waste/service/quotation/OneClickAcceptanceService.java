package cn.iocoder.yudao.module.waste.service.quotation;

import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;

import java.util.Map;

/**
 * 一键接受报价服务接口
 *
 * @author 芋道源码
 */
public interface OneClickAcceptanceService {

    /**
     * 一键接受报价
     *
     * @param quotationId 报价ID
     * @param acceptedBy 接受人
     * @param acceptReason 接受原因
     * @return 处理结果
     */
    Map<String, Object> oneClickAcceptQuotation(Long quotationId, String acceptedBy, String acceptReason);

    /**
     * 一键接受最优报价
     *
     * @param appointmentId 预约单ID
     * @param acceptedBy 接受人
     * @return 处理结果
     */
    Map<String, Object> oneClickAcceptBestQuotation(Long appointmentId, String acceptedBy);

    /**
     * 一键接受最低价报价
     *
     * @param appointmentId 预约单ID
     * @param acceptedBy 接受人
     * @return 处理结果
     */
    Map<String, Object> oneClickAcceptLowestPriceQuotation(Long appointmentId, String acceptedBy);

    /**
     * 验证是否可以接受报价
     *
     * @param quotationId 报价ID
     * @return 验证结果
     */
    Map<String, Object> validateQuotationAcceptance(Long quotationId);

    /**
     * 接受报价后的后续处理
     *
     * @param quotation 已接受的报价
     * @return 处理结果
     */
    Map<String, Object> processAfterAcceptance(AppointmentQuotationDO quotation);

    /**
     * 自动生成合同
     *
     * @param quotationId 报价ID
     * @return 合同生成结果
     */
    Map<String, Object> autoGenerateContract(Long quotationId);

    /**
     * 自动创建订单
     *
     * @param quotationId 报价ID
     * @return 订单创建结果
     */
    Map<String, Object> autoCreateOrder(Long quotationId);

    /**
     * 通知相关方
     *
     * @param quotationId 报价ID
     * @param appointmentId 预约单ID
     */
    void notifyRelatedParties(Long quotationId, Long appointmentId);

    /**
     * 拒绝其他报价
     *
     * @param appointmentId 预约单ID
     * @param acceptedQuotationId 已接受的报价ID
     * @param rejectedBy 拒绝人
     */
    void rejectOtherQuotations(Long appointmentId, Long acceptedQuotationId, String rejectedBy);

    /**
     * 检查企业是否支持一键接受
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 是否支持
     */
    boolean supportsOneClickAcceptance(Long recyclingEnterpriseId);

    /**
     * 获取一键接受的配置信息
     *
     * @param recyclingEnterpriseId 回收企业ID
     * @return 配置信息
     */
    Map<String, Object> getOneClickAcceptanceConfig(Long recyclingEnterpriseId);

    /**
     * 批量接受报价（用于多个预约单）
     *
     * @param quotationIds 报价ID列表
     * @param acceptedBy 接受人
     * @return 批量处理结果
     */
    Map<String, Object> batchAcceptQuotations(java.util.List<Long> quotationIds, String acceptedBy);

    /**
     * 条件接受报价（满足特定条件时自动接受）
     *
     * @param appointmentId 预约单ID
     * @param conditions 接受条件
     * @return 处理结果
     */
    Map<String, Object> conditionalAcceptQuotation(Long appointmentId, Map<String, Object> conditions);

    /**
     * 获取接受报价的历史记录
     *
     * @param appointmentId 预约单ID
     * @return 历史记录
     */
    java.util.List<Map<String, Object>> getAcceptanceHistory(Long appointmentId);
} 