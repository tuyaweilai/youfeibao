package cn.iocoder.yudao.module.icbc.controller.admin.scrapcode;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodeRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodeSaveReqVO;
import cn.iocoder.yudao.module.icbc.service.scrapcode.IcbcScrapCodeService;
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
 * 管理后台 - 平台级报废产品税收分类编码表。
 *
 * <p>平台运营维护，租户只读（{@code /enabled-list}）。
 */
@Tag(name = "管理后台 - 平台级报废产品编码表")
@RestController
@RequestMapping("/icbc/scrap-code")
@Validated
public class IcbcScrapCodeController {

    @Resource
    private IcbcScrapCodeService scrapCodeService;

    @PostMapping("/create")
    @Operation(summary = "创建报废产品编码")
    @PreAuthorize("@icbc.hasPermission('icbc:scrap-code:create')")
    public CommonResult<Long> create(@Valid @RequestBody IcbcScrapCodeSaveReqVO createReqVO) {
        return success(scrapCodeService.createScrapCode(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报废产品编码")
    @PreAuthorize("@icbc.hasPermission('icbc:scrap-code:update')")
    public CommonResult<Boolean> update(@Valid @RequestBody IcbcScrapCodeSaveReqVO updateReqVO) {
        scrapCodeService.updateScrapCode(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报废产品编码")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:scrap-code:delete')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        scrapCodeService.deleteScrapCode(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报废产品编码")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:scrap-code:query')")
    public CommonResult<IcbcScrapCodeRespVO> get(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(scrapCodeService.getScrapCode(id), IcbcScrapCodeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报废产品编码分页")
    @PreAuthorize("@icbc.hasPermission('icbc:scrap-code:query')")
    public CommonResult<PageResult<IcbcScrapCodeRespVO>> page(@Valid IcbcScrapCodePageReqVO pageReqVO) {
        return success(BeanUtils.toBean(scrapCodeService.getScrapCodePage(pageReqVO), IcbcScrapCodeRespVO.class));
    }

    @GetMapping("/enabled-list")
    @Operation(summary = "获得启用的报废产品编码列表（租户选编码用）")
    @PreAuthorize("@icbc.hasPermission('icbc:scrap-code:query')")
    public CommonResult<List<IcbcScrapCodeRespVO>> enabledList() {
        return success(BeanUtils.toBean(scrapCodeService.getEnabledList(), IcbcScrapCodeRespVO.class));
    }

}
