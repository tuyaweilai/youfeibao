package cn.iocoder.yudao.module.icbc.controller.admin.station;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.service.station.StationService;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 场站与场站二维码（#34）。
 *
 * <p>场站是收货二维码的粒度：一码一场站，码内不带任何令牌（公开且长期贴），只编码场站码。
 * 企业维护名称、地址与「是否在收货」，这些是扫码首屏唯一的公开信息（不含个人数据）。
 */
@Tag(name = "管理后台 - 场站与场站二维码")
@RestController
@RequestMapping("/icbc/station")
@Validated
public class StationController {

    @Resource
    private StationService stationService;

    @PostMapping("/create")
    @Operation(summary = "创建场站")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STATION_MANAGE + "')")
    public CommonResult<Long> create(@Valid @RequestBody StationSaveReqVO createReqVO) {
        return success(stationService.createStation(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新场站（含是否在收货）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STATION_MANAGE + "')")
    public CommonResult<Boolean> update(@Valid @RequestBody StationSaveReqVO updateReqVO) {
        stationService.updateStation(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除场站")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STATION_MANAGE + "')")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        stationService.deleteStation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得场站")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STATION_QUERY + "')")
    public CommonResult<StationRespVO> get(@RequestParam("id") Long id) {
        return success(toResp(stationService.getStation(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得场站分页")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STATION_QUERY + "')")
    public CommonResult<PageResult<StationRespVO>> page(@Valid StationPageReqVO pageReqVO) {
        PageResult<IcbcStationDO> page = stationService.getStationPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream().map(this::toResp).toList(),
                page.getTotal()));
    }

    private StationRespVO toResp(IcbcStationDO station) {
        StationRespVO resp = BeanUtils.toBean(station, StationRespVO.class);
        resp.setEntryUrl(stationService.buildEntryUrl(station));
        return resp;
    }

}
