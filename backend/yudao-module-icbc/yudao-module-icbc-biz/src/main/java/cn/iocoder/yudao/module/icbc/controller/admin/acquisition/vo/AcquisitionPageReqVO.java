package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 收购登记分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AcquisitionPageReqVO extends PageParam {

    @Schema(description = "出售者档案编号", example = "1024")
    private Long payeeId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "收购单号", example = "ACQ17645472000001234")
    private String acquisitionNo;

    @Schema(description = "出售者姓名", example = "张三")
    private String sellerName;

    @Schema(description = "车牌号", example = "京A12345")
    private String vehiclePlateNo;

    @Schema(description = "交易时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] tradeTime;

}
