package cn.iocoder.yudao.module.icbc.convert.callback;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.callback.vo.CallbackNotifyRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.callback.CallbackNotifyDO;
import cn.iocoder.yudao.module.icbc.service.callback.CallbackNotifyService;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 工行回调通知 Convert
 *
 * @author 芋道源码
 */
@Mapper
public interface CallbackNotifyConvert {

    CallbackNotifyConvert INSTANCE = Mappers.getMapper(CallbackNotifyConvert.class);

    CallbackNotifyRespVO convert(CallbackNotifyDO bean);

    PageResult<CallbackNotifyRespVO> convertPage(PageResult<CallbackNotifyDO> page);

    CallbackNotifyDO convert(CallbackNotifyService.CallbackNotifyCreateReqVO bean);

} 