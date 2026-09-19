package cn.iocoder.yudao.module.icbc.controller.admin.notify.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 出售者触达记录 Response VO")
@Data
public class NotifyRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "触达类型")
    private String bizType;

    @Schema(description = "触达类型名")
    private String bizTypeName;

    @Schema(description = "业务键（幂等键）")
    private String bizKey;

    @Schema(description = "短信模板编码")
    private String templateCode;

    @Schema(description = "出售者档案编号")
    private Long payeeId;

    @Schema(description = "出售者姓名")
    private String sellerName;

    @Schema(description = "接收手机号（脱敏）")
    private String mobileMasked;

    @Schema(description = "发送状态")
    private Integer status;

    @Schema(description = "发送状态名")
    private String statusName;

    @Schema(description = "短信正文")
    private String content;

    @Schema(description = "一次性令牌链接")
    private String link;

    @Schema(description = "失败原因 / 未发送说明")
    private String errorMsg;

    @Schema(description = "发送时间")
    private LocalDateTime sendTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
