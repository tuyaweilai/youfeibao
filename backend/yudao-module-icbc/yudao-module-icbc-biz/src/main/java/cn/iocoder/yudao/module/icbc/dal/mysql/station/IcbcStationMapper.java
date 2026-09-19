package cn.iocoder.yudao.module.icbc.dal.mysql.station;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.controller.admin.station.vo.StationPageReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.station.IcbcStationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 场站 Mapper。
 */
@Mapper
public interface IcbcStationMapper extends BaseMapperX<IcbcStationDO> {

    /**
     * 按场站码查询（租户内唯一）。
     */
    default IcbcStationDO selectByStationCode(String stationCode) {
        return selectOne(IcbcStationDO::getStationCode, stationCode);
    }

    default PageResult<IcbcStationDO> selectPage(StationPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<IcbcStationDO>()
                .likeIfPresent(IcbcStationDO::getName, reqVO.getName())
                .eqIfPresent(IcbcStationDO::getStationCode, reqVO.getStationCode())
                .eqIfPresent(IcbcStationDO::getOpenStatus, reqVO.getOpenStatus())
                .orderByDesc(IcbcStationDO::getId));
    }

}
