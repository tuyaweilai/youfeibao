package cn.iocoder.yudao.module.icbc.controller.admin.payee.vo;

import cn.iocoder.yudao.module.icbc.enums.IcbcOccupationEnum;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link PayeeInfoSaveReqVO} 的字段校验测试（#84）。
 *
 * <p>后台的「职业」以前是自由文本，手敲一个词就送去工行、被驳回。这里把两件事钉死：
 * 职业只能是工行 15 值字典里的值；银行卡号一旦填了就必须是 16-19 位数字（不填仍然允许，
 * 卡可以后补，见 {@code SELLER_BANK_CARD_REQUIRED}）。
 */
public class PayeeInfoSaveReqVOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testOccupation_mustComeFromIcbcDictionary() {
        PayeeInfoSaveReqVO vo = baseValid();
        vo.setOccupation("程序员"); // 手敲
        assertFalse(validator.validate(vo).isEmpty());

        vo.setOccupation("16"); // 越界
        assertFalse(validator.validate(vo).isEmpty());

        vo.setOccupation(IcbcOccupationEnum.OTHER.getCode()); // 14-其他
        assertTrue(validator.validate(vo).isEmpty());

        vo.setOccupation(null); // 不填走缺省，允许
        assertTrue(validator.validate(vo).isEmpty());
    }

    @Test
    public void testBankCardNo_mustBeDigitsWhenPresent() {
        PayeeInfoSaveReqVO vo = baseValid();
        vo.setBankCardNo("6222 0212 3456 7890"); // 带空格
        assertFalse(validator.validate(vo).isEmpty());

        vo.setBankCardNo("622202123456"); // 太短
        assertFalse(validator.validate(vo).isEmpty());

        vo.setBankCardNo("6222021234567890123"); // 19 位
        assertTrue(validator.validate(vo).isEmpty());

        vo.setBankCardNo(null); // 不填允许（卡可以后补）
        assertTrue(validator.validate(vo).isEmpty());
    }

    @Test
    public void testRequiredFieldsStillEnforced() {
        PayeeInfoSaveReqVO vo = baseValid();
        vo.setName(null);
        Set<ConstraintViolation<PayeeInfoSaveReqVO>> violations = validator.validate(vo);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("收方姓名不能为空")));
    }

    private PayeeInfoSaveReqVO baseValid() {
        PayeeInfoSaveReqVO vo = new PayeeInfoSaveReqVO();
        vo.setName("张三");
        vo.setIdCardNo("110101199001011234");
        vo.setMobile("13800138000");
        return vo;
    }

}
