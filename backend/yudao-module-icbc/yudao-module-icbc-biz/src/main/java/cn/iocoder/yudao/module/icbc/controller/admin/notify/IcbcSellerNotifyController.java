package cn.iocoder.yudao.module.icbc.controller.admin.notify;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.notify.vo.*;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.notify.SellerNotifyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 / 收货员端 - 出售者触达（#36，ADR 0023）。
 *
 * <p>一期触达只有两条路：短信（平台 / 租户开关，默认关闭）与**收货员一键把确认链接转达给他**。
 * 首次交易、从未留手机号的场景只有转达这一条通路，所以它是必需功能。
 */
@Tag(name = "管理后台 - 出售者触达")
@RestController
@RequestMapping("/icbc/notify")
@Validated
public class IcbcSellerNotifyController {

    @Resource
    private SellerNotifyService sellerNotifyService;

    @GetMapping("/page")
    @Operation(summary = "触达记录分页", description = "看每条触达发没发出去、为什么没发出去")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_NOTIFY_QUERY + "')")
    public CommonResult<PageResult<NotifyRespVO>> page(@Valid NotifyPageReqVO pageReqVO) {
        return success(sellerNotifyService.getNotifyPage(pageReqVO));
    }

    @GetMapping("/setting")
    @Operation(summary = "查看触达设置")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_NOTIFY_QUERY + "')")
    public CommonResult<NotifySettingRespVO> setting() {
        return success(sellerNotifyService.getSetting());
    }

    @PutMapping("/setting")
    @Operation(summary = "开启 / 关闭本租户短信触达")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_NOTIFY_MANAGE + "')")
    public CommonResult<Boolean> saveSetting(@Valid @RequestBody NotifySettingSaveReqVO reqVO) {
        sellerNotifyService.saveSetting(reqVO);
        return success(true);
    }

    @PostMapping("/settlement/forward-link")
    @Operation(summary = "一键把结算确认链接转达给出售者",
            description = "返回一次性令牌链接与可直接复制的短信文案；sendSms=true 时顺带发短信，由人显式触发")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.SELLER_NOTIFY_MANAGE + "')")
    public CommonResult<NotifyForwardLinkRespVO> forwardLink(@Valid @RequestBody NotifyForwardLinkReqVO reqVO) {
        return success(sellerNotifyService.forwardSettlementLink(reqVO));
    }

}
