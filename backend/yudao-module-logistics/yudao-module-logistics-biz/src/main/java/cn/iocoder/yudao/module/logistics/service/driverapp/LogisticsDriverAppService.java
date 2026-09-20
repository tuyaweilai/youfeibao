package cn.iocoder.yudao.module.logistics.service.driverapp;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo.LogisticsTransportNodeReportReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.transporttask.vo.LogisticsTransportTaskPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.driver.LogisticsDriverDO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.transporttask.LogisticsTransportTaskDO;

import javax.validation.Valid;

/**
 * 司机端 Service（V2c #79）。
 *
 * <p><b>这个类是司机端的唯一入口，也是归属校验的唯一落点</b>：司机端的每个动作都先按登录账号
 * 找到他的司机档案，再强制把查询 / 操作范围限到「派给我的任务」。权限位只回答「他能不能用司机端」，
 * 「这是不是他的任务」由这里回答——两件事分开，才不会出现「有权限就能看别人的活」。
 *
 * <p>司机是回收企业建档的租户内账号（ADR 0032）：登录体系与收货员现场端同一套（`/admin-api` + token），
 * 不新建鉴权体系。他没有收购定稿、结算确认、付款开票的任何权限。
 */
public interface LogisticsDriverAppService {

    /**
     * 当前登录账号对应的司机档案；没建档就拦下来并给出可操作的提示。
     *
     * @return 司机档案
     */
    LogisticsDriverDO getCurrentDriver();

    /**
     * 派给我的任务分页（司机编号由登录账号决定，忽略入参里的 driverId）。
     */
    PageResult<LogisticsTransportTaskDO> getMyTaskPage(LogisticsTransportTaskPageReqVO pageReqVO);

    /**
     * 取我的一趟任务；不是我的就报「这不是派给你的任务」。
     */
    LogisticsTransportTaskDO getMyTask(Long taskId);

    /**
     * 接单（只能接自己的）。
     */
    void acceptMyTask(Long taskId);

    /**
     * 上报运输节点（只能报自己任务上的），幂等键与司机端一致。
     *
     * @return 节点编号
     */
    Long reportMyNode(@Valid LogisticsTransportNodeReportReqVO reportReqVO);

}
