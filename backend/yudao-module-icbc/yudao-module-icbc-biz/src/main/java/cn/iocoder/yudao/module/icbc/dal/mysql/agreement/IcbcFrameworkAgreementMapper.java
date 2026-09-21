package cn.iocoder.yudao.module.icbc.dal.mysql.agreement;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.icbc.dal.dataobject.agreement.IcbcFrameworkAgreementDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 框架收购协议 Mapper
 */
@Mapper
public interface IcbcFrameworkAgreementMapper extends BaseMapperX<IcbcFrameworkAgreementDO> {

    default IcbcFrameworkAgreementDO selectActiveByPayeeId(Long payeeId) {
        return selectOne(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getPayeeId, payeeId)
                .eq(IcbcFrameworkAgreementDO::getStatus, 1)
                .orderByDesc(IcbcFrameworkAgreementDO::getId)
                .last("LIMIT 1"));
    }

    default List<IcbcFrameworkAgreementDO> selectListByPayeeId(Long payeeId) {
        return selectList(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getPayeeId, payeeId)
                .orderByDesc(IcbcFrameworkAgreementDO::getId));
    }

    /**
     * 按第三方签署任务号查协议：签署完成回调据此定位到具体协议（回调只带任务号）。
     */
    default IcbcFrameworkAgreementDO selectBySignTaskId(String signTaskId) {
        if (signTaskId == null || signTaskId.isEmpty()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getSignTaskId, signTaskId)
                .orderByDesc(IcbcFrameworkAgreementDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 查某出售者当前待签署的协议（最新一条）：自然人端「去签署」据此生成链接。
     */
    default IcbcFrameworkAgreementDO selectPendingByPayeeId(Long payeeId) {
        return selectOne(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getPayeeId, payeeId)
                .eq(IcbcFrameworkAgreementDO::getStatus, 0)
                .orderByDesc(IcbcFrameworkAgreementDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 批量查生效中的协议：一票一档证据链把出售者的框架收购协议挂进合同流。
     */
    default List<IcbcFrameworkAgreementDO> selectEffectiveByPayeeIds(Collection<Long> payeeIds) {
        if (payeeIds == null || payeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .in(IcbcFrameworkAgreementDO::getPayeeId, payeeIds)
                .eq(IcbcFrameworkAgreementDO::getStatus, 1)
                .orderByDesc(IcbcFrameworkAgreementDO::getId));
    }

    /**
     * 批量查询待签署（status=0）的协议：自然人端首页「待我确认」用。
     */
    default List<IcbcFrameworkAgreementDO> selectPendingByPayeeIds(Collection<Long> payeeIds) {
        if (payeeIds == null || payeeIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<IcbcFrameworkAgreementDO>()
                .in(IcbcFrameworkAgreementDO::getPayeeId, payeeIds)
                .eq(IcbcFrameworkAgreementDO::getStatus, 0)
                .orderByDesc(IcbcFrameworkAgreementDO::getId));
    }

    /**
     * 把「待签署」协议推进到「生效」：**条件更新**（status 仍必须是 0），返回受影响行数。
     *
     * <p>为什么不是普通 {@code updateById}：同一份签署通知可能被第三方并发重投。状态自检只能保证
     * 「先读后写」两次读到待签署的两个线程都通过检查；真正的兜底是这条带 {@code status = 0}
     * 条件的原子更新——只有一个线程改得动，另一个受影响 0 行、按重放处理（照
     * {@code CallbackNotifyServiceImpl}「状态自检 + 唯一键兜底」的同一思路，这里用条件更新兜底）。
     * 同时写入签署时间与两份文书地址：文件地址与生效在同一条 UPDATE 里落，避免中间态。
     */
    default int promoteToEffectiveIfPending(Long id, LocalDateTime signedAt, String fileUrl, String noticeFileUrl) {
        return update(null, new LambdaUpdateWrapper<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getId, id)
                .eq(IcbcFrameworkAgreementDO::getStatus, 0)
                .set(IcbcFrameworkAgreementDO::getStatus, 1)
                .set(IcbcFrameworkAgreementDO::getSignedAt, signedAt)
                .set(IcbcFrameworkAgreementDO::getFileUrl, fileUrl)
                .set(IcbcFrameworkAgreementDO::getNoticeFileUrl, noticeFileUrl));
    }

    /**
     * 回填主文书地址：只在当前为空时写（回调可能早于文件可查，之后重放补取）。
     */
    default int fillFileUrlIfBlank(Long id, String fileUrl) {
        return update(null, new LambdaUpdateWrapper<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getId, id)
                .isNull(IcbcFrameworkAgreementDO::getFileUrl)
                .set(IcbcFrameworkAgreementDO::getFileUrl, fileUrl));
    }

    /**
     * 回填告知函地址：只在当前为空时写。
     */
    default int fillNoticeFileUrlIfBlank(Long id, String noticeFileUrl) {
        return update(null, new LambdaUpdateWrapper<IcbcFrameworkAgreementDO>()
                .eq(IcbcFrameworkAgreementDO::getId, id)
                .isNull(IcbcFrameworkAgreementDO::getNoticeFileUrl)
                .set(IcbcFrameworkAgreementDO::getNoticeFileUrl, noticeFileUrl));
    }

}
