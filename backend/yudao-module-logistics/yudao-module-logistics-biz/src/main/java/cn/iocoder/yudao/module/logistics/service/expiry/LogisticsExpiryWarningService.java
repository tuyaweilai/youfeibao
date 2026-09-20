package cn.iocoder.yudao.module.logistics.service.expiry;

import cn.iocoder.yudao.module.logistics.controller.admin.expiry.vo.LogisticsExpiryWarningItemVO;

import java.util.List;

/**
 * 证件到期提醒 Service（V3 #70）。
 *
 * <p>四类证件：车辆行驶证、车辆保险、司机驾驶证、司机从业资格证。工作台按它显示「今天该处理什么」，
 * 与派车门禁共用同一套「过期」判定（{@code isDocumentExpired}），避免出现「提醒说没过期、派车却被拦」。
 */
public interface LogisticsExpiryWarningService {

    /**
     * 取即将到期与已过期的证件。
     *
     * @param days 提前多少天开始提醒（含；已过期的一律列出）
     * @return 扁平列表，按剩余天数升序（最快的排前面）
     */
    List<LogisticsExpiryWarningItemVO> getWarningList(int days);

}
