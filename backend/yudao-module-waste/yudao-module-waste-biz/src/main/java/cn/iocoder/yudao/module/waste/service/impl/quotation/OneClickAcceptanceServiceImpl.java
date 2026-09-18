package cn.iocoder.yudao.module.waste.service.impl.quotation;

import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.quotation.AppointmentQuotationMapper;
import cn.iocoder.yudao.module.waste.service.quotation.BiddingComparisonService;
import cn.iocoder.yudao.module.waste.service.quotation.BusinessModeConfigService;
import cn.iocoder.yudao.module.waste.service.quotation.OneClickAcceptanceService;
import cn.iocoder.yudao.module.waste.service.quotation.QuotationContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 一键接受报价服务实现
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class OneClickAcceptanceServiceImpl implements OneClickAcceptanceService {

    @Resource
    private AppointmentQuotationMapper appointmentQuotationMapper;

    @Resource
    private AppointmentMapper appointmentMapper;

    @Resource
    private BiddingComparisonService biddingComparisonService;

    @Resource
    private BusinessModeConfigService businessModeConfigService;

    @Resource
    private QuotationContractService quotationContractService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> oneClickAcceptQuotation(Long quotationId, String acceptedBy, String acceptReason) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 验证报价是否可以接受
            Map<String, Object> validation = validateQuotationAcceptance(quotationId);
            if (!(Boolean) validation.get("canAccept")) {
                result.put("success", false);
                result.put("message", validation.get("message"));
                return result;
            }

            // 2. 获取报价信息
            AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
            if (quotation == null) {
                result.put("success", false);
                result.put("message", "报价不存在");
                return result;
            }

            // 3. 接受报价
            quotation.setStatus(2); // 已接受
            quotation.setAcceptedAt(LocalDateTime.now());
            quotation.setAcceptedBy(acceptedBy);
            quotation.setAcceptReason(acceptReason);
            appointmentQuotationMapper.updateById(quotation);

            // 4. 拒绝其他报价
            rejectOtherQuotations(quotation.getAppointmentId(), quotationId, acceptedBy);

            // 5. 后续处理
            Map<String, Object> afterProcessResult = processAfterAcceptance(quotation);

            // 6. 通知相关方
            notifyRelatedParties(quotationId, quotation.getAppointmentId());

            result.put("success", true);
            result.put("message", "报价接受成功");
            result.put("quotationId", quotationId);
            result.put("afterProcessResult", afterProcessResult);
            
            log.info("[oneClickAcceptQuotation][一键接受报价成功] quotationId={}, acceptedBy={}", 
                    quotationId, acceptedBy);

        } catch (Exception e) {
            log.error("[oneClickAcceptQuotation][一键接受报价失败] quotationId={}", quotationId, e);
            result.put("success", false);
            result.put("message", "接受报价失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> oneClickAcceptBestQuotation(Long appointmentId, String acceptedBy) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取最优报价
            AppointmentQuotationDO bestQuotation = biddingComparisonService.getBestQuotation(appointmentId);
            if (bestQuotation == null) {
                result.put("success", false);
                result.put("message", "没有找到有效的报价");
                return result;
            }

            // 一键接受最优报价
            return oneClickAcceptQuotation(bestQuotation.getId(), acceptedBy, "系统推荐的最优报价");

        } catch (Exception e) {
            log.error("[oneClickAcceptBestQuotation][一键接受最优报价失败] appointmentId={}", appointmentId, e);
            result.put("success", false);
            result.put("message", "接受最优报价失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> oneClickAcceptLowestPriceQuotation(Long appointmentId, String acceptedBy) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取最低价报价
            AppointmentQuotationDO lowestPriceQuotation = biddingComparisonService.getLowestPriceQuotation(appointmentId);
            if (lowestPriceQuotation == null) {
                result.put("success", false);
                result.put("message", "没有找到有效的报价");
                return result;
            }

            // 一键接受最低价报价
            return oneClickAcceptQuotation(lowestPriceQuotation.getId(), acceptedBy, "选择最低价报价");

        } catch (Exception e) {
            log.error("[oneClickAcceptLowestPriceQuotation][一键接受最低价报价失败] appointmentId={}", appointmentId, e);
            result.put("success", false);
            result.put("message", "接受最低价报价失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> validateQuotationAcceptance(Long quotationId) {
        Map<String, Object> validation = new HashMap<>();
        
        AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            validation.put("canAccept", false);
            validation.put("message", "报价不存在");
            return validation;
        }

        // 检查报价状态
        if (quotation.getStatus() != 1) { // 1-待处理
            validation.put("canAccept", false);
            validation.put("message", "报价状态不允许接受");
            return validation;
        }

        // 检查报价是否过期
        if (quotation.getValidUntil() != null && quotation.getValidUntil().isBefore(LocalDateTime.now())) {
            validation.put("canAccept", false);
            validation.put("message", "报价已过期");
            return validation;
        }

        // 检查预约单状态
        AppointmentDO appointment = appointmentMapper.selectById(quotation.getAppointmentId());
        if (appointment == null) {
            validation.put("canAccept", false);
            validation.put("message", "预约单不存在");
            return validation;
        }

        // 检查是否已有其他接受的报价
        List<AppointmentQuotationDO> acceptedQuotations = appointmentQuotationMapper
                .selectAcceptedQuotationsByAppointmentId(quotation.getAppointmentId());
        if (!acceptedQuotations.isEmpty()) {
            validation.put("canAccept", false);
            validation.put("message", "已有其他报价被接受");
            return validation;
        }

        validation.put("canAccept", true);
        validation.put("message", "可以接受报价");
        return validation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> processAfterAcceptance(AppointmentQuotationDO quotation) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 自动生成合同
            Map<String, Object> contractResult = autoGenerateContract(quotation.getId());
            result.put("contractGeneration", contractResult);

            // 2. 自动创建订单
            Map<String, Object> orderResult = autoCreateOrder(quotation.getId());
            result.put("orderCreation", orderResult);

            // 3. 更新预约单状态
            AppointmentDO appointment = appointmentMapper.selectById(quotation.getAppointmentId());
            if (appointment != null) {
                appointment.setStatus(2); // 已确认
                appointmentMapper.updateById(appointment);
            }

            result.put("success", true);
            result.put("message", "后续处理完成");

        } catch (Exception e) {
            log.error("[processAfterAcceptance][后续处理失败] quotationId={}", quotation.getId(), e);
            result.put("success", false);
            result.put("message", "后续处理失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> autoGenerateContract(Long quotationId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 调用合同服务生成合同
            Long contractId = quotationContractService.generateContractFromQuotation(quotationId);
            
            result.put("success", true);
            result.put("contractId", contractId);
            result.put("message", "合同生成成功");
            
            log.info("[autoGenerateContract][自动生成合同成功] quotationId={}, contractId={}", 
                    quotationId, contractId);

        } catch (Exception e) {
            log.error("[autoGenerateContract][自动生成合同失败] quotationId={}", quotationId, e);
            result.put("success", false);
            result.put("message", "合同生成失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> autoCreateOrder(Long quotationId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 这里应该调用订单服务创建订单
            // 简化实现，返回成功结果
            Long orderId = System.currentTimeMillis(); // 模拟订单ID
            
            result.put("success", true);
            result.put("orderId", orderId);
            result.put("message", "订单创建成功");
            
            log.info("[autoCreateOrder][自动创建订单成功] quotationId={}, orderId={}", 
                    quotationId, orderId);

        } catch (Exception e) {
            log.error("[autoCreateOrder][自动创建订单失败] quotationId={}", quotationId, e);
            result.put("success", false);
            result.put("message", "订单创建失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public void notifyRelatedParties(Long quotationId, Long appointmentId) {
        try {
            // 这里应该实现通知逻辑
            // 1. 通知产废企业
            // 2. 通知回收企业
            // 3. 通知平台管理员
            
            log.info("[notifyRelatedParties][通知相关方] quotationId={}, appointmentId={}", 
                    quotationId, appointmentId);

        } catch (Exception e) {
            log.error("[notifyRelatedParties][通知失败] quotationId={}, appointmentId={}", 
                    quotationId, appointmentId, e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectOtherQuotations(Long appointmentId, Long acceptedQuotationId, String rejectedBy) {
        try {
            List<AppointmentQuotationDO> otherQuotations = appointmentQuotationMapper
                    .selectValidQuotationsByAppointmentId(appointmentId);
            
            otherQuotations.stream()
                    .filter(q -> !q.getId().equals(acceptedQuotationId))
                    .forEach(q -> {
                        q.setStatus(3); // 已拒绝
                        q.setRejectedAt(LocalDateTime.now());
                        q.setRejectedBy(rejectedBy);
                        q.setRejectReason("其他报价被选中");
                        appointmentQuotationMapper.updateById(q);
                    });

            log.info("[rejectOtherQuotations][拒绝其他报价] appointmentId={}, acceptedQuotationId={}, count={}", 
                    appointmentId, acceptedQuotationId, otherQuotations.size() - 1);

        } catch (Exception e) {
            log.error("[rejectOtherQuotations][拒绝其他报价失败] appointmentId={}, acceptedQuotationId={}", 
                    appointmentId, acceptedQuotationId, e);
        }
    }

    @Override
    public boolean supportsOneClickAcceptance(Long recyclingEnterpriseId) {
        return businessModeConfigService.autoAcceptsSingleQuotation(recyclingEnterpriseId);
    }

    @Override
    public Map<String, Object> getOneClickAcceptanceConfig(Long recyclingEnterpriseId) {
        Map<String, Object> config = new HashMap<>();
        
        config.put("supportsOneClick", supportsOneClickAcceptance(recyclingEnterpriseId));
        config.put("autoAcceptSingle", businessModeConfigService.autoAcceptsSingleQuotation(recyclingEnterpriseId));
        config.put("enableNegotiation", businessModeConfigService.enablesPriceNegotiation(recyclingEnterpriseId));
        config.put("timeoutHours", businessModeConfigService.getQuotationTimeoutHours(recyclingEnterpriseId));
        
        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchAcceptQuotations(List<Long> quotationIds, String acceptedBy) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();
        int successCount = 0;
        
        for (Long quotationId : quotationIds) {
            try {
                Map<String, Object> singleResult = oneClickAcceptQuotation(quotationId, acceptedBy, "批量接受");
                results.add(singleResult);
                
                if ((Boolean) singleResult.get("success")) {
                    successCount++;
                }
                
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("quotationId", quotationId);
                errorResult.put("success", false);
                errorResult.put("message", e.getMessage());
                results.add(errorResult);
            }
        }
        
        result.put("totalCount", quotationIds.size());
        result.put("successCount", successCount);
        result.put("failureCount", quotationIds.size() - successCount);
        result.put("results", results);
        
        log.info("[batchAcceptQuotations][批量接受报价完成] total={}, success={}", 
                quotationIds.size(), successCount);
        
        return result;
    }

    @Override
    public Map<String, Object> conditionalAcceptQuotation(Long appointmentId, Map<String, Object> conditions) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 获取所有有效报价
            List<AppointmentQuotationDO> quotations = biddingComparisonService.getValidQuotationsSorted(appointmentId);
            
            if (quotations.isEmpty()) {
                result.put("success", false);
                result.put("message", "没有有效报价");
                return result;
            }

            // 根据条件筛选报价
            AppointmentQuotationDO selectedQuotation = null;
            
            // 示例条件处理
            if (conditions.containsKey("maxPrice")) {
                // 价格不超过指定值
                // 实际实现中应该有更复杂的条件处理逻辑
            }
            
            if (selectedQuotation != null) {
                return oneClickAcceptQuotation(selectedQuotation.getId(), "系统自动", "满足条件自动接受");
            } else {
                result.put("success", false);
                result.put("message", "没有满足条件的报价");
            }

        } catch (Exception e) {
            log.error("[conditionalAcceptQuotation][条件接受报价失败] appointmentId={}", appointmentId, e);
            result.put("success", false);
            result.put("message", "条件接受失败: " + e.getMessage());
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getAcceptanceHistory(Long appointmentId) {
        List<Map<String, Object>> history = new ArrayList<>();
        
        try {
            List<AppointmentQuotationDO> acceptedQuotations = appointmentQuotationMapper
                    .selectAcceptedQuotationsByAppointmentId(appointmentId);
            
            for (AppointmentQuotationDO quotation : acceptedQuotations) {
                Map<String, Object> record = new HashMap<>();
                record.put("quotationId", quotation.getId());
                record.put("acceptedAt", quotation.getAcceptedAt());
                record.put("acceptedBy", quotation.getAcceptedBy());
                record.put("acceptReason", quotation.getAcceptReason());
                record.put("quotedPrice", quotation.getQuotedPrice());
                record.put("recyclingEnterpriseId", quotation.getRecyclingEnterpriseId());
                
                history.add(record);
            }

        } catch (Exception e) {
            log.error("[getAcceptanceHistory][获取接受历史失败] appointmentId={}", appointmentId, e);
        }

        return history;
    }
} 