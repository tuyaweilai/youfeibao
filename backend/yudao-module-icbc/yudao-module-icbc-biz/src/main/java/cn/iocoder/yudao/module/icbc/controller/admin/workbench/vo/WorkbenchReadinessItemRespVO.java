package cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 工作台开票就绪徽标的一项检查（#56 T18）。
 *
 * <p>这是「租户开票就绪」一级菜单被取消后的落点：就绪状态降级为工作台顶部的一个徽标，
 * 点开看这一份检查表。检查项与「开票就绪自检」页一致，只是这里只取**本地库能判定**的部分
 * （适配层连通性要打网络，不放在工作台的首屏加载里）。
 */
@Schema(description = "管理后台 - 工作台开票就绪检查项")
@Data
public class WorkbenchReadinessItemRespVO {

    @Schema(description = "检查项编码", example = "QUALIFICATION")
    private String code;

    @Schema(description = "检查项名称", example = "三层资质齐全有效")
    private String name;

    @Schema(description = "是否满足", example = "true")
    private boolean ready;

    @Schema(description = "说明（不满足时给出下一步）")
    private String message;

}
