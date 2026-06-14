package ltd.newbee.mall.service;

import ltd.newbee.mall.common.NewBeeMallException;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.dao.GoodsCategoryMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.entity.GoodsCategory;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.service.impl.NewBeeMallGoodsServiceImpl;
import ltd.newbee.mall.util.PageQueryUtil;
import ltd.newbee.mall.util.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NewBeeMallGoodsService 单元测试。
 * 覆盖商品管理核心逻辑：新增/修改时的分类校验与重名校验、库存状态批量更新、按 id 查询。
 */
@ExtendWith(MockitoExtension.class)
class NewBeeMallGoodsServiceImplTest {

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @InjectMocks
    private NewBeeMallGoodsServiceImpl goodsService;

    private NewBeeMallGoods buildGoods() {
        NewBeeMallGoods goods = new NewBeeMallGoods();
        goods.setGoodsId(1L);
        goods.setGoodsName("测试商品");
        goods.setGoodsIntro("商品简介");
        goods.setTag("热卖");
        goods.setGoodsCategoryId(100L);
        goods.setGoodsSellStatus((byte) 0);
        goods.setStockNum(10);
        goods.setSellingPrice(50);
        return goods;
    }

    private GoodsCategory buildLevel3Category() {
        GoodsCategory category = new GoodsCategory();
        category.setCategoryId(100L);
        category.setCategoryLevel((byte) 3);
        return category;
    }

    @Test
    @DisplayName("新增商品-分类不存在返回分类异常")
    void saveNewBeeMallGoods_categoryNotExist_returnsCategoryError() {
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(null);

        String result = goodsService.saveNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
        verify(goodsMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("新增商品-分类非三级返回分类异常")
    void saveNewBeeMallGoods_categoryNotLevelThree_returnsCategoryError() {
        GoodsCategory category = buildLevel3Category();
        category.setCategoryLevel((byte) 2);
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(category);

        String result = goodsService.saveNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("新增商品-同分类同名已存在返回重复异常")
    void saveNewBeeMallGoods_duplicate_returnsSameGoodsExist() {
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(buildLevel3Category());
        when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(buildGoods());

        String result = goodsService.saveNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.SAME_GOODS_EXIST.getResult(), result);
        verify(goodsMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("新增商品-校验通过且入库成功返回 success")
    void saveNewBeeMallGoods_success() {
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(buildLevel3Category());
        when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
        when(goodsMapper.insertSelective(any(NewBeeMallGoods.class))).thenReturn(1);

        String result = goodsService.saveNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        verify(goodsMapper).insertSelective(any(NewBeeMallGoods.class));
    }

    @Test
    @DisplayName("新增商品-入库失败返回数据库异常")
    void saveNewBeeMallGoods_insertFails_returnsDbError() {
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(buildLevel3Category());
        when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
        when(goodsMapper.insertSelective(any(NewBeeMallGoods.class))).thenReturn(0);

        String result = goodsService.saveNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("修改商品-商品不存在返回数据不存在")
    void updateNewBeeMallGoods_goodsNotExist_returnsDataNotExist() {
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(buildLevel3Category());
        when(goodsMapper.selectByPrimaryKey(anyLong())).thenReturn(null);

        String result = goodsService.updateNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    @DisplayName("修改商品-校验通过且更新成功返回 success")
    void updateNewBeeMallGoods_success() {
        when(goodsCategoryMapper.selectByPrimaryKey(anyLong())).thenReturn(buildLevel3Category());
        when(goodsMapper.selectByPrimaryKey(anyLong())).thenReturn(buildGoods());
        when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
        when(goodsMapper.updateByPrimaryKeySelective(any(NewBeeMallGoods.class))).thenReturn(1);

        String result = goodsService.updateNewBeeMallGoods(buildGoods());

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    @DisplayName("按 id 查询商品-存在则返回该商品")
    void getNewBeeMallGoodsById_found_returnsGoods() {
        NewBeeMallGoods goods = buildGoods();
        when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(goods);

        NewBeeMallGoods result = goodsService.getNewBeeMallGoodsById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getGoodsId());
    }

    @Test
    @DisplayName("按 id 查询商品-不存在则抛出业务异常")
    void getNewBeeMallGoodsById_notFound_throwsException() {
        when(goodsMapper.selectByPrimaryKey(999L)).thenReturn(null);

        assertThrows(NewBeeMallException.class, () -> goodsService.getNewBeeMallGoodsById(999L));
    }

    @Test
    @DisplayName("批量修改上架状态-影响行数大于0返回 true")
    void batchUpdateSellStatus_success_returnsTrue() {
        Long[] ids = {1L, 2L};
        when(goodsMapper.batchUpdateSellStatus(any(Long[].class), anyInt())).thenReturn(2);

        assertTrue(goodsService.batchUpdateSellStatus(ids, 1));
    }

    @Test
    @DisplayName("批量修改上架状态-影响行数为0返回 false")
    void batchUpdateSellStatus_noRowsAffected_returnsFalse() {
        Long[] ids = {1L, 2L};
        when(goodsMapper.batchUpdateSellStatus(any(Long[].class), anyInt())).thenReturn(0);

        assertFalse(goodsService.batchUpdateSellStatus(ids, 1));
    }

    @Test
    @DisplayName("分页查询商品-返回封装了总数与列表的 PageResult")
    void getNewBeeMallGoodsPage_returnsPageResult() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("limit", 10);
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        when(goodsMapper.findNewBeeMallGoodsList(pageUtil)).thenReturn(Collections.singletonList(buildGoods()));
        when(goodsMapper.getTotalNewBeeMallGoods(pageUtil)).thenReturn(1);

        PageResult result = goodsService.getNewBeeMallGoodsPage(pageUtil);

        assertEquals(1, result.getTotalCount());
        assertEquals(1, result.getList().size());
    }
}
