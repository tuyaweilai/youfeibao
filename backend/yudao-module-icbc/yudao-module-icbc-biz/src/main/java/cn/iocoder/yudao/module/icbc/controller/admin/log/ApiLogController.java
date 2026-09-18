package cn.iocoder.yudao.module.icbc.controller.admin.log;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.log.ApiLogDO;
import cn.iocoder.yudao.module.icbc.service.log.ApiLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 工行接口调用日志")
@RestController
@RequestMapping("/icbc/api-log")
@Validated
public class ApiLogController {

    @Resource
    private ApiLogService apiLogService;

    @GetMapping("/page")
    @Operation(summary = "获得工行接口调用日志分页")
    @PreAuthorize("@icbc.hasPermission('icbc:api-log:query')")
    public CommonResult<PageResult<ApiLogRespVO>> getApiLogPage(@Valid ApiLogPageReqVO pageReqVO) {
        PageResult<ApiLogDO> pageResult = apiLogService.getApiLogPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ApiLogRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得工行接口调用日志")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('icbc:api-log:query')")
    public CommonResult<ApiLogRespVO> getApiLog(@RequestParam("id") Long id) {
        ApiLogDO apiLog = apiLogService.getApiLog(id);
        return success(BeanUtils.toBean(apiLog, ApiLogRespVO.class));
    }

} 