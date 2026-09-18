package cn.iocoder.yudao.module.icbc.service.goodscfg;

import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.module.icbc.UnitTestConfiguration;
import cn.iocoder.yudao.module.icbc.controller.admin.goodscfg.vo.IcbcGoodsConfigSaveReqVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.goodscfg.IcbcGoodsConfigDO;
import cn.iocoder.yudao.module.icbc.dal.mysql.goodscfg.IcbcGoodsConfigMapper;
import cn.iocoder.yudao.module.icbc.service.goodscfg.impl.IcbcGoodsConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.icbc.enums.ErrorCodeConstants.GOODS_CONFIG_TAX_METHOD_INVALID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link IcbcGoodsConfigServiceImpl} 的单元测试类。
 */
@Import({IcbcGoodsConfigServiceImpl.class, UnitTestConfiguration.class})
@Transactional
@Rollback
public class IcbcGoodsConfigServiceImplTest extends BaseDbUnitTest {

    @Resource
    private IcbcGoodsConfigServiceImpl goodsConfigService;

    @Resource
    private IcbcGoodsConfigMapper goodsConfigMapper;

    @Test
    public void testCreateGoodsConfig_withTaxMethod() {
        IcbcGoodsConfigSaveReqVO reqVO = buildReqVO("废铁", "1090101010000000000", "SIMPLE");

        Long id = goodsConfigService.createGoodsConfig(reqVO);

        assertNotNull(id);
        IcbcGoodsConfigDO config = goodsConfigMapper.selectById(id);
        assertEquals("SIMPLE", config.getTaxMethod());
        assertEquals("1090101010000000000", config.getMergedCode());
    }

    @Test
    public void testCreateGoodsConfig_invalidTaxMethod() {
        IcbcGoodsConfigSaveReqVO reqVO = buildReqVO("废铜", "1090101020000000000", "WRONG");
        assertServiceException(() -> goodsConfigService.createGoodsConfig(reqVO),
                GOODS_CONFIG_TAX_METHOD_INVALID);
    }

    @Test
    public void testGetGoodsConfigByMergedCode() {
        goodsConfigService.createGoodsConfig(buildReqVO("废铁", "1090101010000000000", "SIMPLE"));

        IcbcGoodsConfigDO config = goodsConfigService.getGoodsConfigByMergedCode("1090101010000000000");

        assertNotNull(config);
        assertEquals("SIMPLE", config.getTaxMethod());
    }

    @Test
    public void testGetGoodsConfigByMergedCode_notConfigured() {
        assertEquals(null, goodsConfigService.getGoodsConfigByMergedCode("9999999999999999999"));
        assertEquals(null, goodsConfigService.getGoodsConfigByMergedCode(null));
    }

    private IcbcGoodsConfigSaveReqVO buildReqVO(String name, String mergedCode, String taxMethod) {
        IcbcGoodsConfigSaveReqVO reqVO = new IcbcGoodsConfigSaveReqVO();
        reqVO.setName(name);
        reqVO.setUnit("吨");
        reqVO.setTaxRate(new BigDecimal("0.13"));
        reqVO.setTaxMethod(taxMethod);
        reqVO.setMergedCode(mergedCode);
        reqVO.setStatus(0);
        return reqVO;
    }

}
