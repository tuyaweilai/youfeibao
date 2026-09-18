package cn.iocoder.yudao.module.icbc.dal.dataobject.download;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 工行发票下载记录 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_invoice_download")
@KeySequence("icbc_invoice_download_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDownloadDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 发票订单ID
     */
    private Long invoiceOrderId;
    
    /**
     * 合作方订单号
     */
    private String partnerOrderId;
    
    /**
     * 工行订单号
     */
    private String orderNumber;
    
    /**
     * 发票号码
     */
    private String invoiceNumber;
    
    /**
     * 发票下载URL
     */
    private String downloadUrl;
    
    /**
     * 本地文件存储路径
     */
    private String filePath;
    
    /**
     * 文件名称
     */
    private String fileName;
    
    /**
     * 文件大小(字节)
     */
    private Long fileSize;
    
    /**
     * 下载状态
     * 
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.DownloadStatusEnum}
     */
    private Integer downloadStatus;
    
    /**
     * 下载时间
     */
    private LocalDateTime downloadTime;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 错误信息
     */
    private String errorMsg;

} 