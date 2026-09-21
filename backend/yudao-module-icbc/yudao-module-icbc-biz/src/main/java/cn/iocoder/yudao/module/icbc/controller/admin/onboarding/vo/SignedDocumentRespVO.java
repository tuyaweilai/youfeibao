package cn.iocoder.yudao.module.icbc.controller.admin.onboarding.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已签文书（#95）。两份文书各自一条：框架收购协议 + 反向发票合规告知函。
 *
 * <p>文件托管在第三方，我们只留地址；地址每次从端口现查，不落库。
 */
@Schema(description = "管理后台 - 框架收购协议已签文书")
@Data
public class SignedDocumentRespVO {

    @Schema(description = "文书名", example = "框架收购协议")
    private String name;

    @Schema(description = "已签文件地址（托管在第三方）")
    private String fileUrl;

    @Schema(description = "该文书签署时间")
    private LocalDateTime signedAt;

}
