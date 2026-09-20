package cn.iocoder.yudao.module.waste.service.quotation;

import cn.iocoder.yudao.module.waste.controller.admin.quotation.vo.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 基于报价的快速电子合同生成 Service 接口
 *
 * @author 芋道源码
 */
public interface QuotationContractService {

    /**
     * 基于报价生成合同
     *
     * @param reqVO 生成请求
     * @return 生成结果
     */
    QuotationContractGenerateRespVO generateContractFromQuotation(@Valid QuotationContractGenerateReqVO reqVO);

    /**
     * 基于报价生成合同（简化版本）
     *
     * @param quotationId 报价ID
     * @return 合同ID
     */
    Long generateContractFromQuotation(Long quotationId);

    /**
     * 填充合同模板
     *
     * @param quotationId 报价ID
     * @param templateContent 模板内容
     * @return 填充后的合同内容
     */
    String fillContractTemplate(Long quotationId, String templateContent);

    /**
     * 获取报价的默认合同类型
     *
     * @param quotationId 报价ID
     * @return 合同类型ID
     */
    Long getDefaultContractTypeForQuotation(Long quotationId);

    /**
     * 检查是否可以生成合同
     *
     * @param quotationId 报价ID
     * @return 是否可以生成
     */
    boolean canGenerateContract(Long quotationId);

    /**
     * 发起合同签署流程
     *
     * @param contractId 合同ID
     * @param signerEnterpriseIds 签署企业ID列表
     * @return 签署流程ID
     */
    Long initiateContractSigning(Long contractId, List<Long> signerEnterpriseIds);

    /**
     * 一键接受报价并生成合同
     *
     * @param quotationId 报价ID
     * @param contractTypeId 合同类型ID
     * @param templateId 模板ID
     * @return 生成结果
     */
    QuotationContractGenerateRespVO acceptQuotationAndGenerateContract(Long quotationId, Long contractTypeId, Long templateId);

    /**
     * 批量生成合同（新版本）
     *
     * @param reqVO 批量生成请求
     * @return 批量生成结果
     */
    QuotationContractBatchGenerateRespVO batchGenerateContracts(@Valid QuotationContractBatchGenerateReqVO reqVO);

    /**
     * 批量生成合同（兼容版本）
     *
     * @param quotationIds 报价ID列表
     * @param contractTypeId 合同类型ID
     * @return 合同ID列表
     */
    List<Long> batchGenerateContracts(List<Long> quotationIds, Long contractTypeId);
} 