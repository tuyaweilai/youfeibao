package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 运输节点上报 Request VO")
@Data
public class LogisticsTransportNodeReportReqVO {

    @Schema(description = "运输任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "运输任务编号不能为空")
    private Long taskId;

    @Schema(description = "节点类型：3-起运（本票只支持起运，其余四类归 #71）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "节点类型不能为空")
    private Integer nodeType;

    @Schema(description = "发生时间：事情实际发生的时刻（弱网补录时可能早于上报时间）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "节点发生时间不能为空")
    private LocalDateTime nodeTime;

    @Schema(description = "位置描述", example = "城东场站门口")
    private String location;

    @Schema(description = "纬度")
    private BigDecimal latitude;

    @Schema(description = "经度")
    private BigDecimal longitude;

    @Schema(description = "凭证照片 URL 列表")
    private List<String> photos;

    @Schema(description = "客户端请求号（幂等键，重复提交只落一条）",
            requiredMode = Schema.RequiredMode.REQUIRED, example = "5f1e2d3c-...")
    @NotEmpty(message = "客户端请求号不能为空")
    private String clientRequestId;

    @Schema(description = "备注")
    private String remark;

}
