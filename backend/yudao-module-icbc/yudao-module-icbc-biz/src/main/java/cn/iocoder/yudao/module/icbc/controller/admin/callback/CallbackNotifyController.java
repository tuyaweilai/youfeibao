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
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工行回调通知
 *
 * 九类异步通知走同一个入口 {@code POST /icbc/callback/notify}：先落表，再处理。
 */
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

    /**
     * 工行异步通知唯一入口
     *
     * 支持两种投递：JSON body（外层 {@code {notifyData, signData}}）与表单参数 {@code biz_content}。
     * 通知先落表再处理，投递失败不影响记录，可重放。
     */
    @PostMapping("/notify")
    @Operation(summary = "接收工行回调通知")
    public CommonResult<String> receiveNotify(HttpServletRequest request) throws IOException {
        String bizContent = request.getParameter("biz_content");
        String body = bizContent != null ? bizContent
                : StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
        log.info("接收工行回调通知，报文字节数: {}", body == null ? 0 : body.length());
        String result = callbackNotifyService.receive(body);
        return success(result);
    }

    @PostMapping("/replay")
    @Operation(summary = "重放回调通知")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('icbc:callback:retry')")
    public CommonResult<Boolean> replayCallback(@RequestParam("id") Long id) {
        callbackNotifyService.replay(id);
        return success(true);
    }

}
