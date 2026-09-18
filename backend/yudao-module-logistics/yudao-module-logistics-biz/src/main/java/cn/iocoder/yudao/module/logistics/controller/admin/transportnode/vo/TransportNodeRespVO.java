package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 物流运输节点记录 Response VO")
@Data
public class TransportNodeRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "运输任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long taskId;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer nodeType;

    @Schema(description = "节点类型名称", example = "任务创建")
    private String nodeTypeName;

    @Schema(description = "节点时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 10:00:00")
    private LocalDateTime nodeTime;

    @Schema(description = "节点位置", example = "北京市朝阳区xxx街道xxx号")
    private String nodeLocation;

    @Schema(description = "纬度", example = "39.9042")
    private BigDecimal latitude;

    @Schema(description = "经度", example = "116.4074")
    private BigDecimal longitude;

    @Schema(description = "操作员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long operatorId;

    @Schema(description = "操作员姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    private String operatorName;

    @Schema(description = "照片URLs(JSON数组)", example = "[\"http://example.com/photo1.jpg\",\"http://example.com/photo2.jpg\"]")
    private String photos;

    @Schema(description = "附加数据(JSON格式)", example = "{\"temperature\":25,\"humidity\":60}")
    private String additionalData;

    @Schema(description = "备注", example = "正常到达")
    private String remark;

    @Schema(description = "租户ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long tenantId;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 08:00:00")
    private LocalDateTime createTime;

    @Schema(description = "更新时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01 18:00:00")
    private LocalDateTime updateTime;

} 