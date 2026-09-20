package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifySummaryRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.enums.CallbackNotifyTypeEnum;
import cn.iocoder.yudao.module.icbc.enums.CallbackProcessStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.callback.CallbackNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营：工行通知监控与重放（#15）。
 *
 * <p>通知是工行推来的全局事件，不属于某一个回收企业租户，所以这里跨租户读取。
 * 只有平台运营角色（或超管）能进；重放失败的通知不会产生重复业务，已成功的通知也不会被重复处理。
 */
@Tag(name = "管理后台 - 平台运营：工行通知监控")
@RestController
@RequestMapping("/icbc/platform/callback")
@Validated
public class PlatformCallbackNotifyController {

    @Resource
    private CallbackNotifyService callbackNotifyService;

    @GetMapping("/page")
    @Operation(summary = "分页获得工行回调通知（跨租户）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CALLBACK_QUERY + "')")
    public CommonResult<PageResult<CallbackNotifyRespVO>> getPlatformCallbackNotifyPage(
            @Valid CallbackNotifyPageReqVO pageReqVO) {
        PageResult<CallbackNotifyDO> page = callbackNotifyService.getPlatformCallbackNotifyPage(pageReqVO);
        List<CallbackNotifyRespVO> list = page.getList().stream()
                .map(PlatformCallbackNotifyController::toRespVO)
                .collect(Collectors.toList());
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/get")
    @Operation(summary = "获得工行回调通知详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CALLBACK_QUERY + "')")
    public CommonResult<CallbackNotifyRespVO> getPlatformCallbackNotify(@RequestParam("id") Long id) {
        return success(toRespVO(callbackNotifyService.getCallbackNotify(id)));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得九类通知的处理结果概览（跨租户）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CALLBACK_QUERY + "')")
    public CommonResult<CallbackNotifySummaryRespVO> getPlatformCallbackNotifySummary() {
        return success(callbackNotifyService.getPlatformCallbackNotifySummary());
    }

    @PostMapping("/replay")
    @Operation(summary = "重放处理失败的通知")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_CALLBACK_RETRY + "')")
    public CommonResult<Boolean> replayCallback(@RequestParam("id") Long id) {
        callbackNotifyService.replay(id);
        return success(true);
    }

    /**
     * 把处理结果翻译成人能看懂的名字，并给出失败时的下一步。
     * 入参为 null（记录不存在）时返回 null，由调用方决定是否报错。
     */
    static CallbackNotifyRespVO toRespVO(CallbackNotifyDO record) {
        if (record == null) {
            return null;
        }
        CallbackNotifyRespVO resp = new CallbackNotifyRespVO();
        resp.setId(record.getId());
        resp.setNotifyId(record.getNotifyId());
        resp.setNotifyType(record.getNotifyType());
        CallbackNotifyTypeEnum type = CallbackNotifyTypeEnum.of(record.getNotifyType());
        resp.setNotifyTypeName(type != null ? type.getName() : "未知");
        resp.setBusinessName(CallbackNotifyTypeEnum.businessNameOf(record.getNotifyType()));
        resp.setBusinessId(record.getBusinessId());
        resp.setNotifyData(record.getNotifyData());
        resp.setSign(record.getSign());
        resp.setProcessStatus(record.getProcessStatus());
        resp.setProcessStatusName(CallbackProcessStatusEnum.nameOf(record.getProcessStatus()));
        resp.setProcessMsg(record.getProcessMsg());
        resp.setProcessTime(record.getProcessTime());
        resp.setRetryCount(record.getRetryCount());
        resp.setCreateTime(record.getCreateTime());
        resp.setNextAction(nextActionOf(record.getProcessStatus()));
        return resp;
    }

    private static String nextActionOf(Integer processStatus) {
        if (CallbackProcessStatusEnum.FAILURE.getStatus().equals(processStatus)) {
            return "处理失败，可查看失败原因后手动重放";
        }
        if (CallbackProcessStatusEnum.PENDING.getStatus().equals(processStatus)) {
            return "待处理，稍后可重放触发处理";
        }
        return null;
    }

}
