package cn.iocoder.yudao.module.icbc.controller.admin.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量开票申请入参。
 *
 * <p>批量中某一笔失败不影响其他笔：服务端逐笔独立校验与提交，逐笔返回结果。
 */
@Schema(description = "管理后台 - 批量开票申请入参")
@Data
@EqualsAndHashCode(callSuper = true)
public class InvoiceApplicationBatchReqVO extends InvoiceApplicationBaseReqVO {

    @Schema(description = "收购单编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "收购单编号列表不能为空")
    private List<Long> acquisitionIds;
}
