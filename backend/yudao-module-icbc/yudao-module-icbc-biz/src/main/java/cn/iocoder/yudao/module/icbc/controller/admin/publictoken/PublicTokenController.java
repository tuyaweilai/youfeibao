package cn.iocoder.yudao.module.icbc.controller.admin.publictoken;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenCreateReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.publictoken.vo.PublicTokenRevokeReqVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.token.PublicTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 公开令牌。给没有账号的自然人签发短期单用途令牌（二维码 / 短信里的链接）。
 */
@Tag(name = "管理后台 - 公开令牌")
@RestController
@RequestMapping("/icbc/public-token")
@Validated
@Slf4j
public class PublicTokenController {

    @Resource
    private PublicTokenService publicTokenService;

    @PostMapping("/create")
    @Operation(summary = "签发公开令牌")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PUBLIC_TOKEN_CREATE + "')")
    public CommonResult<PublicTokenRespVO> createPublicToken(@Valid @RequestBody PublicTokenCreateReqVO reqVO) {
        return success(publicTokenService.mint(reqVO));
    }

    @PostMapping("/revoke")
    @Operation(summary = "作废一枚公开令牌（链接立刻失效，收货员可重新生成）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PUBLIC_TOKEN_CREATE + "')")
    public CommonResult<Boolean> revokePublicToken(@Valid @RequestBody PublicTokenRevokeReqVO reqVO) {
        publicTokenService.revoke(reqVO.getToken());
        return success(true);
    }

}
