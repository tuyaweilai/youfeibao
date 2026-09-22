package cn.iocoder.yudao.module.icbc.service.station;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.publicapi.vo.PublicStationRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;

import javax.validation.Valid;

/**
 * 场站 Service（#34）。
 *
 * <p>两块职责刻意分开：
 * <ul>
 *   <li><b>企业侧</b>：管理员维护本租户的场站与场站码（二维码只编码场站码）；</li>
 *   <li><b>公开侧</b>：按场站码返回**只有公开信息**的首屏（回收企业、场站、地址、是否在收货）。</li>
 * </ul>
 */
public interface StationService {

    Long createStation(@Valid StationSaveReqVO createReqVO);

    void updateStation(@Valid StationSaveReqVO updateReqVO);

    void deleteStation(Long id);

    IcbcStationDO getStation(Long id);

    /**
     * 按编号查场站，**查不到返回 null**（不抛异常）。
     *
     * <p>用在「场站编号是客户端带来的提示」的场景（自然人端扫场站码进屋）：提示可能已过期
     * （场站被删、换过库、旧链接），过期提示不该让整个接口失败，调用方按「没带场站」处理即可。
     *
     * @param id 场站编号，可为 null
     * @return 场站，不存在时为 null
     */
    IcbcStationDO getStationOrNull(Long id);

    PageResult<IcbcStationDO> getStationPage(StationPageReqVO pageReqVO);

    /**
     * 免登录解析场站公开信息：只编码场站码，服务端解析出租户与公开字段。
     *
     * <p>读取限流（per IP），不返回任何个人数据。
     *
     * @param stationCode 场站码
     * @param clientIp    调用方 IP（限流键）
     * @return 场站公开信息
     */
    PublicStationRespVO resolvePublic(String stationCode, String clientIp);

    /**
     * 场站的二维码入口地址（用配置的 seller 端地址 + 场站码拼出）。
     */
    String buildEntryUrl(IcbcStationDO station);

}
