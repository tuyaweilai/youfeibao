package cn.iocoder.yudao.module.icbc.enums;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 工行收方入驻的「职业」字典（15 值）。
 *
 * <p>依据工行《聚富通智慧清分收方新增接口 V1》的 {@code occupation}：{@code receiverType=03-自然人}
 * 时必填，取值就是这 15 个。它是**工行的字典**，不是我们的分类——所以后台只能从这些里选，
 * 手敲一个词送出去会被工行驳回（#84）。缺省用 {@link #OTHER}（唯一不宣称任何事实的取值，见
 * {@code SellerOnboardingServiceImpl.DEFAULT_OCCUPATION} 的说明）。
 *
 * <p>放在 api 层：biz 的收方档案 VO 用 {@code @InEnum} 校验，入驻上送时用枚举兜底。
 */
@Getter
@RequiredArgsConstructor
public enum IcbcOccupationEnum implements ArrayValuable<String> {

    CIVIL_SERVANT("1", "公务员"),
    PUBLIC_INSTITUTION("2", "事业单位员工"),
    COMPANY_EMPLOYEE("3", "公司员工"),
    SOLDIER_POLICE("4", "军人警察"),
    WORKER("5", "工人"),
    FARMER("6", "农民"),
    MANAGER("7", "管理人员"),
    TECHNICIAN("8", "技术人员"),
    PRIVATE_OWNER("9", "私营业主"),
    STAR("10", "文体明星"),
    FREELANCER("11", "自由职业者"),
    STUDENT("12", "学生"),
    UNEMPLOYED("13", "无职业"),
    OTHER("14", "其他"),
    RETIRED("15", "退休"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(IcbcOccupationEnum::getCode).toArray(String[]::new);

    /**
     * 工行字典值
     */
    private final String code;
    /**
     * 中文名（后台下拉展示用）
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

    public static IcbcOccupationEnum valueOfCode(String code) {
        return Arrays.stream(values()).filter(item -> item.getCode().equals(code)).findFirst().orElse(null);
    }

    public static String nameOf(String code) {
        IcbcOccupationEnum item = valueOfCode(code);
        return item != null ? item.getName() : null;
    }

}
