package cn.iocoder.yudao.module.icbc.service.sellerauth;

import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerLoginRespVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerSmsLoginReqVO;
import cn.iocoder.yudao.module.icbc.controller.app.sellerauth.vo.SellerSubjectRespVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 自然人出售者登录 Service（ADR 0017）。
 *
 * <p>登录凭证复用 {@code yudao-module-member} 的会员用户，但**落在平台租户**：同一个人在两家回收
 * 企业卖货，手机号只对应一个登录凭证。身份锚点（自然人主体）与登录凭证分离，两者互不替代：
 * 注销/解绑只动凭证，身份与交易记录不动。
 */
public interface SellerAuthService {

    /**
     * 发送登录短信验证码。
     */
    void sendSmsCode(String mobile);

    /**
     * 手机号 + 短信验证码登录。验证码通过后按手机号取或建登录凭证并签发令牌。
     */
    SellerLoginRespVO smsLogin(@Valid SellerSmsLoginReqVO reqVO);

    /**
     * 退出登录（移除访问令牌）。**只是凭证动作，不影响身份与交易记录**。
     *
     * @param token 访问令牌，由控制层从请求头取出后传入
     */
    void logout(String token);

    /**
     * 当前登录名下的自然人主体。
     */
    List<SellerSubjectRespVO> listSubjects();

    /**
     * 把某个收方档案对应的自然人主体绑到当前登录凭证上（「确认结算时才注册」的落地）。
     *
     * <p>同一身份证已有主体且登记手机号与当前登录手机号不一致时**拒绝，不合并**：提示用原手机号登录
     * 或联系客服，由平台运营人工认领。
     *
     * @param payeeId 本租户内的收方档案编号（租户由请求头 tenant-id 给出，即他扫码的那个场站所属企业）
     * @return 绑定后的自然人主体
     */
    SellerSubjectRespVO bindSubject(Long payeeId);

    /**
     * 解绑自然人主体。**只影响凭证**，不删除身份与交易记录。
     */
    void unbindSubject(Long naturalPersonId);

    /**
     * 校验「本次操作人」确实是当前登录名下的主体。后续所有自然人侧业务端点都先过这一关，
     * 保证不做静默推断（一个登录名下可能有多个主体）。
     */
    void assertBound(Long naturalPersonId);

}
