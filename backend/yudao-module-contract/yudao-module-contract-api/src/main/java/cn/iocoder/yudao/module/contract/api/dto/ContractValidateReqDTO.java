package cn.iocoder.yudao.module.contract.api.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 合同校验请求 DTO
 *
 * @author 芋道源码
 */
@Data
public class ContractValidateReqDTO {

    /**
     * 业务类型
     */
    @NotNull(message = "业务类型不能为空")
    private String businessType;

    /**
     * 参与企业ID列表
     */
    @NotEmpty(message = "参与企业不能为空")
    private List<Long> partyEnterpriseIds;

    /**
     * 业务日期
     */
    @NotNull(message = "业务日期不能为空")
    private LocalDate businessDate;

    /**
     * 废物类型列表（可选）
     */
    private List<String> wasteTypes;

    /**
     * 额外条件（可选）
     */
    private Map<String, Object> additionalCriteria;

} 