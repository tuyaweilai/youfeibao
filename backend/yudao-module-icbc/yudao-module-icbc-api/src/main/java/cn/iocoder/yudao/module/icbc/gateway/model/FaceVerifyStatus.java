package cn.iocoder.yudao.module.icbc.gateway.model;

import lombok.Builder;
import lombok.Data;

/**
 * 实人认证结果
 *
 * 对应工行 `/api/jft/api/user/faceH5/query/V1` 的查询结果，以及
 * `{appId, transNode, outUserId, verifyResult}` 的结果通知。两条路径都归一到本模型。
 */
@Data
@Builder
public class FaceVerifyStatus {

    private String outUserId;
    /**
     * 认证状态（authResult，原样透传）：00-初始，01-认证中，以此类推
     */
    private String authResult;
    /**
     * 是否认证通过
     */
    private boolean passed;
    /**
     * 失败原因
     */
    private String failReason;

}
