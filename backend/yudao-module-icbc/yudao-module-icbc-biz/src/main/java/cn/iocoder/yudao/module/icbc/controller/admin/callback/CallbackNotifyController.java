package cn.iocoder.yudao.module.icbc.controller.admin.callback;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.service.callback.CallbackNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StreamUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 工行异步通知入口。
 *
 * <p>九类通知走同一个入口 {@code POST /icbc/callback/notify}：先落表，再处理。
 * 处理结果与重放在平台运营侧查看（{@code /icbc/platform/callback}）。
 */
@Tag(name = "管理后台 - 工行回调通知入口")
@RestController
@RequestMapping("/icbc/callback")
@Validated
@Slf4j
public class CallbackNotifyController {

    @Resource
    private CallbackNotifyService callbackNotifyService;

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

}
