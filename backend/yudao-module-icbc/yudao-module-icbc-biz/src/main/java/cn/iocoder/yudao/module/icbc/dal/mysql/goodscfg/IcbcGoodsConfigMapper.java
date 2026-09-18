package cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 品类与税收分类编码配置 Mapper
 */
@Mapper
public interface IcbcGoodsConfigMapper extends BaseMapperX<IcbcGoodsConfigDO> {

    default PageResult<IcbcGoodsConfigDO> selectPage(IcbcGoodsConfigPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcGoodsConfigDO>()
                .likeIfPresent(IcbcGoodsConfigDO::getName, reqVO.getName())
                .eqIfPresent(IcbcGoodsConfigDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcGoodsConfigDO::getId));
    }

    default List<IcbcGoodsConfigDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<IcbcGoodsConfigDO>()
                .eq(IcbcGoodsConfigDO::getStatus, 0)
                .orderByDesc(IcbcGoodsConfigDO::getId));
    }

    default IcbcGoodsConfigDO selectByName(String name) {
        return selectOne(IcbcGoodsConfigDO::getName, name);
    }

    /**
     * 按税收分类合并编码查询品类配置。同一个编码可能被多条品类复用，取第一条即可。
     */
    default IcbcGoodsConfigDO selectByMergedCode(String mergedCode) {
        return selectList(new LambdaQueryWrapperX<IcbcGoodsConfigDO>()
                .eq(IcbcGoodsConfigDO::getMergedCode, mergedCode)
                .orderByAsc(IcbcGoodsConfigDO::getId))
                .stream().findFirst().orElse(null);
    }

}
