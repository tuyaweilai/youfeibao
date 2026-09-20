package cn.iocoder.yudao.module.logistics.service.driver;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.driver.vo.LogisticsDriverSaveReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;

import javax.validation.Valid;

/**
 * 司机档案 Service（V2a #77）。
 *
 * <p>司机是回收企业建档的租户内账号：{@code userId} 指向系统用户，司机用它登录司机端（V2c）。
 * 一个租户内一个用户只能建一份司机档案。
 */
public interface LogisticsDriverService {

    Long createDriver(@Valid LogisticsDriverSaveReqVO createReqVO);

    void updateDriver(@Valid LogisticsDriverSaveReqVO updateReqVO);

    void deleteDriver(Long id);

    /**
     * 获得司机；不存在时抛业务异常。
     *
     * @param id 司机编号
     * @return 司机
     */
    LogisticsDriverDO getDriver(Long id);

    /**
     * 按租户内系统用户获得司机档案；没有则返回 {@code null}。
     *
     * <p>司机端登录后要靠它把「登录的人」认成「哪个司机」（V2c）。
     *
     * @param userId 系统用户编号
     * @return 司机档案，可能为 null
     */
    LogisticsDriverDO getDriverByUserId(Long userId);

    /**
     * 获得可派的司机：**硬门禁**（在职）+ **软门禁**（驾驶证与从业资格证未过期）。
     */
    LogisticsDriverDO getAssignableDriver(Long driverId);

    /**
     * 同上，但**不查证件是否过期**——授权放行走这条路；**离职照样拦**。
     */
    LogisticsDriverDO getAssignableDriverAllowingExpiredDocuments(Long driverId);

    /**
     * 证件是否过期（驾驶证或从业资格证任一到期日早于今天）。
     */
    boolean isDocumentExpired(LogisticsDriverDO driver);

    PageResult<LogisticsDriverDO> getDriverPage(LogisticsDriverPageReqVO pageReqVO);

    /**
     * 导出司机列表（不分页）。
     */
    java.util.List<LogisticsDriverDO> getDriverList(LogisticsDriverPageReqVO exportReqVO);

}
