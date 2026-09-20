package cn.iocoder.yudao.module.waste.convert.recycler;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigCreateReqVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigRespVO;
import cn.iocoder.yudao.module.waste.controller.admin.recycler.vo.RecyclerBusinessConfigUpdateReqVO;
import cn.iocoder.yudao.module.waste.dal.dataobject.recycler.RecyclerBusinessConfigDO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 回收企业业务模式配置 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface RecyclerBusinessConfigConvert {

    RecyclerBusinessConfigConvert INSTANCE = Mappers.getMapper(RecyclerBusinessConfigConvert.class);

    RecyclerBusinessConfigDO convert(RecyclerBusinessConfigCreateReqVO bean);

    RecyclerBusinessConfigDO convert(RecyclerBusinessConfigUpdateReqVO bean);

    RecyclerBusinessConfigRespVO convert(RecyclerBusinessConfigDO bean);

    List<RecyclerBusinessConfigRespVO> convertList(List<RecyclerBusinessConfigDO> list);

    PageResult<RecyclerBusinessConfigRespVO> convertPage(PageResult<RecyclerBusinessConfigDO> page);

} 