package cn.iocoder.yudao.module.icbc.dal.mysql.qualification;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.qualification.vo.IcbcQualificationPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.qualification.IcbcQualificationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 租户三层资质 Mapper
 */
@Mapper
public interface IcbcQualificationMapper extends BaseMapperX<IcbcQualificationDO> {

    default PageResult<IcbcQualificationDO> selectPage(IcbcQualificationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcQualificationDO>()
                .eqIfPresent(IcbcQualificationDO::getType, reqVO.getType())
                .likeIfPresent(IcbcQualificationDO::getName, reqVO.getName())
                .eqIfPresent(IcbcQualificationDO::getStatus, reqVO.getStatus())
                .orderByAsc(IcbcQualificationDO::getType)
                .orderByDesc(IcbcQualificationDO::getId));
    }

    default List<IcbcQualificationDO> selectList() {
        return selectList(new LambdaQueryWrapperX<IcbcQualificationDO>().orderByAsc(IcbcQualificationDO::getType));
    }

}
