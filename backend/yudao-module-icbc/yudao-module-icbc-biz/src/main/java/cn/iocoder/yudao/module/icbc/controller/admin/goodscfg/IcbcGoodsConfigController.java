package cn.iocoder.yudao.module.icbc.controller.admin.goodscfg;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.service.goodscfg.IcbcGoodsConfigService;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
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
 * 管理后台 - 品类与税收分类编码配置
 */
@Tag(name = "管理后台 - 品类与税收分类编码配置")
@RestController
@RequestMapping("/icbc/goods-config")
@Validated
public class IcbcGoodsConfigController {

    @Resource
    private IcbcGoodsConfigService goodsConfigService;

    @PostMapping("/create")
    @Operation(summary = "创建品类配置")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.GOODS_CONFIG_CREATE + "')")
    public CommonResult<Long> create(@Valid @RequestBody IcbcGoodsConfigSaveReqVO createReqVO) {
        return success(goodsConfigService.createGoodsConfig(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新品类配置")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.GOODS_CONFIG_UPDATE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody IcbcGoodsConfigSaveReqVO updateReqVO) {
        goodsConfigService.updateGoodsConfig(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除品类配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.GOODS_CONFIG_DELETE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        goodsConfigService.deleteGoodsConfig(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得品类配置")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.GOODS_CONFIG_QUERY + "')")
    public CommonResult<IcbcGoodsConfigRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(goodsConfigService.getGoodsConfig(id), IcbcGoodsConfigRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得品类配置分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.GOODS_CONFIG_QUERY + "')")
    public CommonResult<PageResult<IcbcGoodsConfigRespVO>> page(@Valid IcbcGoodsConfigPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(goodsConfigService.getGoodsConfigPage(pageReqVO), IcbcGoodsConfigRespVO.class));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的品类列表")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.GOODS_CONFIG_QUERY + "')")
    public CommonResult<List<IcbcGoodsConfigRespVO>> enabledList() {
        return success(BeanUtils.toBean(goodsConfigService.getEnabledList(), IcbcGoodsConfigRespVO.class));
    }

}
