package cn.iocoder.yudao.module.icbc.controller.admin.naturalperson;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonClaimReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonConflictRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonLoginDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.naturalperson.IcbcNaturalPersonLoginMapper;
import cn.iocoder.yudao.module.icbc.enums.PayeeRealNameStatusEnum;
import cn.iocoder.yudao.module.icbc.service.naturalperson.NaturalPersonService;
import cn.iocoder.yudao.module.icbc.service.naturalperson.impl.NaturalPersonServiceImpl;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 平台运营 - 自然人主体（跨租户）。
 *
 * <p>这里只提供**人工**入口：身份认领与解绑、停用与恢复。自动化流程只负责拒绝
 * （同一身份证绑不同手机号时不覆盖、不自动合并），裁决由平台运营核实后手工完成（ADR 0017）。
 */
@Tag(name = "平台运营 - 自然人主体")
@RestController
@RequestMapping("/icbc/platform/natural-person")
@Validated
@Slf4j
public class NaturalPersonAdminController {

    @Resource
    private NaturalPersonService naturalPersonService;
    @Resource
    private IcbcNaturalPersonLoginMapper naturalPersonLoginMapper;

    @GetMapping("/page")
    @Operation(summary = "分页查询自然人主体（跨租户）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_NATURAL_PERSON_QUERY + "')")
    public CommonResult<PageResult<NaturalPersonRespVO>> getPage(@Valid NaturalPersonPageReqVO pageReqVO) {
        PageResult<IcbcNaturalPersonDO> page = naturalPersonService.getNaturalPersonPage(pageReqVO);
        PageResult<NaturalPersonRespVO> result = new PageResult<>(toRespList(page.getList()), page.getTotal());
        return success(result);
    }

    @GetMapping("/get")
    @Operation(summary = "获得自然人主体")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_NATURAL_PERSON_QUERY + "')")
    public CommonResult<NaturalPersonRespVO> get(@RequestParam("id") Long id) {
        return success(toResp(naturalPersonService.getNaturalPerson(id)));
    }

    @GetMapping("/conflicts")
    @Operation(summary = "身份冲突清单（跨租户，只读）",
            description = "同一身份证在不同租户姓名/手机号不一致者。命中 ADR 0017「不自动合并」规则，出人工清单；"
                    + "本接口不改任何数据，裁决由平台运营核实后完成")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_NATURAL_PERSON_QUERY + "')")
    public CommonResult<List<NaturalPersonConflictRespVO>> getConflicts() {
        return success(naturalPersonService.getIdentityConflictList());
    }

    @PostMapping("/claim")
    @Operation(summary = "身份认领", description = "核实后把某个登录凭证绑到已有自然人主体上（人工入口）")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_NATURAL_PERSON_MANAGE + "')")
    public CommonResult<Boolean> claim(@Valid @RequestBody NaturalPersonClaimReqVO reqVO) {
        naturalPersonService.bindLogin(reqVO.getNaturalPersonId(), reqVO.getMemberUserId(),
                NaturalPersonServiceImpl.SOURCE_OPS_CLAIM, reqVO.getRemark());
        return success(true);
    }

    @PostMapping("/unbind")
    @Operation(summary = "解绑登录凭证", description = "只解绑凭证，主体与交易记录保留")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_NATURAL_PERSON_MANAGE + "')")
    public CommonResult<Boolean> unbind(@Valid @RequestBody NaturalPersonClaimReqVO reqVO) {
        naturalPersonService.unbindLogin(reqVO.getNaturalPersonId(), reqVO.getMemberUserId());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "停用 / 恢复身份", description = "停用是人为处置（如冒用），不删除任何数据")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.PLATFORM_NATURAL_PERSON_MANAGE + "')")
    public CommonResult<Boolean> updateStatus(@RequestParam("id") Long id,
                                             @RequestParam("status") Integer status,
                                             @RequestParam(value = "remark", required = false) String remark) {
        naturalPersonService.updateStatus(id, status, remark);
        return success(true);
    }

    private List<NaturalPersonRespVO> toRespList(List<IcbcNaturalPersonDO> list) {
        return list.stream().map(this::toResp).toList();
    }

    private NaturalPersonRespVO toResp(IcbcNaturalPersonDO person) {
        NaturalPersonRespVO resp = new NaturalPersonRespVO();
        resp.setId(person.getId());
        resp.setOutUserId(person.getOutUserId());
        resp.setName(person.getName());
        // 平台运营看到的是「是谁」，不是可以复制走的原始证件号
        resp.setIdCardNo(MaskUtils.maskIdCard(person.getIdCardNo()));
        resp.setMobile(MaskUtils.maskMobile(person.getMobile()));
        resp.setRealNameStatus(person.getRealNameStatus());
        PayeeRealNameStatusEnum realName = PayeeRealNameStatusEnum.of(person.getRealNameStatus());
        resp.setRealNameStatusName(realName != null ? realName.getName() : null);
        resp.setRealNameTime(person.getRealNameTime());
        resp.setStatus(person.getStatus());
        resp.setRemark(person.getRemark());
        resp.setCreateTime(person.getCreateTime());
        resp.setMemberUserIds(naturalPersonLoginMapper.selectListByNaturalPersonId(person.getId()).stream()
                .map(IcbcNaturalPersonLoginDO::getMemberUserId).toList());
        return resp;
    }

}
