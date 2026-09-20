package cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDealRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDeliveryCheckReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderDeliveryCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderProgressRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSettingRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderSettingSaveReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.purchaseorder.vo.PurchaseOrderStatusUpdateReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.purchaseorder.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 采购订单（#46 T08）。
 *
 * <p>采购订单是采购**执行依据**（一个合同 → 多个订单 → 多次收货），与「到站预约」不是一回事：
 * 预约是交易对方声明的到场计划，订单是回收企业内部的采购计划。零散收购可以不挂订单。
 */
@Tag(name = "管理后台 - 采购订单")
@RestController
@RequestMapping("/icbc/purchase-order")
@Validated
public class PurchaseOrderController {

    @Resource
    private PurchaseOrderService purchaseOrderService;

    @PostMapping("/create")
    @Operation(summary = "创建采购订单（草稿）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_MANAGE + "')")
    public CommonResult<Long> create(@Valid @RequestBody PurchaseOrderSaveReqVO createReqVO) {
        return success(purchaseOrderService.createOrder(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改采购订单（只允许草稿）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_MANAGE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody PurchaseOrderSaveReqVO updateReqVO) {
        purchaseOrderService.updateOrder(updateReqVO);
        return success(true);
    }

    @PostMapping("/update-status")
    @Operation(summary = "采购订单状态流转（开始执行 / 暂停 / 恢复 / 完成 / 关闭）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_MANAGE + "')")
    public CommonResult<Boolean> updateStatus(@Valid @RequestBody PurchaseOrderStatusUpdateReqVO reqVO) {
        purchaseOrderService.updateStatus(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除采购订单（只允许草稿）")
    @Parameter(name = "id", description = "订单编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_MANAGE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        purchaseOrderService.deleteOrder(id);
        return success(true);
    }

    @PostMapping("/deal/create")
    @Operation(summary = "登记一次成交（留价格快照与调整原因）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_MANAGE + "')")
    public CommonResult<Long> createDeal(@Valid @RequestBody PurchaseOrderDealReqVO reqVO) {
        return success(purchaseOrderService.recordDeal(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得采购订单详情（含品类明细、已收量推导与价格表）")
    @Parameter(name = "id", description = "订单编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_QUERY + "')")
    public CommonResult<PurchaseOrderRespVO> get(@RequestParam("id") Long id) {
        return success(purchaseOrderService.getDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购订单分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_QUERY + "')")
    public CommonResult<PageResult<PurchaseOrderRespVO>> page(@Valid PurchaseOrderPageReqVO pageReqVO) {
        return success(purchaseOrderService.getOrderPage(pageReqVO));
    }

    @GetMapping("/progress")
    @Operation(summary = "获得采购订单执行进度（计划 / 验收 / 入库 / 结算 / 未履行五口径分列）")
    @Parameter(name = "id", description = "订单编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_QUERY + "')")
    public CommonResult<PurchaseOrderProgressRespVO> progress(@RequestParam("id") Long id) {
        return success(purchaseOrderService.getProgress(id));
    }

    @PostMapping("/delivery-check")
    @Operation(summary = "校验一次交货是否被允许（超量 / 过期 / 跨场站，按企业配置拦截或要求授权审核）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_QUERY + "')")
    public CommonResult<PurchaseOrderDeliveryCheckRespVO> deliveryCheck(
            @Valid @RequestBody PurchaseOrderDeliveryCheckReqVO reqVO) {
        return success(purchaseOrderService.checkDelivery(reqVO));
    }

    @GetMapping("/setting")
    @Operation(summary = "获得采购履约配置（完成比例口径与三类异常的处理方式）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_SETTING_QUERY + "')")
    public CommonResult<PurchaseOrderSettingRespVO> getSetting() {
        return success(purchaseOrderService.getSetting());
    }

    @PutMapping("/setting/update")
    @Operation(summary = "修改采购履约配置")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_SETTING_MANAGE + "')")
    public CommonResult<Boolean> updateSetting(@Valid @RequestBody PurchaseOrderSettingSaveReqVO reqVO) {
        purchaseOrderService.updateSetting(reqVO);
        return success(true);
    }

    @GetMapping("/deal/list")
    @Operation(summary = "获得采购订单成交记录（价格快照只追加）")
    @Parameter(name = "orderId", description = "订单编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_QUERY + "')")
    public CommonResult<List<PurchaseOrderDealRespVO>> dealList(@RequestParam("orderId") Long orderId) {
        return success(purchaseOrderService.getDealList(orderId));
    }

    @GetMapping("/resolve-price")
    @Operation(summary = "按明细与交货日算参考价（固定单价 / 按交货日价格表）")
    @Parameter(name = "itemId", description = "明细编号", required = true)
    @Parameter(name = "deliveryDate", description = "交货日")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PURCHASE_ORDER_QUERY + "')")
    public CommonResult<BigDecimal> resolvePrice(@RequestParam("itemId") Long itemId,
                                                 @RequestParam(value = "deliveryDate", required = false)
                                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate) {
        return success(purchaseOrderService.resolveUnitPrice(itemId, deliveryDate));
    }

}
