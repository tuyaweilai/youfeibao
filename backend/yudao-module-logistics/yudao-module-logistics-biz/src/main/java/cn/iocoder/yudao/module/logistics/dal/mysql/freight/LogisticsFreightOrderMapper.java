package cn.iocoder.yudao.module.logistics.dal.mysql.freight;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightPageReqVO;
import cn.iocoder.yudao.module.logistics.controller.admin.freight.vo.LogisticsFreightReconciliationReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.freight.LogisticsFreightOrderDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 承运商运费单 Mapper（V8 #75）。
 *
 * <p>「一趟一张」：{@link #selectByTaskId} 返回该任务唯一的运费单（没有返回 null）。
 * 分页与对账汇总共用同一套过滤条件。
 */
@Mapper
public interface LogisticsFreightOrderMapper extends BaseMapperX<LogisticsFreightOrderDO> {

    default LogisticsFreightOrderDO selectByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return selectOne(LogisticsFreightOrderDO::getTaskId, taskId);
    }

    default LogisticsFreightOrderDO selectByFreightNo(String freightNo) {
        return selectOne(LogisticsFreightOrderDO::getFreightNo, freightNo);
    }

    default PageResult<LogisticsFreightOrderDO> selectPage(LogisticsFreightPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<LogisticsFreightOrderDO> selectList(LogisticsFreightPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    /**
     * 对账汇总用：按「承运商 + 合同 + 时间」过滤后全量取出，在服务层聚合。
     */
    default List<LogisticsFreightOrderDO> selectList(LogisticsFreightReconciliationReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<LogisticsFreightOrderDO>()
                .eqIfPresent(LogisticsFreightOrderDO::getCarrierId, reqVO.getCarrierId())
                .likeIfPresent(LogisticsFreightOrderDO::getCarrierName, reqVO.getCarrierName())
                .eqIfPresent(LogisticsFreightOrderDO::getContractId, reqVO.getContractId())
                .eqIfPresent(LogisticsFreightOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(LogisticsFreightOrderDO::getCreateTime,
                        reqVO.getCreateTimeBegin(), reqVO.getCreateTimeEnd())
                .orderByAsc(LogisticsFreightOrderDO::getCarrierId)
                .orderByAsc(LogisticsFreightOrderDO::getContractId)
                .orderByAsc(LogisticsFreightOrderDO::getId));
    }

    default LambdaQueryWrapperX<LogisticsFreightOrderDO> buildQuery(LogisticsFreightPageReqVO reqVO) {
        return new LambdaQueryWrapperX<LogisticsFreightOrderDO>()
                .likeIfPresent(LogisticsFreightOrderDO::getFreightNo, reqVO.getFreightNo())
                .likeIfPresent(LogisticsFreightOrderDO::getTaskNo, reqVO.getTaskNo())
                .eqIfPresent(LogisticsFreightOrderDO::getCarrierId, reqVO.getCarrierId())
                .likeIfPresent(LogisticsFreightOrderDO::getCarrierName, reqVO.getCarrierName())
                .eqIfPresent(LogisticsFreightOrderDO::getContractId, reqVO.getContractId())
                .eqIfPresent(LogisticsFreightOrderDO::getStatus, reqVO.getStatus())
                .betweenIfPresent(LogisticsFreightOrderDO::getCreateTime,
                        reqVO.getCreateTimeBegin(), reqVO.getCreateTimeEnd())
                .orderByDesc(LogisticsFreightOrderDO::getId);
    }

}
