package cn.iocoder.yudao.module.icbc.service.warning;

import cn.iocoder.yudao.module.icbc.dal.dataobject.warning.IcbcExpiryWarningDO;

import java.util.List;

/**
 * 资质到期预警 Service 接口。
 */
public interface IcbcExpiryWarningService {

    /**
     * 扫描本租户临近到期的资质，为尚未预警的资质生成一条待处理预警。
     *
     * <p>幂等：同一资质若已有未处理的预警，不会重复生成。供定时任务调用。
     *
     * @param days 提前预警的天数窗口
     * @return 本次新增的预警条数
     */
    int scan(int days);

    /**
     * 本租户待处理的预警列表
     */
    List<IcbcExpiryWarningDO> getOpenList();

    /**
     * 处理（关闭）一条预警
     */
    void acknowledge(Long id);

}
