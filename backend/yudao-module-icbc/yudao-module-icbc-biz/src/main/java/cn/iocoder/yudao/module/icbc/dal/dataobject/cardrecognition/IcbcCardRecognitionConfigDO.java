package cn.iocoder.yudao.module.icbc.dal.dataobject.cardrecognition;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 卡证识别的平台级参数 DO（#103，ADR 0037）。
 *
 * <p>与电子签章**各立一处**：OCR 的额度与密钥是平台共享的，回收企业不该自己配腾讯密钥，
 * 所以这张表**不带租户维度**（继承 {@link BaseDO}），对应表名需登记在
 * {@code yudao.tenant.ignore-tables} 中——不登记就查不到行，页面永远显示「未配置」。
 *
 * <p><b>密钥只落后端、界面不回显明文</b>：{@code secretId} / {@code secretKey} 只写不读——
 * 保存时留空表示「不改动」，查询响应里只回「已配置」与否。
 *
 * <p>{@code provider} 是**运行期**判定的供应商（{@code stub} / {@code tencent}），取代了 #93 的
 * 启动期 {@code icbc.card-recognition.mode}；DB 为空时回落 yaml / env。
 */
@TableName("icbc_card_recognition_config")
@KeySequence("icbc_card_recognition_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IcbcCardRecognitionConfigDO extends BaseDO {

    @TableId
    private Long id;

    /** 供应商：stub-未启用，tencent-腾讯云 OCR */
    private String provider;

    /** 腾讯云 API 密钥 SecretId（只写不读） */
    private String secretId;

    /** 腾讯云 API 密钥 SecretKey（只写不读） */
    private String secretKey;

    /** 地域（OCR 是全局服务，签名需要） */
    private String region;

    /** OCR 服务域名 */
    private String endpoint;

    /** 接口超时（毫秒） */
    private Integer timeout;

    /** 最近一次连通性自检分类：OK / AUTH_FAILED / NETWORK / VENDOR_ERROR（只存分类，不存密钥或厂商原始报文） */
    private String lastCheckResult;

    /** 最近一次连通性自检时间 */
    private LocalDateTime lastCheckTime;

    /** 备注 */
    private String remark;

}
