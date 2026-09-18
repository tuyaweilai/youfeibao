package cn.iocoder.yudao.module.contract.job;

import cn.iocoder.yudao.framework.quartz.core.handler.JobHandler;
import cn.iocoder.yudao.framework.tenant.core.job.TenantJob;
import cn.iocoder.yudao.module.contract.dal.dataobject.contract.ContractDO;
import cn.iocoder.yudao.module.contract.service.contract.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * 合同到期提醒定时任务
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class ContractExpiryReminderJob implements JobHandler {

    @Resource
    private ContractService contractService;

    @Override
    @TenantJob
    public String execute(String param) throws Exception {
        // 获取即将到期的合同（默认30天内）
        Integer days = 30;
        if (param != null && !param.trim().isEmpty()) {
            try {
                days = Integer.parseInt(param.trim());
            } catch (NumberFormatException e) {
                log.warn("合同到期提醒任务参数格式错误，使用默认值30天: {}", param);
            }
        }

        List<ContractDO> expiringContracts = contractService.getExpiringContracts(days);
        
        if (expiringContracts.isEmpty()) {
            log.info("没有即将到期的合同");
            return "没有即将到期的合同";
        }

        log.info("发现 {} 份即将到期的合同", expiringContracts.size());
        
        // TODO: 发送提醒通知
        // 1. 发送邮件通知
        // 2. 发送站内消息
        // 3. 发送短信通知（可选）
        // 4. 推送到企业微信/钉钉（可选）
        
        for (ContractDO contract : expiringContracts) {
            log.info("合同即将到期提醒: 合同编号={}, 合同名称={}", 
                    contract.getContractNo(), contract.getName());
            
            // TODO: 实现具体的通知逻辑
            sendExpiryNotification(contract);
        }

        return String.format("处理完成，共发送 %d 份合同到期提醒", expiringContracts.size());
    }

    /**
     * 发送到期提醒通知
     *
     * @param contract 合同信息
     */
    private void sendExpiryNotification(ContractDO contract) {
        // TODO: 实现通知发送逻辑
        // 1. 构建通知内容
        // 2. 获取通知接收人（合同相关人员）
        // 3. 发送通知
        
        log.debug("发送合同到期提醒通知: contractId={}, contractNo={}", 
                contract.getId(), contract.getContractNo());
    }

} 