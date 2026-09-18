package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 运输节点记录创建 Request VO")
@Data
public class TransportNodeCreateReqVO {

    @Schema(description = "任务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "节点类型不能为空")
    private Integer nodeType;

    @Schema(description = "节点时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "节点时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nodeTime;

    @Schema(description = "位置信息", example = "上海市浦东新区")
    private String location;

    @Schema(description = "纬度", example = "31.2304")
    private BigDecimal latitude;

    @Schema(description = "经度", example = "121.4737")
    private BigDecimal longitude;

    @Schema(description = "操作员ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "操作员ID不能为空")
    private Long operatorId;

    @Schema(description = "操作员姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotBlank(message = "操作员姓名不能为空")
    private String operatorName;

    @Schema(description = "照片URLs(JSON数组)", example = "[\"http://example.com/photo1.jpg\"]")
    private String photos;

    @Schema(description = "附加数据(JSON格式)", example = "{\"temperature\":25}")
    private String additionalData;

    @Schema(description = "备注", example = "正常到达")
    private String remark;
} 