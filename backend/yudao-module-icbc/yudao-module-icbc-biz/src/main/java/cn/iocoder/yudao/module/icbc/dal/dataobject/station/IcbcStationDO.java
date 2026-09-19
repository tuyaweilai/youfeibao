package cn.iocoder.yudao.module.icbc.dal.dataobject.station;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 场站 DO（#34）。
 *
 * <p>场站是回收企业实际发生收购的固定场所，是交易地点与收货二维码的粒度。二维码一码一场站，
 * 码内**不带任何令牌**（公开且长期贴在磅房 / 墙上），只编码 {@code stationCode}，由服务端
 * 解析出企业与场站的公开信息（ADR 0017：免登录首屏只有公开信息）。
 */
@TableName("icbc_station")
@KeySequence("icbc_station_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcStationDO extends TenantBaseDO {

    /** 主键 */
    @TableId
    private Long id;

    /** 场站码（全局唯一，二维码只编码它；公开端点不带租户，须按码解析出企业） */
    private String stationCode;

    /** 场站名称 */
    private String name;

    /** 场站地址 */
    private String address;

    /** 场站联系电话（公开） */
    private String contactMobile;

    /** 是否在收货：1-在收货，0-暂停收货 */
    private Integer openStatus;

    /** 备注 */
    private String remark;

}
