package cn.iocoder.yudao.module.logistics.service.demo.impl;

import cn.iocoder.yudao.module.logistics.controller.admin.demo.vo.LogisticsTransportTrackDemoRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transportnode.LogisticsTransportNodeDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsTransportNodeTypeEnum;
import cn.iocoder.yudao.module.logistics.service.demo.LogisticsTrackDemoService;
import cn.iocoder.yudao.module.logistics.service.transportnode.LogisticsTransportNodeService;
import cn.iocoder.yudao.module.logistics.service.transporttask.LogisticsTransportTaskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * 运输轨迹演示件 Service 实现（V9 #76）。
 *
 * <p><b>只读、只算、不写</b>：整个类里没有 mapper，也没有任何 insert —— 这是「演示数据不进证据链」
 * 最直接的保证（测试会断言跑完之后节点表一行没多）。
 *
 * <p>模拟方式：取真实上报过的、带经纬度的节点作为**锚点**，在相邻锚点之间插点；插值带上按
 * 「任务编号 + 段号」做种子的确定性偏移，让它看起来像条路，而不是一条直尺线。锚点不足两个时，
 * 用一个固定的演示原点画一小段——那种情况下**连锚点都是编的**，页面上的说明会写明。
 */
@Service
@Validated
public class LogisticsTrackDemoServiceImpl implements LogisticsTrackDemoService {

    /** 演示原点（杭州附近）：锚点不足两个时的落脚点 */
    private static final BigDecimal DEMO_ORIGIN_LATITUDE = new BigDecimal("30.2741000");
    private static final BigDecimal DEMO_ORIGIN_LONGITUDE = new BigDecimal("120.1551000");

    /** 每段插几个点 */
    private static final int POINTS_PER_SEGMENT = 6;

    /** 没锚点时那条假路线的跨度（度），约几公里 */
    private static final BigDecimal FALLBACK_SPAN = new BigDecimal("0.0200000");

    @Value("${logistics.demo.transport-track.enabled:false}")
    private boolean demoEnabled;

    @Resource
    private LogisticsTransportTaskService logisticsTransportTaskService;
    @Resource
    private LogisticsTransportNodeService logisticsTransportNodeService;

    @Override
    public LogisticsTransportTrackDemoRespVO getTransportTrackDemo(Long taskId) {
        LogisticsTransportTaskDO task = logisticsTransportTaskService.getTask(taskId);

        LogisticsTransportTrackDemoRespVO resp = new LogisticsTransportTrackDemoRespVO();
        resp.setSource("SIMULATED");
        resp.setEnabled(demoEnabled);
        resp.setNote("演示数据：这条线是按锚点插值算出来的模拟轨迹，不代表真实行驶路径，"
                + "也不会进入一票一档、异常统计或任何报表。真实凭证只有节点照片与上报记录。");
        if (!demoEnabled) {
            // 关掉时连点都不给：页面回到「只有真实节点与凭证」
            resp.setPoints(new ArrayList<>());
            resp.setAnchors(new ArrayList<>());
            return resp;
        }

        List<LogisticsTransportNodeDO> realNodes = logisticsTransportNodeService.getNodeListByTaskId(task.getId())
                .stream()
                .filter(node -> node.getLatitude() != null && node.getLongitude() != null)
                .sorted(Comparator.comparing(LogisticsTransportNodeDO::getNodeTime))
                .collect(Collectors.toList());

        resp.setAnchors(realNodes.stream().map(this::toAnchor).collect(Collectors.toList()));
        resp.setPoints(buildPoints(task, realNodes));
        return resp;
    }

    private LogisticsTransportTrackDemoRespVO.TrackAnchor toAnchor(LogisticsTransportNodeDO node) {
        LogisticsTransportTrackDemoRespVO.TrackAnchor anchor = new LogisticsTransportTrackDemoRespVO.TrackAnchor();
        anchor.setNodeTypeName(LogisticsTransportNodeTypeEnum.ofType(node.getNodeType())
                .map(LogisticsTransportNodeTypeEnum::getName).orElse(null));
        anchor.setNodeTime(node.getNodeTime());
        anchor.setLatitude(node.getLatitude());
        anchor.setLongitude(node.getLongitude());
        return anchor;
    }

