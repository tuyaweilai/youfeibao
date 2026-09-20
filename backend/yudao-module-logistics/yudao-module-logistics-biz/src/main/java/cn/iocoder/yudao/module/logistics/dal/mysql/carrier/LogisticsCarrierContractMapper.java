package cn.iocoder.yudao.module.logistics.dal.mysql.carrier;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo.LogisticsCarrierContractPageReqVO;
import cn.iocoder.yudao.module.logistics.dal.dataobject.carrier.LogisticsCarrierContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 承运合同 Mapper（V8 #75）。分页与导出共用同一条件。
 */
@Mapper
public interface LogisticsCarrierContractMapper extends BaseMapperX<LogisticsCarrierContractDO> {

    /**
     * 按合同编号查询（租户内唯一；租户由框架自动加条件）。
     */
    default LogisticsCarrierContractDO selectByContractNo(String contractNo) {
        return selectOne(LogisticsCarrierContractDO::getContractNo, contractNo);
    }

    /**
     * 按承运商取合同列表（按生效日期倒序），汇集运费时校验合同归属用。
     */
    default List<LogisticsCarrierContractDO> selectListByCarrierId(Long carrierId) {
        return selectList(new LambdaQueryWrapperX<LogisticsCarrierContractDO>()
                .eq(LogisticsCarrierContractDO::getCarrierId, carrierId)
                .orderByDesc(LogisticsCarrierContractDO::getEffectiveFrom)
                .orderByDesc(LogisticsCarrierContractDO::getId));
    }

    default PageResult<LogisticsCarrierContractDO> selectPage(LogisticsCarrierContractPageReqVO reqVO) {
        return selectPage(reqVO, buildQuery(reqVO));
    }

    default List<LogisticsCarrierContractDO> selectList(LogisticsCarrierContractPageReqVO reqVO) {
        return selectList(buildQuery(reqVO));
    }

    default LambdaQueryWrapperX<LogisticsCarrierContractDO> buildQuery(LogisticsCarrierContractPageReqVO reqVO) {
        return new LambdaQueryWrapperX<LogisticsCarrierContractDO>()
                .likeIfPresent(LogisticsCarrierContractDO::getContractNo, reqVO.getContractNo())
                .eqIfPresent(LogisticsCarrierContractDO::getCarrierId, reqVO.getCarrierId())
                .likeIfPresent(LogisticsCarrierContractDO::getCarrierName, reqVO.getCarrierName())
                .likeIfPresent(LogisticsCarrierContractDO::getRoute, reqVO.getRoute())
                .eqIfPresent(LogisticsCarrierContractDO::getGoodsConfigId, reqVO.getGoodsConfigId())
                .eqIfPresent(LogisticsCarrierContractDO::getBillingMode, reqVO.getBillingMode())
                .eqIfPresent(LogisticsCarrierContractDO::getStatus, reqVO.getStatus())
                .orderByDesc(LogisticsCarrierContractDO::getId);
    }

}
