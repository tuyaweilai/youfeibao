package cn.iocoder.yudao.module.icbc.dal.mysql.cardrecognition;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.cardrecognition.IcbcCardRecognitionConfigDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 卡证识别平台级参数 Mapper（#103）。
 *
 * <p>平台级唯一一份：表是全局表，只该有一行；查询一律取第一条，保存一律原地更新。
 */
@Mapper
public interface IcbcCardRecognitionConfigMapper extends BaseMapperX<IcbcCardRecognitionConfigDO> {

    /**
     * 平台参数（唯一一份）；没配置过时返回 {@code null}。
     */
    default IcbcCardRecognitionConfigDO selectConfig() {
        return selectOne(new LambdaQueryWrapperX<IcbcCardRecognitionConfigDO>()
                .orderByAsc(IcbcCardRecognitionConfigDO::getId)
                .last("LIMIT 1"));
    }

}
