package cn.iocoder.yudao.module.icbc.dal.mysql.scrapcode;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.scrapcode.vo.IcbcScrapCodePageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.scrapcode.IcbcScrapCodeDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 平台级报废产品税收分类编码 Mapper
 */
@Mapper
public interface IcbcScrapCodeMapper extends BaseMapperX<IcbcScrapCodeDO> {

    default PageResult<IcbcScrapCodeDO> selectPage(IcbcScrapCodePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcScrapCodeDO>()
                .likeIfPresent(IcbcScrapCodeDO::getName, reqVO.getName())
                .likeIfPresent(IcbcScrapCodeDO::getMergedCode, reqVO.getMergedCode())
                .eqIfPresent(IcbcScrapCodeDO::getStatus, reqVO.getStatus())
                .orderByDesc(IcbcScrapCodeDO::getId));
    }

    default List<IcbcScrapCodeDO> selectEnabledList() {
        return selectList(new LambdaQueryWrapperX<IcbcScrapCodeDO>()
                .eq(IcbcScrapCodeDO::getStatus, 0)
                .orderByDesc(IcbcScrapCodeDO::getId));
    }

    /**
     * 按税收分类合并编码查询品类配置。同一个编码可能被多条品类复用，取第一条即可。
     */
    default IcbcScrapCodeDO selectByMergedCode(String mergedCode) {
        return selectList(new LambdaQueryWrapperX<IcbcScrapCodeDO>()
                .eq(IcbcScrapCodeDO::getMergedCode, mergedCode)
                .orderByAsc(IcbcScrapCodeDO::getId))
                .stream().findFirst().orElse(null);
    }

}
