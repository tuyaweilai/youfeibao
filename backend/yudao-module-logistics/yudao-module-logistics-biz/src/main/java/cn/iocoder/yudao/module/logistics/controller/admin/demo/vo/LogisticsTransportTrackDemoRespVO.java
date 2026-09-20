package cn.iocoder.yudao.module.logistics.controller.admin.demo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 运输轨迹**演示件**的响应（V9 #76）。
 *
 * <p>这个对象刻意把「这是模拟数据」写在最显眼的位置：{@code enabled} / {@code simulated} / {@code note}
 * 三个字段都在回答同一个问题——**看到这条线的人必须能立刻知道它不是真实行驶路径**。
 * 税总 5 号公告第十七条把「运输发票或凭证」列为业务真实性材料，在证据链里放一条假路径
 * 违反 ADR 0021「只讲可核验的事」，所以演示数据从字段上就与真实凭证分开。
 */
@Schema(description = "管理后台 - 运输轨迹演示（模拟数据，不代表真实行驶路径）")
@Data
public class LogisticsTransportTrackDemoRespVO {

    @Schema(description = "演示轨迹是否开启（由 logistics.demo.transport-track.enabled 控制）", example = "true")
    private Boolean enabled;

    @Schema(description = "数据来源：恒为 SIMULATED", example = "SIMULATED")
    private String source;

    @Schema(description = "给页面直接显示的说明：这条线是模拟的，不代表真实行驶路径")
    private String note;

    @Schema(description = "轨迹点（模拟）")
    private List<TrackPoint> points;

    @Schema(description = "锚点：真实上报过的节点位置（有经纬度的那些）。模拟轨迹在锚点之间插值，"
            + "所以它至少锚在真事实上，但**中间的路是编的**")
    private List<TrackAnchor> anchors;

    @Schema(description = "运输轨迹演示点")
    @Data
    public static class TrackPoint {

        @Schema(description = "纬度")
        private BigDecimal latitude;

        @Schema(description = "经度")
        private BigDecimal longitude;

        @Schema(description = "模拟时刻")
        private LocalDateTime time;

        @Schema(description = "恒为 true：这个点是模拟的", example = "true")
        private Boolean simulated;

    }

    @Schema(description = "运输轨迹演示锚点（来自真实节点上报）")
    @Data
    public static class TrackAnchor {

        @Schema(description = "节点类型名", example = "起运")
        private String nodeTypeName;

        @Schema(description = "发生时间")
        private LocalDateTime nodeTime;

        @Schema(description = "纬度")
        private BigDecimal latitude;

        @Schema(description = "经度")
        private BigDecimal longitude;

    }

}
