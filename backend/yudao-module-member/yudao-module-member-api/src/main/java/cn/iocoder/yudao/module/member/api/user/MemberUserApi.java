package cn.iocoder.yudao.module.member.api.user;

import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * 会员用户的 API 接口
 *
 * @author 芋道源码
 */
public interface MemberUserApi {

    /**
     * 获得会员用户信息
     *
     * @param id 用户编号
     * @return 用户信息
     */
    MemberUserRespDTO getUser(Long id);

    /**
     * 获得会员用户信息们
     *
     * @param ids 用户编号的数组
     * @return 用户信息们
     */
    List<MemberUserRespDTO> getUserList(Collection<Long> ids);

    /**
     * 获得会员用户 Map
     *
     * @param ids 用户编号的数组
     * @return 会员用户 Map
     */
    default Map<Long, MemberUserRespDTO> getUserMap(Collection<Long> ids) {
        List<MemberUserRespDTO> list = getUserList(ids);
        return convertMap(list, MemberUserRespDTO::getId);
    }

    /**
     * 基于用户昵称，模糊匹配用户列表
     *
     * @param nickname 用户昵称，模糊匹配
     * @return 用户信息的列表
     */
    List<MemberUserRespDTO> getUserListByNickname(String nickname);

    /**
     * 基于手机号，精准匹配用户
     *
     * @param mobile 手机号
     * @return 用户信息
     */
    MemberUserRespDTO getUserByMobile(String mobile);

    /**
     * 按手机号取或建会员用户（不存在则注册）。
     *
     * <p>自然人的登录凭证复用会员用户：调用方需要在**平台租户**下执行，否则同一个手机号会在
     * 各个回收企业租户下各建一个（见 ADR 0017）。
     *
     * @param mobile     手机号
     * @param registerIp 注册 IP
     * @param terminal   注册终端，见 {@link cn.iocoder.yudao.framework.common.enums.TerminalEnum}
     * @return 会员用户信息
     */
    MemberUserRespDTO createUserIfAbsent(String mobile, String registerIp, Integer terminal);

    /**
     * 校验用户是否存在
     *
     * @param id 用户编号
     */
    void validateUser(Long id);

}
