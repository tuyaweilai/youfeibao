package cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo;

import cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo.LogisticsTransportStopSaveReqVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.Valid;
import java.util.List;

/**
 * 运输任务新增 Request VO（V5 #72）。
 *
 * <p>比 {@link LogisticsTransportTaskSaveReqVO} 多一个 {@code stops}：建任务时一次把多个停靠点带上
 * （一车提三家）。**分开成两个 VO 是因为响应 VO 也继承了保存 VO**——响应要返回带进度的停靠点
 *（{@code List<LogisticsTransportStopRespVO>}），与保存请求的 {@code List<LogisticsTransportStopSaveReqVO>}
 * 同名同泛型位置会与父类 getter 冲突。
 */
@Schema(description = "管理后台 - 运输任务新增 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsTransportTaskCreateReqVO extends LogisticsTransportTaskSaveReqVO {

    @Schema(description = "停靠点（一个任务可含多个、对应不同出售者；建任务时一起建）")
    @Valid
    private List<LogisticsTransportStopSaveReqVO> stops;

}
