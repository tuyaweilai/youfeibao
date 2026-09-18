package cn.iocoder.yudao.module.waste.service.impl.quotation;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.waste.dal.dataobject.appointment.AppointmentDO;
import cn.iocoder.yudao.module.waste.dal.dataobject.quotation.AppointmentQuotationDO;
import cn.iocoder.yudao.module.waste.dal.mysql.appointment.AppointmentMapper;
import cn.iocoder.yudao.module.waste.dal.mysql.quotation.AppointmentQuotationMapper;
import cn.iocoder.yudao.module.waste.enums.QuotationStatusEnum;
import cn.iocoder.yudao.module.waste.service.quotation.AppointmentQuotationService;
import cn.iocoder.yudao.module.waste.service.quotation.QuotationContractService;
import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.waste.enums.ErrorCodeConstants.*;

/**
 * 基于报价的快速电子合同生成服务实现类
 *
 * @author 芋道源码
 */
@Service
@Slf4j
public class QuotationContractServiceImpl implements QuotationContractService {

    @Resource
    private AppointmentQuotationMapper appointmentQuotationMapper;
    
    @Resource
    private AppointmentMapper appointmentMapper;
    
    @Resource
    private AppointmentQuotationService appointmentQuotationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationContractGenerateRespVO generateContractFromQuotation(QuotationContractGenerateReqVO reqVO) {
        log.info("[generateContractFromQuotation][基于报价生成合同] quotationId={}", reqVO.getQuotationId());
        
        // 1. 校验报价存在且已接受
        AppointmentQuotationDO quotation = validateQuotationAccepted(reqVO.getQuotationId());
        
        // 2. 获取预约信息
        AppointmentDO appointment = appointmentMapper.selectById(quotation.getAppointmentId());
        if (appointment == null) {
            throw exception(APPOINTMENT_NOT_EXISTS);
        }
        
        // 3. 生成合同内容
        String contractContent = generateContractContent(quotation, appointment, reqVO);
        
        // 4. 创建合同（暂时模拟）
        // TODO: 等待合同模块完全集成后启用
        Long contractId = createContractMock(quotation, appointment, contractContent, reqVO);
        
        // 5. 更新报价状态为已生成合同
        quotation.setContractId(contractId);
        quotation.setStatus(6); // 已生成合同
        appointmentQuotationMapper.updateById(quotation);
        
        // 6. 构建返回结果
        QuotationContractGenerateRespVO respVO = new QuotationContractGenerateRespVO();
        respVO.setContractId(contractId);
        respVO.setQuotationId(quotation.getId());
        respVO.setContractContent(contractContent);
        respVO.setContractName(generateContractName(quotation, appointment));
        respVO.setTotalAmount(quotation.getTotalAmount());
        respVO.setGenerateTime(LocalDateTime.now());
        respVO.setSuccess(true);
        respVO.setMessage("合同生成成功");
        
        log.info("[generateContractFromQuotation][合同生成成功] contractId={}, quotationId={}", 
                contractId, quotation.getId());
        
        return respVO;
    }

    @Override
    public Long generateContractFromQuotation(Long quotationId) {
        // 获取默认合同类型
        Long defaultContractTypeId = getDefaultContractTypeForQuotation(quotationId);
        
        QuotationContractGenerateReqVO reqVO = new QuotationContractGenerateReqVO();
        reqVO.setQuotationId(quotationId);
        reqVO.setContractTypeId(defaultContractTypeId);
        reqVO.setTemplateId(null);
        
        QuotationContractGenerateRespVO result = generateContractFromQuotation(reqVO);
        return result.getContractId();
    }

    @Override
    public String fillContractTemplate(Long quotationId, String templateContent) {
        // 1. 获取报价信息
        AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw new IllegalArgumentException("报价记录不存在");
        }
        
        // 2. 获取预约信息
        AppointmentDO appointment = appointmentMapper.selectById(quotation.getAppointmentId());
        if (appointment == null) {
            throw new IllegalArgumentException("预约记录不存在");
        }
        
        // 3. 构建模板变量映射
        Map<String, String> variables = buildTemplateVariables(quotation, appointment);
        
