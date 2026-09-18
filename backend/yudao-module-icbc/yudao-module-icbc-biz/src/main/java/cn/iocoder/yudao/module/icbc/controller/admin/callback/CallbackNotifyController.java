package cn.iocoder.yudao.module.icbc.controller.admin.callback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.service.callback.CallbackNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工行回调通知")
@RestController
@RequestMapping("/icbc/callback")
@Validated
@Slf4j
public class CallbackNotifyController {

    @Resource
    private CallbackNotifyService callbackNotifyService;

    @GetMapping("/page")
    @Operation(summary = "获得工行回调通知分页")
    @PreAuthorize("@ss.hasPermission('icbc:callback:query')")
    public CommonResult<PageResult<CallbackNotifyRespVO>> getCallbackNotifyPage(@Valid CallbackNotifyPageReqVO pageReqVO) {
        PageResult<CallbackNotifyDO> pageResult = callbackNotifyService.getCallbackNotifyPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, CallbackNotifyRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得工行回调通知")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('icbc:callback:query')")
    public CommonResult<CallbackNotifyRespVO> getCallbackNotify(@RequestParam("id") Long id) {
        CallbackNotifyDO callbackNotify = callbackNotifyService.getCallbackNotify(id);
        return success(BeanUtils.toBean(callbackNotify, CallbackNotifyRespVO.class));
    }

    @PostMapping("/notify")
    @Operation(summary = "接收工行回调通知")
    public CommonResult<String> receiveNotify(@RequestParam("notifyId") String notifyId,
                                              @RequestParam("notifyType") String notifyType,
                                              @RequestParam("businessId") String businessId,
                                              @RequestParam("notifyData") String notifyData,
                                              @RequestParam(value = "sign", required = false) String sign) {
        log.info("接收工行回调通知，notifyId: {}, notifyType: {}, businessId: {}", notifyId, notifyType, businessId);
        
        String result = callbackNotifyService.processCallback(notifyId, notifyType, businessId, notifyData, sign);
        return success(result);
    }

    @PostMapping("/retry")
    @Operation(summary = "重试回调通知")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('icbc:callback:retry')")
    public CommonResult<Boolean> retryCallback(@RequestParam("id") Long id) {
        callbackNotifyService.retryCallback(id);
        return success(true);
    }

} 