package cn.iocoder.yudao.module.logistics.controller.admin.transporthandover;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverCreateReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporthandover.vo.LogisticsTransportHandoverRespVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporthandover.LogisticsTransportHandoverDO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.transporthandover.LogisticsTransportHandoverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 交接登记（V6 #73）。
 *
 * <p>现场登记的主入口在**司机端**（`/logistics/driver-app/handover/*`）；PC 这一组是查处与代录
 * （司机没带手机、或事后补录）。**这里没有金额**：现场不产生金额，收购单在回场复磅后由 icbc 侧生成。
 */
@Tag(name = "管理后台 - 交接登记")
@RestController
@RequestMapping("/logistics/transport-handover")
@Validated
public class LogisticsTransportHandoverController {

    @Resource
    private LogisticsTransportHandoverService logisticsTransportHandoverService;

    @PostMapping("/create")
    @Operation(summary = "登记交接（司机端为主入口，PC 可代录）",
            description = "品类、参考量、参考单价与凭证照片；同一停靠点只登记一次，带 clientRequestId 时幂等")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_HANDOVER_MANAGE + "')")
    public CommonResult<Long> createHandover(@Valid @RequestBody LogisticsTransportHandoverCreateReqVO reqVO) {
        return success(logisticsTransportHandoverService.createHandover(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得交接登记")
    @Parameter(name = "id", description = "交接登记编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_HANDOVER_QUERY + "')")
    public CommonResult<LogisticsTransportHandoverRespVO> getHandover(@RequestParam("id") Long id) {
        return success(logisticsTransportHandoverService.toResp(logisticsTransportHandoverService.getHandover(id)));
    }

    @GetMapping("/list-by-task")
    @Operation(summary = "获得某趟任务的全部交接登记", description = "集货时一家一条，按停靠点正序")
    @Parameter(name = "taskId", description = "运输任务编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_HANDOVER_QUERY + "')")
    public CommonResult<List<LogisticsTransportHandoverRespVO>> getHandoverListByTask(@RequestParam("taskId") Long taskId) {
        return success(logisticsTransportHandoverService.getHandoverListByTaskId(taskId).stream()
                .map(logisticsTransportHandoverService::toResp).toList());
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得交接登记")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.TRANSPORT_HANDOVER_QUERY + "')")
    public CommonResult<PageResult<LogisticsTransportHandoverRespVO>> getHandoverPage(
            @Valid LogisticsTransportHandoverPageReqVO reqVO) {
        PageResult<LogisticsTransportHandoverDO> page = logisticsTransportHandoverService.getPage(reqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(logisticsTransportHandoverService::toResp).toList(), page.getTotal()));
    }

}
