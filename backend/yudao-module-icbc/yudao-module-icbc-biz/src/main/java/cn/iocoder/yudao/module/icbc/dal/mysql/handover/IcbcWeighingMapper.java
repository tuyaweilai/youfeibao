package cn.iocoder.yudao.module.icbc.dal.mysql.handover;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.handover.IcbcWeighingDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collections;
import java.util.List;

/**
 * 磅次 Mapper（#50 T12）。
 *
 * <p>同一批次至多一条 {@code effective = true}：指定有效磅次时先把其余置 false（一条 UPDATE），
 * 再置目标那一行为 true，避免并发下出现两个「有效磅次」。
 */
@Mapper
public interface IcbcWeighingMapper extends BaseMapperX<IcbcWeighingDO> {

    default List<IcbcWeighingDO> selectListByBatchId(Long batchId) {
        if (batchId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcWeighingDO>()
                .eq(IcbcWeighingDO::getBatchId, batchId)
                .orderByAsc(IcbcWeighingDO::getSeqNo)
                .orderByAsc(IcbcWeighingDO::getId));
    }

    /**
     * 该批次的有效磅次；没有指定过则返回 null（调用方必须拦住计量）。
     */
    default IcbcWeighingDO selectEffectiveByBatchId(Long batchId) {
        if (batchId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<IcbcWeighingDO>()
                .eq(IcbcWeighingDO::getBatchId, batchId)
                .eq(IcbcWeighingDO::getEffective, Boolean.TRUE)
                .orderByDesc(IcbcWeighingDO::getSeqNo));
    }

    default Long selectCountByBatchId(Long batchId) {
        if (batchId == null) {
            return 0L;
        }
        return selectCount(IcbcWeighingDO::getBatchId, batchId);
    }

    /**
     * 把该批次除 {@code exceptId} 之外的有效磅次全部置为无效。
     */
    default int clearEffective(Long batchId, Long exceptId) {
        return update(null, new LambdaUpdateWrapper<IcbcWeighingDO>()
                .set(IcbcWeighingDO::getEffective, Boolean.FALSE)
                .eq(IcbcWeighingDO::getBatchId, batchId)
                .eq(IcbcWeighingDO::getEffective, Boolean.TRUE)
                .ne(exceptId != null, IcbcWeighingDO::getId, exceptId));
    }

}
