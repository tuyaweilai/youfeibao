package cn.iocoder.yudao.module.waste.controller.admin.payment;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.waste.controller.admin.payment.vo.*;
import cn.iocoder.yudao.module.waste.dal.dataobject.payment.ProducerPaymentConfigDO;
import cn.iocoder.yudao.module.waste.service.payment.ProducerPaymentConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 产废企业付款配置")
@RestController
@RequestMapping("/waste/producer-payment-config")
@Validated
public class ProducerPaymentConfigController {

    @Resource
    private ProducerPaymentConfigService producerPaymentConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建产废企业付款配置")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:create')")
    public CommonResult<Long> createProducerPaymentConfig(@Valid @RequestBody ProducerPaymentConfigCreateReqVO createReqVO) {
        return success(producerPaymentConfigService.createProducerPaymentConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新产废企业付款配置")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:update')")
    public CommonResult<Boolean> updateProducerPaymentConfig(@Valid @RequestBody ProducerPaymentConfigUpdateReqVO updateReqVO) {
        producerPaymentConfigService.updateProducerPaymentConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产废企业付款配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:delete')")
    public CommonResult<Boolean> deleteProducerPaymentConfig(@RequestParam("id") Long id) {
        producerPaymentConfigService.deleteProducerPaymentConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得产废企业付款配置")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:query')")
    public CommonResult<ProducerPaymentConfigRespVO> getProducerPaymentConfig(@RequestParam("id") Long id) {
        ProducerPaymentConfigDO producerPaymentConfig = producerPaymentConfigService.getProducerPaymentConfig(id);
        return success(BeanUtils.toBean(producerPaymentConfig, ProducerPaymentConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得产废企业付款配置分页")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:query')")
    public CommonResult<PageResult<ProducerPaymentConfigRespVO>> getProducerPaymentConfigPage(@Valid ProducerPaymentConfigPageReqVO pageReqVO) {
        PageResult<ProducerPaymentConfigDO> pageResult = producerPaymentConfigService.getProducerPaymentConfigPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ProducerPaymentConfigRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产废企业付款配置 Excel")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProducerPaymentConfigExcel(@Valid ProducerPaymentConfigPageReqVO pageReqVO,
                                                  HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ProducerPaymentConfigDO> list = producerPaymentConfigService.getProducerPaymentConfigPage(pageReqVO).getList();
        // 导出 Excel
        ExcelUtils.write(response, "产废企业付款配置.xls", "数据", ProducerPaymentConfigRespVO.class,
                BeanUtils.toBean(list, ProducerPaymentConfigRespVO.class));
    }

    // ==================== 业务方法 ====================

    @PostMapping("/set-default")
    @Operation(summary = "设置默认配置")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:update')")
    public CommonResult<Boolean> setDefaultConfig(@RequestParam("enterpriseId") Long enterpriseId,
                                                  @RequestParam("configId") Long configId) {
        producerPaymentConfigService.setDefaultConfig(enterpriseId, configId);
        return success(true);
    }

    @GetMapping("/by-enterprise")
    @Operation(summary = "根据企业ID获取付款配置")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:query')")
    public CommonResult<List<ProducerPaymentConfigRespVO>> getConfigsByEnterpriseId(@RequestParam("enterpriseId") Long enterpriseId) {
        List<ProducerPaymentConfigDO> list = producerPaymentConfigService.getConfigsByEnterpriseId(enterpriseId);
        return success(BeanUtils.toBean(list, ProducerPaymentConfigRespVO.class));
    }

    @GetMapping("/default-config")
    @Operation(summary = "获取企业默认付款配置")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:query')")
    public CommonResult<ProducerPaymentConfigRespVO> getDefaultConfig(@RequestParam("enterpriseId") Long enterpriseId) {
        ProducerPaymentConfigDO config = producerPaymentConfigService.getDefaultConfig(enterpriseId);
        return success(BeanUtils.toBean(config, ProducerPaymentConfigRespVO.class));
    }

    @GetMapping("/by-payment-method")
    @Operation(summary = "根据付款方式获取配置")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:query')")
    public CommonResult<List<ProducerPaymentConfigRespVO>> getConfigsByPaymentMethod(@RequestParam("enterpriseId") Long enterpriseId,
                                                                                     @RequestParam("paymentMethod") Integer paymentMethod) {
        List<ProducerPaymentConfigDO> list = producerPaymentConfigService.getConfigsByPaymentMethod(enterpriseId, paymentMethod);
        return success(BeanUtils.toBean(list, ProducerPaymentConfigRespVO.class));
    }

    @PutMapping("/batch-update-status")
    @Operation(summary = "批量更新配置状态")
    @PreAuthorize("@ss.hasPermission('waste:producer-payment-config:update')")
    public CommonResult<Boolean> batchUpdateStatus(@RequestParam("ids") List<Long> ids,
                                                   @RequestParam("status") Integer status) {
        producerPaymentConfigService.batchUpdateStatus(ids, status);
        return success(true);
    }

} 