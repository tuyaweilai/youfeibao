package cn.iocoder.yudao.module.logistics.controller.admin.demo;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.logistics.controller.admin.demo.vo.LogisticsTransportTrackDemoRespVO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.demo.LogisticsTrackDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 运输轨迹**演示件**（V9 #76）。
 *
 * <p>路径上带 {@code demo} 是刻意的：它不该被当成「轨迹接口」，免得将来有人把它接进证据链。
 * 一期没有连续定位能力（CONTEXT.md「轨迹」只有上报时的一次性快照），这里产出的是一条模拟折线，
 * 由 {@code logistics.demo.transport-track.enabled} 一键开关，响应里显式标 {@code source=SIMULATED}。
 */
@Tag(name = "管理后台 - 运输轨迹演示（模拟数据）")
@RestController
@RequestMapping("/logistics/demo")
@Validated
public class LogisticsTrackDemoController {

    @Resource
    private LogisticsTrackDemoService logisticsTrackDemoService;

    @GetMapping("/transport-track")
    @Operation(summary = "取运输轨迹演示（模拟数据，不代表真实行驶路径）",
            description = "不落库、不进一票一档、不进异常统计与报表；开关关闭时返回 enabled=false 且不带点")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.DEMO_TRACK_QUERY + "')")
    public CommonResult<LogisticsTransportTrackDemoRespVO> getTransportTrackDemo(@RequestParam("taskId") Long taskId) {
        return success(logisticsTrackDemoService.getTransportTrackDemo(taskId));
    }

}
