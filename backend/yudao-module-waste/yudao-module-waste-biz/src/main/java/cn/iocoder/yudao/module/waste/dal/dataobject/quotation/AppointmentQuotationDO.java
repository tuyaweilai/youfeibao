package cn.iocoder.yudao.module.waste.dal.dataobject.quotation;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预约报价记录 DO
 *
 * @author 芋道源码
 */
@TableName("waste_appointment_quotation")
@KeySequence("waste_appointment_quotation_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentQuotationDO extends BaseDO {

    /**
     * 报价记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 预约单ID
     */
    private Long appointmentId;

    /**
     * 回收企业ID
     */
    private Long recyclingEnterpriseId;

    // ========== 报价信息 ==========
    /**
     * 报价金额
     */
    private BigDecimal quotedPrice;

    /**
     * 价格单位
     */
    private String priceUnit;

    /**
     * 总金额 (报价*数量)
     */
    private BigDecimal totalAmount;

    /**
     * 报价说明 (如:包含运费,质量要求等)
     */
    private String quotationRemark;

    /**
     * 报价有效期至
     */
    private LocalDateTime validUntil;

    // ========== 状态管理 ==========
    /**
     * 报价状态 (0:待确认, 1:已被接受, 2:已被拒绝, 3:已失效)
     *
     * 枚举 {@link cn.iocoder.yudao.module.waste.enums.QuotationStatusEnum}
     */
    private Integer status;

    /**
     * 接受时间
     */
    private LocalDateTime acceptedTime;

    /**
     * 接受人
     */
    private String acceptedBy;

    /**
     * 接受原因
     */
    private String acceptReason;

    /**
     * 拒绝时间
     */
    private LocalDateTime rejectedAt;

    /**
     * 拒绝人
     */
    private String rejectedBy;

    /**
     * 拒绝原因
     */
    private String rejectionReason;

    /**
     * 关联生成的合同ID
     */
    private Long contractId;

    // ========== 业务方法 ==========
    
    /**
     * 设置接受时间
     */
    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedTime = acceptedAt;
    }
    
    /**
     * 获取接受时间
     */
    public LocalDateTime getAcceptedAt() {
        return this.acceptedTime;
    }
    
    /**
     * 设置接受人
     */
    public void setAcceptedBy(String acceptedBy) {
        this.acceptedBy = acceptedBy;
    }
    
    /**
     * 获取接受人
     */
    public String getAcceptedBy() {
        return this.acceptedBy;
    }
    
    /**
     * 设置接受原因
     */
    public void setAcceptReason(String acceptReason) {
        this.acceptReason = acceptReason;
    }
    
    /**
     * 获取接受原因
     */
    public String getAcceptReason() {
        return this.acceptReason;
    }
    
    /**
     * 设置拒绝时间
     */
    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }
    
    /**
     * 设置拒绝人
     */
    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }
    
    /**
     * 设置拒绝原因
     */
    public void setRejectReason(String rejectReason) {
        this.rejectionReason = rejectReason;
    }

} 