package cn.iocoder.yudao.module.icbc.controller.admin.log.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工行接口调用日志 Response VO")
@Data
public class ApiLogRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "消息通讯唯一编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MSG123456")
    private String msgId;

    @Schema(description = "接口名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "收方新增接口")
    private String apiName;

    @Schema(description = "接口URL", requiredMode = Schema.RequiredMode.REQUIRED, example = "/jft/api/user/edpreceive/add/V1")
    private String apiUrl;

    @Schema(description = "请求方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "POST")
    private String method;

    @Schema(description = "请求参数（脱敏后）", example = "{\"name\":\"张三\",\"idCardNo\":\"***\"}")
    private String requestParams;

    @Schema(description = "响应数据", example = "{\"return_code\":\"0000\",\"return_msg\":\"成功\"}")
    private String responseData;

    @Schema(description = "工行返回码", example = "0000")
    private String returnCode;

    @Schema(description = "工行返回消息", example = "成功")
    private String returnMsg;

    @Schema(description = "调用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer status;

    @Schema(description = "耗时（毫秒）", example = "1500")
    private Integer costTime;

    @Schema(description = "业务ID", example = "ORDER123456")
    private String businessId;

    @Schema(description = "业务类型", example = "PAYEE_ADD")
    private String businessType;

    @Schema(description = "错误信息", example = "网络超时")
    private String errorMsg;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

} 