    private List<LogisticsTransportTrackDemoRespVO.TrackPoint> buildPoints(
            LogisticsTransportTaskDO task, List<LogisticsTransportNodeDO> anchors) {
        List<LogisticsTransportTrackDemoRespVO.TrackPoint> points = new ArrayList<>();
        if (anchors.size() >= 2) {
            for (int i = 0; i < anchors.size() - 1; i++) {
                interpolate(points, task.getId(), i, anchors.get(i), anchors.get(i + 1));
            }
            // 最后一个锚点本身也要在线上
            points.add(point(anchors.get(anchors.size() - 1).getLatitude(),
                    anchors.get(anchors.size() - 1).getLongitude(),
                    anchors.get(anchors.size() - 1).getNodeTime()));
            return points;
        }
        // 锚点不足两个：连锚点都是编的，画一小段并让时间从「现在往前半小时」排开
        LocalDateTime start = anchors.isEmpty()
                ? LocalDateTime.now().minusMinutes(30)
                : anchors.get(0).getNodeTime();
        Random random = new Random(task.getId() == null ? 0L : task.getId());
        BigDecimal latitude = anchors.isEmpty() ? DEMO_ORIGIN_LATITUDE : anchors.get(0).getLatitude();
        BigDecimal longitude = anchors.isEmpty() ? DEMO_ORIGIN_LONGITUDE : anchors.get(0).getLongitude();
        BigDecimal step = FALLBACK_SPAN.divide(BigDecimal.valueOf(POINTS_PER_SEGMENT), 7, RoundingMode.HALF_UP);
        for (int i = 0; i <= POINTS_PER_SEGMENT; i++) {
            points.add(point(latitude, longitude, start.plusMinutes(5L * i)));
            latitude = latitude.add(step).add(jitter(random));
            longitude = longitude.add(step).add(jitter(random));
        }
        return points;
    }

    private void interpolate(List<LogisticsTransportTrackDemoRespVO.TrackPoint> points, Long taskId, int segment,
                             LogisticsTransportNodeDO from, LogisticsTransportNodeDO to) {
        Random random = new Random((taskId == null ? 0L : taskId) * 31 + segment);
        for (int i = 0; i < POINTS_PER_SEGMENT; i++) {
            BigDecimal ratio = BigDecimal.valueOf(i).divide(BigDecimal.valueOf(POINTS_PER_SEGMENT), 7, RoundingMode.HALF_UP);
            BigDecimal latitude = lerp(from.getLatitude(), to.getLatitude(), ratio).add(jitter(random));
            BigDecimal longitude = lerp(from.getLongitude(), to.getLongitude(), ratio).add(jitter(random));
            long minutes = java.time.Duration.between(from.getNodeTime(), to.getNodeTime()).toMinutes();
            LocalDateTime time = from.getNodeTime().plusMinutes(minutes * i / POINTS_PER_SEGMENT);
            points.add(point(latitude, longitude, time));
        }
    }

    private BigDecimal lerp(BigDecimal from, BigDecimal to, BigDecimal ratio) {
        return from.add(to.subtract(from).multiply(ratio)).setScale(7, RoundingMode.HALF_UP);
    }

    /** 确定性抖动：同一条任务每次算出来一样，避免「刷新一次换一条路」 */
    private BigDecimal jitter(Random random) {
        return BigDecimal.valueOf((random.nextDouble() - 0.5) * 0.002).setScale(7, RoundingMode.HALF_UP);
    }

    private LogisticsTransportTrackDemoRespVO.TrackPoint point(BigDecimal latitude, BigDecimal longitude,
                                                              LocalDateTime time) {
        LogisticsTransportTrackDemoRespVO.TrackPoint point = new LogisticsTransportTrackDemoRespVO.TrackPoint();
        point.setLatitude(latitude.setScale(7, RoundingMode.HALF_UP));
        point.setLongitude(longitude.setScale(7, RoundingMode.HALF_UP));
        point.setTime(time);
        point.setSimulated(true);
        return point;
    }

}