        // 4. 替换模板变量
        String filledContent = templateContent;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            filledContent = filledContent.replace(placeholder, entry.getValue());
        }
        
        log.debug("[fillContractTemplate][合同模板填充完成] quotationId={}", quotationId);
        
        return filledContent;
    }

    @Override
    public Long getDefaultContractTypeForQuotation(Long quotationId) {
        // 根据业务逻辑确定默认合同类型
        // 这里简化处理，实际应该根据废物类型、业务模式等确定
        return 1L; // 假设1为危废转移合同类型ID
    }

    @Override
    public boolean canGenerateContract(Long quotationId) {
        AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            return false;
        }
        
        // 只有已被接受的报价才能生成合同
        return QuotationStatusEnum.ACCEPTED.getStatus().equals(quotation.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long initiateContractSigning(Long contractId, List<Long> signerEnterpriseIds) {
        log.info("[initiateContractSigning][发起合同签署] contractId={}, signerIds={}", contractId, signerEnterpriseIds);
        
        // 暂时模拟签署流程
        // TODO: 等待合同模块完全集成后启用
        // return contractApi.initiateEsignature(contractId, signerEnterpriseIds);
        
        log.info("[initiateContractSigning][模拟发起合同签署] contractId={}", contractId);
        return contractId; // 返回合同ID作为签署流程ID
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationContractGenerateRespVO acceptQuotationAndGenerateContract(Long quotationId, Long contractTypeId, Long templateId) {
        log.info("[acceptQuotationAndGenerateContract][一键接受报价并生成合同] quotationId={}", quotationId);
        
        // 1. 接受报价
        AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        
        if (quotation.getStatus() != 1) { // 1-待接受
            throw exception(QUOTATION_STATUS_NOT_PENDING);
        }
        
        quotation.setStatus(2); // 2-已接受
        quotation.setAcceptedTime(LocalDateTime.now());
        appointmentQuotationMapper.updateById(quotation);
        
        // 2. 生成合同
        QuotationContractGenerateReqVO generateReq = new QuotationContractGenerateReqVO();
        generateReq.setQuotationId(quotationId);
        generateReq.setContractTypeId(contractTypeId);
        generateReq.setTemplateId(templateId);
        
        return generateContractFromQuotation(generateReq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuotationContractBatchGenerateRespVO batchGenerateContracts(QuotationContractBatchGenerateReqVO reqVO) {
        log.info("[batchGenerateContracts][批量生成合同] quotationIds={}", reqVO.getQuotationIds());
        
        List<QuotationContractGenerateRespVO> successList = new ArrayList<>();
        List<String> failureList = new ArrayList<>();
        
        for (Long quotationId : reqVO.getQuotationIds()) {
            try {
                QuotationContractGenerateReqVO generateReq = new QuotationContractGenerateReqVO();
                generateReq.setQuotationId(quotationId);
                generateReq.setContractTypeId(reqVO.getContractTypeId());
                generateReq.setTemplateId(reqVO.getTemplateId());
                
                QuotationContractGenerateRespVO result = generateContractFromQuotation(generateReq);
                successList.add(result);
            } catch (Exception e) {
                log.error("[batchGenerateContracts][批量生成合同失败] quotationId={}", quotationId, e);
                failureList.add("报价ID " + quotationId + ": " + e.getMessage());
            }
        }
        
        QuotationContractBatchGenerateRespVO respVO = new QuotationContractBatchGenerateRespVO();
        respVO.setSuccessContracts(successList);
        respVO.setFailureMessages(failureList);
        respVO.setTotalCount(reqVO.getQuotationIds().size());
        respVO.setSuccessCount(successList.size());
        respVO.setFailureCount(failureList.size());
        
        log.info("[batchGenerateContracts][批量生成合同完成] 总数={}, 成功={}, 失败={}", 
                respVO.getTotalCount(), respVO.getSuccessCount(), respVO.getFailureCount());
        
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchGenerateContracts(List<Long> quotationIds, Long contractTypeId) {
        // 兼容版本：将参数转换为新版本的请求对象
        QuotationContractBatchGenerateReqVO reqVO = new QuotationContractBatchGenerateReqVO();
        reqVO.setQuotationIds(quotationIds);
        reqVO.setContractTypeId(contractTypeId);
        
        QuotationContractBatchGenerateRespVO result = batchGenerateContracts(reqVO);
        
        // 提取合同ID列表
        List<Long> contractIds = new ArrayList<>();
        if (result.getSuccessContracts() != null) {
            for (QuotationContractGenerateRespVO contract : result.getSuccessContracts()) {
                contractIds.add(contract.getContractId());
            }
        }
        
        return contractIds;
    }

    /**
     * 构建合同创建请求
     */
    /*
    private ContractCreateReqDTO buildContractCreateRequest(AppointmentQuotationDO quotation, 
                                                           AppointmentDO appointment, 
                                                           Long contractTypeId, Long templateId) {
        ContractCreateReqDTO createReq = new ContractCreateReqDTO();
        
        // 基本信息
        createReq.setName(generateContractName(quotation, appointment));
        createReq.setTypeId(contractTypeId);
        createReq.setTemplateId(templateId);
        createReq.setIsElectronic(true);
        createReq.setPrimaryOwnerEnterpriseId(quotation.getRecyclingEnterpriseId());
        createReq.setTotalAmount(quotation.getTotalAmount());
        createReq.setCurrency("CNY");
        createReq.setPriorityLevel(1); // 重要
        
        // 生效日期（当前日期）
        createReq.setEffectiveDate(LocalDate.now());
        
        // 失效日期（1年后）
        createReq.setExpiryDate(LocalDate.now().plusYears(1));
        
        // 备注
        createReq.setRemark("基于报价ID " + quotation.getId() + " 自动生成的合同");
        
        return createReq;
    }
    */

    /**
     * 生成合同名称
     */
    private String generateContractName(AppointmentQuotationDO quotation, AppointmentDO appointment) {
        return String.format("危废处置服务合同_%s_%s_%s", 
                appointment.getWasteName(),
                quotation.getRecyclingEnterpriseId(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
    }

    /**
     * 构建模板变量映射
     */
    private Map<String, String> buildTemplateVariables(AppointmentQuotationDO quotation, AppointmentDO appointment) {
        Map<String, String> variables = new HashMap<>();
        
        // 报价相关变量
        variables.put("quotationId", quotation.getId().toString());
        variables.put("quotedPrice", quotation.getQuotedPrice().toString());
        variables.put("totalAmount", quotation.getTotalAmount().toString());
        variables.put("priceUnit", quotation.getPriceUnit());
        variables.put("quotationRemark", StrUtil.nullToDefault(quotation.getQuotationRemark(), ""));
        
        // 预约相关变量
        variables.put("appointmentId", appointment.getId().toString());
        variables.put("wasteCode", appointment.getWasteCode());
        variables.put("wasteName", StrUtil.nullToDefault(appointment.getWasteName(), ""));
        variables.put("quantity", appointment.getEstimatedQuantity().toString());
        variables.put("unit", appointment.getQuantityUnit());
        variables.put("pickupAddress", StrUtil.nullToDefault(appointment.getPickupAddress(), ""));
        variables.put("contactPerson", StrUtil.nullToDefault(appointment.getProducerContactName(), ""));
        variables.put("contactPhone", StrUtil.nullToDefault(appointment.getProducerContactPhone(), ""));
        
        // 企业相关变量
        variables.put("recyclingEnterpriseId", quotation.getRecyclingEnterpriseId().toString());
        variables.put("customerEnterpriseId", appointment.getProducerEnterpriseId().toString());
        
        // 时间相关变量
        variables.put("currentDate", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
        variables.put("currentDateTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        return variables;
    }

    // 暂时使用模拟方法创建合同
    private Long createContractMock(AppointmentQuotationDO quotation, AppointmentDO appointment, 
                                   String contractContent, QuotationContractGenerateReqVO reqVO) {
        // 模拟合同创建，返回一个假的合同ID
        // TODO: 等待合同模块完全集成后替换为真实的合同创建逻辑
        log.info("[createContractMock][模拟创建合同] quotationId={}, appointmentId={}", 
                quotation.getId(), appointment.getId());
        return System.currentTimeMillis(); // 使用时间戳作为模拟ID
    }

    /**
     * 校验报价已被接受
     */
    private AppointmentQuotationDO validateQuotationAccepted(Long quotationId) {
        AppointmentQuotationDO quotation = appointmentQuotationMapper.selectById(quotationId);
        if (quotation == null) {
            throw exception(QUOTATION_NOT_EXISTS);
        }
        
        if (quotation.getStatus() != 1) { // 1表示已接受
            throw exception(QUOTATION_NOT_ACCEPTED);
        }
        
        return quotation;
    }

    private String generateContractContent(AppointmentQuotationDO quotation, AppointmentDO appointment, 
                                         QuotationContractGenerateReqVO reqVO) {
        // 基础合同模板
        String template = "危废处置服务合同\n\n" +
                "甲方（委托方）：{customerEnterpriseName}\n" +
                "乙方（处置方）：{recyclerEnterpriseName}\n\n" +
                "根据《中华人民共和国合同法》等相关法律法规，甲乙双方就危废处置服务事宜达成如下协议：\n\n" +
                "一、服务内容\n" +
                "1. 废物类型：{wasteType}\n" +
                "2. 废物代码：{wasteCode}\n" +
                "3. 预计数量：{estimatedQuantity} {quantityUnit}\n" +
                "4. 处置方式：{disposalMethod}\n\n" +
                "二、价格条款\n" +
                "1. 处置单价：{quotationPrice} 元/{priceUnit}\n" +
                "2. 预计总金额：{totalAmount} 元\n" +
                "3. 结算方式：{settlementMethod}\n\n" +
                "三、服务时间\n" +
                "1. 预约时间：{appointmentTime}\n" +
                "2. 服务地址：{serviceAddress}\n\n" +
                "四、其他条款\n" +
                "{additionalTerms}\n\n" +
                "本合同一式两份，甲乙双方各执一份，具有同等法律效力。\n\n" +
                "甲方（盖章）：________________    乙方（盖章）：________________\n" +
                "日期：{contractDate}              日期：{contractDate}";
        
        // 填充变量
        Map<String, String> variables = buildTemplateVariables(quotation, appointment);
        
        String contractContent = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            contractContent = contractContent.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return contractContent;
    }

    /**
     * 生成默认合同名称
     */
    private String generateDefaultContractName(AppointmentDO appointment, AppointmentQuotationDO quotation) {
        return String.format("危废处置服务合同_%s_%s_%s", 
                appointment.getWasteName(),
                quotation.getRecyclingEnterpriseId(),
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")));
    }
} 