package cn.iocoder.yudao.module.icbc.service.naturalperson;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonPageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.naturalperson.vo.NaturalPersonRegisterReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.naturalperson.IcbcNaturalPersonDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * 自然人主体 Service（平台级身份档案，见 ADR 0017）。
 *
 * <p>本接口承载两件事，且刻意把它们分开：
 * <ul>
 *   <li><b>身份锚点</b>：按身份证件号码唯一确定一个人，持有平台级 {@code outUserId}；</li>
 *   <li><b>登录凭证绑定</b>：一个登录凭证可以绑多个主体，一个主体也可以有多个凭证。</li>
 * </ul>
 * 身份不会因为凭证注销而消失，凭证也不会因为身份停用而自动解绑。
 */
public interface NaturalPersonService {

    /**
     * 身份登记：按身份证件号码取或建自然人主体。
     *
     * <p>同一身份证已有主体且登记的手机号/姓名不一致时**不覆盖、不自动合并**，直接拒绝并提示
     * 用原手机号登录或联系客服（ADR 0017）。完全一致的重复登记是幂等的。
     *
     * @param reqVO 身份登记信息
     * @return 自然人主体
     */
    IcbcNaturalPersonDO register(@Valid NaturalPersonRegisterReqVO reqVO);

    /**
     * 获得自然人主体；不存在时抛 {@code NATURAL_PERSON_NOT_EXISTS}。
     */
    IcbcNaturalPersonDO getNaturalPerson(Long id);

    /**
     * 按身份证件号码查询，查不到返回 {@code null}。
     */
    IcbcNaturalPersonDO getByIdCardNo(String idCardNo);

    /**
     * 按平台级外部用户编号查询（工行回调的入口），查不到返回 {@code null}。
     */
    IcbcNaturalPersonDO getByOutUserId(String outUserId);

    /**
     * 按编号批量查询。
     */
    List<IcbcNaturalPersonDO> getNaturalPersonList(Collection<Long> ids);

    /**
     * 标记实人认证进行中。已通过的不会回退（重复发起不该把已通过的认证打回去）。
     */
    void markRealNamePending(Long naturalPersonId);

    /**
     * 回写实人认证结果。认证中（未通过且没有失败原因）保持原状，通过时显式清空失败原因。
     */
    void applyRealNameResult(Long naturalPersonId, boolean passed, String failReason);

    // ==================== 登录凭证 ====================

    /**
     * 绑定登录凭证。重复绑定是幂等的。
     *
     * @param naturalPersonId 自然人主体编号
     * @param memberUserId    会员用户编号（平台租户下的登录凭证）
     * @param bindSource      绑定来源：REGISTER-本人注册，OPS_CLAIM-平台运营人工认领
     * @param remark          备注（人工认领时记录核实过程）
     */
    void bindLogin(Long naturalPersonId, Long memberUserId, String bindSource, String remark);

    /**
     * 解绑登录凭证。**只影响凭证，不删除主体与交易记录**（ADR 0017）。重复解绑是幂等的。
     */
    void unbindLogin(Long naturalPersonId, Long memberUserId);

    /**
     * 某个登录凭证名下的全部自然人主体。
     */
    List<IcbcNaturalPersonDO> getNaturalPersonListByMemberUserId(Long memberUserId);

    /**
     * 某个登录凭证到某个自然人主体的绑定是否存在。用于「显式选择本次操作人」的校验。
     */
    boolean isBoundToLogin(Long naturalPersonId, Long memberUserId);

    // ==================== 平台运营 ====================

    /**
     * 平台运营分页查询（跨租户）。
     */
    PageResult<IcbcNaturalPersonDO> getNaturalPersonPage(NaturalPersonPageReqVO reqVO);

    /**
     * 平台运营停用 / 恢复身份。停用是人为处置（如冒用），不删除任何数据。
     */
    void updateStatus(Long id, Integer status, String remark);

}
