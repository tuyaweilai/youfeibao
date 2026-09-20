package cn.iocoder.yudao.module.icbc.dal.mysql.purchaseorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.purchaseorder.IcbcPurchaseSettingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 采购履约配置 Mapper（#47 T09）。一个租户一行；没配过时返回 null，由 Service 给默认值。
 */
@Mapper
public interface IcbcPurchaseSettingMapper extends BaseMapperX<IcbcPurchaseSettingDO> {

    default IcbcPurchaseSettingDO selectSetting() {
        List<IcbcPurchaseSettingDO> list = selectList(new LambdaQueryWrapperX<IcbcPurchaseSettingDO>()
                .orderByAsc(IcbcPurchaseSettingDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }

}
