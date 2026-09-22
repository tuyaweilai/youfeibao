package cn.iocoder.yudao.module.icbc.service.acquisition;

import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * 一笔收购的进度：一个档位 + 叠加的异常标注（ADR 0038）。
 *
 * <p>档位与异常是两件事：档位说「走到哪一步了」，异常说「哪条状态线出问题了」。
 * 四条状态线（预开票 / 付款 / 开票 / 缴税上传）可以任意交错，任何一条出问题都不得被
 * 一个好看的档位盖住（ADR 0021）。
 */
@Getter
@Builder
public class AcquisitionProgress {

    /** 档位 */
    private final AcquisitionStatusEnum stage;

    /** 异常标注，形如「付款：支付失败」；空表示四条线都没有异常 */
    private final List<String> abnormalReasons;

    public static AcquisitionProgress of(AcquisitionStatusEnum stage) {
        return AcquisitionProgress.builder()
                .stage(stage)
                .abnormalReasons(Collections.emptyList())
                .build();
    }

    public Integer getStatus() {
        return stage.getStatus();
    }

    public String getStageName() {
        return stage.getName();
    }

    public String getNextStep() {
        return stage.getNextStep();
    }

    public boolean isAbnormal() {
        return abnormalReasons != null && !abnormalReasons.isEmpty();
    }

}
