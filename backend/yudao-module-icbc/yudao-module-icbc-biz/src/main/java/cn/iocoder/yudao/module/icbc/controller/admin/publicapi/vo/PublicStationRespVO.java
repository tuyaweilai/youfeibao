package cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 场站公开信息（免登录首屏）。
 *
 * <p>扫码进来先看这些**公开且长期**的信息，再看「我的待确认」需要手机号验证（ADR 0017）。
 * 本 VO **不含任何个人数据**：回收企业名称、场站、地址、是否在收货、场站电话。
 */
@Schema(description = "公开端点 - 场站公开信息 Response VO")
@Data
public class PublicStationRespVO {

    @Schema(description = "场站码（二维码内编码的值）", example = "STATION_A")
    private String stationCode;

    @Schema(description = "回收企业名称", example = "某某再生资源有限公司")
    private String enterpriseName;

    @Schema(description = "场站名称", example = "城东收货点")
    private String stationName;

    @Schema(description = "场站地址", example = "某某路 1 号")
    private String address;

    @Schema(description = "是否在收货", example = "true")
    private Boolean open;

    @Schema(description = "是否在收货文案", example = "正在收货")
    private String openStatusName;

    @Schema(description = "场站联系电话（公开，用于联系客服）", example = "0571-88888888")
    private String contactMobile;

    /**
     * 该场站所属租户编号。前端把它作为后续请求的 {@code tenant-id}：
     * 「登录后按该场站 + 该自然人主体匹配待确认结算单」靠它落到正确的回收企业。
     */
    @Schema(description = "所属租户编号", example = "1")
    private Long tenantId;

    @Schema(description = "扫码进入后要做的动作提示（公开信息，不含个人数据）")
    private List<String> guide;

}
