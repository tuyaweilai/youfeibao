package cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 管理后台 - 收购登记离线补传 Request VO。
 *
 * <p>现场端弱网时先把登记存在本地，恢复网络后一次性补传。服务端按每条的
 * {@code clientRequestId} 去重，重复补传只当作同一笔。
 */
@Schema(description = "管理后台 - 收购登记离线补传 Request VO")
@Data
public class AcquisitionOfflineSyncReqVO {

    @Schema(description = "待补传的收购登记列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "待补传的收购登记列表不能为空")
    @Valid
    private List<AcquisitionCreateReqVO> items;

}
