package cn.iocoder.yudao.module.icbc.service.esign;

import cn.iocoder.yudao.module.icbc.controller.admin.esign.vo.EsignConfigSaveReqVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link EsignConfigSaveReqVO#getEnvironment()} 的后端枚举校验（STD-4）。
 *
 * <p>环境只有 TEST / PROD 两套腾讯电子签端点与密钥，填错会把测试合同推到生产（或反之）；
 * 之前只有前端 select 拦着，直连接口能写进任意字符串。这里锁住后端校验这一层。
 */
public class EsignConfigSaveReqVOValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    public void testEnvironment_unknownValueRejected() {
        EsignConfigSaveReqVO req = new EsignConfigSaveReqVO();
        req.setEnvironment("STAGING");
        Set<ConstraintViolation<EsignConfigSaveReqVO>> violations = validator.validate(req);
        assertFalse(violations.isEmpty(), "STAGING 不是合法环境，应被 @InEnum 拦下");
        assertTrue(violations.stream()
                        .anyMatch(item -> "environment".equals(item.getPropertyPath().toString())),
                "违规项应落在 environment 字段上");
    }

    @Test
    public void testEnvironment_testAndProdAccepted() {
        EsignConfigSaveReqVO req = new EsignConfigSaveReqVO();
        req.setEnvironment("TEST");
        assertTrue(validator.validate(req).isEmpty());
        req.setEnvironment("PROD");
        assertTrue(validator.validate(req).isEmpty());
    }

}
