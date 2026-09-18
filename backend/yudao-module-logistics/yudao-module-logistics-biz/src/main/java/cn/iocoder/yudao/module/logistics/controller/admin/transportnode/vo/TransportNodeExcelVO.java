package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物流运输节点记录 Excel VO
 *
 * @author 芋道源码
 */
@Data
public class TransportNodeExcelVO {

    @ExcelProperty("主键ID")
    private Long id;

    @ExcelProperty("运输任务ID")
    private Long taskId;

    @ExcelProperty("节点类型")
    private Integer nodeType;

    @ExcelProperty("节点类型名称")
    private String nodeTypeName;

    @ExcelProperty("节点时间")
    private LocalDateTime nodeTime;

    @ExcelProperty("节点位置")
    private String nodeLocation;

    @ExcelProperty("纬度")
    private BigDecimal latitude;

    @ExcelProperty("经度")
    private BigDecimal longitude;

    @ExcelProperty("操作员ID")
    private Long operatorId;

    @ExcelProperty("操作员姓名")
    private String operatorName;

    @ExcelProperty("照片URLs")
    private String photos;

    @ExcelProperty("附加数据")
    private String additionalData;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

} 