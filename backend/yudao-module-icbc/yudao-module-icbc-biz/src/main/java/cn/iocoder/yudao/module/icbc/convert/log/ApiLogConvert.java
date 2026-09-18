package cn.iocoder.yudao.module.icbc.convert.log;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.log.vo.ApiLogRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.log.ApiLogDO;
import cn.iocoder.yudao.module.icbc.service.log.ApiLogService;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 工行接口调用日志 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface ApiLogConvert {

    ApiLogConvert INSTANCE = Mappers.getMapper(ApiLogConvert.class);

    ApiLogRespVO convert(ApiLogDO bean);

    PageResult<ApiLogRespVO> convertPage(PageResult<ApiLogDO> page);

    ApiLogDO convert(ApiLogService.ApiLogCreateReqVO bean);

} 