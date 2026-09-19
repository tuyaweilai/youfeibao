package cn.iocoder.yudao.module.icbc.service.station.impl;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicStationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.station.IcbcStationMapper;
import cn.iocoder.yudao.module.icbc.service.station.StationResolveRateLimiter;
import cn.iocoder.yudao.module.icbc.service.station.StationService;
import cn.iocoder.yudao.module.system.api.tenant.TenantApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.*;

/**
 * 场站 Service 实现（#34）。
 */
@Service
@Validated
@Slf4j
public class StationServiceImpl implements StationService {

    /** 在收货 */
    private static final Integer OPEN = 1;

    /** 自然人端入口地址（二维码指向它）；本地 / 联调可配，未配置时退化为相对链接 */
    @Value("${icbc.station.entry-url:}")
    private String entryUrl;

    @Resource
    private IcbcStationMapper stationMapper;
    @Resource
    private StationResolveRateLimiter rateLimiter;
    @Resource
    private TenantApi tenantApi;

    @Override
    public Long createStation(StationSaveReqVO createReqVO) {
        assertStationCodeAvailable(createReqVO.getStationCode(), null);
        IcbcStationDO station = BeanUtils.toBean(createReqVO, IcbcStationDO.class);
        stationMapper.insert(station);
        return station.getId();
    }

    @Override
    public void updateStation(StationSaveReqVO updateReqVO) {
        IcbcStationDO exists = getStation(updateReqVO.getId());
        assertStationCodeAvailable(updateReqVO.getStationCode(), exists.getId());
        IcbcStationDO update = BeanUtils.toBean(updateReqVO, IcbcStationDO.class);
        stationMapper.updateById(update);
    }

    @Override
    public void deleteStation(Long id) {
        getStation(id);
        stationMapper.deleteById(id);
    }

    @Override
    public IcbcStationDO getStation(Long id) {
        IcbcStationDO station = stationMapper.selectById(id);
        if (station == null) {
            throw exception(STATION_NOT_EXISTS);
        }
        return station;
    }

    @Override
    public PageResult<IcbcStationDO> getStationPage(StationPageReqVO pageReqVO) {
        return stationMapper.selectPage(pageReqVO);
    }

    @Override
    public PublicStationRespVO resolvePublic(String stationCode, String clientIp) {
        if (StrUtil.isBlank(stationCode)) {
            throw exception(STATION_CODE_REQUIRED);
        }
        rateLimiter.assertAllowed(clientIp);
        // 公开端点没有租户上下文，而 icbc_station 是租户表：显式让开租户拦截，
        // 按全局唯一的场站码读出记录，租户编号从记录里取（不猜、不从请求头取）。
        IcbcStationDO station = TenantUtils.executeIgnore(
                () -> stationMapper.selectByStationCode(stationCode.trim()));
        if (station == null) {
            throw exception(STATION_PUBLIC_NOT_FOUND);
        }
        PublicStationRespVO resp = new PublicStationRespVO();
        resp.setStationCode(station.getStationCode());
        resp.setStationId(station.getId());
        resp.setTenantId(station.getTenantId());
        resp.setEnterpriseName(enterpriseName(station.getTenantId()));
        resp.setStationName(station.getName());
        resp.setAddress(station.getAddress());
        boolean open = OPEN.equals(station.getOpenStatus());
        resp.setOpen(open);
        resp.setOpenStatusName(open ? "正在收货" : "暂停收货");
        resp.setContactMobile(station.getContactMobile());
        resp.setGuide(open
                ? List.of("本页只有公开信息，不含任何个人信息",
                          "要看「我的待确认」，请用手机号验证后查看")
                : List.of("本场站当前暂停收货，到站前请先电话确认"));
        return resp;
    }

    @Override
    public String buildEntryUrl(IcbcStationDO station) {
        String base = StrUtil.isBlank(entryUrl) ? "" : entryUrl.trim();
        if (StrUtil.isBlank(base)) {
            return "?station=" + station.getStationCode();
        }
        return base + (base.contains("?") ? "&" : "?") + "station=" + station.getStationCode();
    }

    private String enterpriseName(Long tenantId) {
        String name = tenantApi.getTenantName(tenantId);
        return StrUtil.isBlank(name) ? "回收企业" : name;
    }

    /**
     * 场站码全局唯一（与 uk_station_code 对齐）：跨租户重码会让二维码解析不出企业。
     */
    private void assertStationCodeAvailable(String stationCode, Long selfId) {
        if (StrUtil.isBlank(stationCode)) {
            throw exception(STATION_CODE_REQUIRED);
        }
        IcbcStationDO exists = TenantUtils.executeIgnore(
                () -> stationMapper.selectByStationCode(stationCode.trim()));
        if (exists != null && !Objects.equals(exists.getId(), selfId)) {
            throw exception(STATION_CODE_EXISTS);
        }
    }

}
