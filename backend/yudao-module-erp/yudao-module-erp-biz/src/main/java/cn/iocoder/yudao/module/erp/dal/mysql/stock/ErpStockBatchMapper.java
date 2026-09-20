package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch.ErpStockBatchPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 批次 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockBatchMapper extends BaseMapperX<ErpStockBatchDO> {

    default PageResult<ErpStockBatchDO> selectPage(ErpStockBatchPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpStockBatchDO>()
                .likeIfPresent(ErpStockBatchDO::getBatchNo, reqVO.getBatchNo())
                .eqIfPresent(ErpStockBatchDO::getGoodsConfigId, reqVO.getGoodsConfigId())
                .eqIfPresent(ErpStockBatchDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpStockBatchDO::getId));
    }

    default ErpStockBatchDO selectByBatchNo(String batchNo) {
        return selectOne(ErpStockBatchDO::getBatchNo, batchNo);
    }

    default List<ErpStockBatchDO> selectListByStatus(Integer status) {
        return selectList(ErpStockBatchDO::getStatus, status);
    }

}
