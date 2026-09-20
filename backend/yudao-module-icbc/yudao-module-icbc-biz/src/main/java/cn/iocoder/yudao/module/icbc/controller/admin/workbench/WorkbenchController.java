package cn.iocoder.yudao.module.icbc.controller.admin.workbench;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.workbench.vo.WorkbenchOverviewRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.workbench.WorkbenchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 工作台（#56 T18）。
 *
 * <p>一屏给出该处理的八类待办与三条预警，每项带来源明细；开票就绪从一级菜单降级为这里的徽标。
 * 只读聚合，不写业务数据。
 */
@Tag(name = "管理后台 - 工作台")
@RestController
@RequestMapping("/icbc/workbench")
@Validated
public class WorkbenchController {

    @Resource
    private WorkbenchService workbenchService;

    @GetMapping("/overview")
    @Operation(summary = "工作台一屏", description = "八类待办与三条预警，每项带最多 10 条来源明细，另含开票就绪徽标")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.WORKBENCH_QUERY + "')")
    public CommonResult<WorkbenchOverviewRespVO> overview() {
        return success(workbenchService.getOverview());
    }

}
