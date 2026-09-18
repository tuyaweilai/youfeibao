package cn.iocoder.yudao.module.icbc.gateway;

/**
 * 一条「提交指令 + 查询确认」的命令
 *
 * 三件事：
 * <ul>
 *   <li>{@link #submit()}：向工行提交指令</li>
 *   <li>{@link #query()}：用同一幂等键查询指令最新状态</li>
 *   <li>{@link #confirm(Object)}：把查询到的状态翻译成「提交结果」</li>
 * </ul>
 *
 * @param <T> 提交返回类型
 * @param <Q> 查询返回类型
 */
public interface IcbcSubmitCommand<T, Q> {

    IcbcGatewayResult<T> submit();

    IcbcGatewayResult<Q> query();

    IcbcGatewayResult<T> confirm(Q queryData);

}
