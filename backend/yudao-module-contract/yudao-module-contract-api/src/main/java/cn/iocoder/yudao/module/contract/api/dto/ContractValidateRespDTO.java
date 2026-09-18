package cn.iocoder.yudao.module.contract.api.dto;

import lombok.Data;

import java.util.List;

/**
 * 合同校验响应 DTO
 *
 * @author 芋道源码
 */
@Data
public class ContractValidateRespDTO {

    /**
     * 是否有效
     */
    private Boolean isValid;

    /**
     * 有效合同列表
     */
    private List<ValidContract> validContracts;

    /**
     * 校验详情
     */
    private ValidationDetails validationDetails;

    @Data
    public static class ValidContract {
        /**
         * 合同ID
         */
        private Long contractId;

        /**
         * 合同编号
         */
        private String contractNo;

        /**
         * 合同名称
         */
        private String contractName;

        /**
         * 匹配分数
         */
        private Integer matchScore;
    }

    @Data
    public static class ValidationDetails {
        /**
         * 是否有有效合同
         */
        private Boolean hasValidContract;

        /**
         * 合同数量
         */
        private Integer contractCount;

        /**
         * 即将到期的合同
         */
        private List<ValidContract> expiringContracts;

        /**
         * 推荐建议
         */
        private List<String> recommendations;
    }

} 