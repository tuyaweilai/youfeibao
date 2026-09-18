package cn.iocoder.yudao.module.icbc.dal.dataobject.download;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 工行发票文件 DO
 *
 * @author 芋道源码
 */
@TableName("icbc_invoice_file")
@KeySequence("icbc_invoice_file_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceFileDO extends TenantBaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 下载记录ID
     */
    private Long downloadId;
    
    /**
     * 发票号码
     */
    private String invoiceNumber;
    
    /**
     * 文件类型
     * 
     * 枚举 {@link cn.iocoder.yudao.module.icbc.enums.InvoiceFileTypeEnum}
     */
    private String fileType;
    
    /**
     * 文件存储路径
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
     * 文件MD5值
     */
    private String fileMd5;
    
    /**
     * 上传时间
     */
    private LocalDateTime uploadTime;
    
    /**
     * 访问次数
     */
    private Integer accessCount;
    
    /**
     * 最后访问时间
     */
    private LocalDateTime lastAccessTime;

} 