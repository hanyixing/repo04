package ltd.newbee.mall.service;

import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallShoppingCartItem;
import ltd.newbee.mall.service.impl.NewBeeMallShoppingCartServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * NewBeeMallShoppingCartService 单元测试。
 * 覆盖购物车核心逻辑：新增（已存在转修改、商品缺失、单品/总量上限、成功）、
 * 修改（数据缺失、上限、越权、数值未变、成功）、删除越权校验、购物车明细组装与商品计数。
 */
@ExtendWith(MockitoExtension.class)
class NewBeeMallShoppingCartServiceImplTest {

    @Mock
    private NewBeeMallShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallShoppingCartServiceImpl shoppingCartService;

    private NewBeeMallShoppingCartItem buildCartItem(Long cartItemId, Long userId, Long goodsId, Integer count) {
        NewBeeMallShoppingCartItem item = new NewBeeMallShoppingCartItem();
        item.setCartItemId(cartItemId);
        item.setUserId(userId);
        item.setGoodsId(goodsId);
        item.setGoodsCount(count);
        return item;
    }

    // ---------- saveNewBeeMallCartItem ----------

    @Test
    @DisplayName("加购-购物车已存在该商品则走修改逻辑并返回 success")
    void saveCartItem_existingItem_updatesAndReturnsSuccess() {
        NewBeeMallShoppingCartItem input = buildCartItem(null, 1L, 2L, 2);
        NewBeeMallShoppingCartItem existing = buildCartItem(5L, 1L, 2L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 2L)).thenReturn(existing);
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 1L, 2L, 1));
        when(shoppingCartItemMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        String result = shoppingCartService.saveNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    @DisplayName("加购-商品不存在返回商品不存在")
    void saveCartItem_goodsNotExist_returnsGoodsNotExist() {
        NewBeeMallShoppingCartItem input = buildCartItem(null, 1L, 2L, 2);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 2L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(2L)).thenReturn(null);

        String result = shoppingCartService.saveNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), result);
        verify(shoppingCartItemMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("加购-超出单个商品最大数量返回单品上限错误")
    void saveCartItem_singleLimitExceeded_returnsLimitError() {
        NewBeeMallShoppingCartItem input = buildCartItem(null, 1L, 2L, 6);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 2L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(2L)).thenReturn(new NewBeeMallGoods());
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(0);

        String result = shoppingCartService.saveNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
        verify(shoppingCartItemMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("加购-超出购物车总量返回总量上限错误")
    void saveCartItem_totalExceeded_returnsTotalError() {
        NewBeeMallShoppingCartItem input = buildCartItem(null, 1L, 2L, 1);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 2L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(2L)).thenReturn(new NewBeeMallGoods());
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(13);

        String result = shoppingCartService.saveNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_TOTAL_NUMBER_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("加购-新商品校验通过且入库成功返回 success")
    void saveCartItem_newItem_success() {
        NewBeeMallShoppingCartItem input = buildCartItem(null, 1L, 2L, 2);
        when(shoppingCartItemMapper.selectByUserIdAndGoodsId(1L, 2L)).thenReturn(null);
        when(goodsMapper.selectByPrimaryKey(2L)).thenReturn(new NewBeeMallGoods());
        when(shoppingCartItemMapper.selectCountByUserId(1L)).thenReturn(0);
        when(shoppingCartItemMapper.insertSelective(any())).thenReturn(1);

        String result = shoppingCartService.saveNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        verify(shoppingCartItemMapper).insertSelective(any());
    }

    // ---------- updateNewBeeMallCartItem ----------

    @Test
    @DisplayName("改购-待修改记录不存在返回数据不存在")
    void updateCartItem_dataNotExist_returnsDataNotExist() {
        NewBeeMallShoppingCartItem input = buildCartItem(5L, 1L, 2L, 2);
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(null);

        String result = shoppingCartService.updateNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    @DisplayName("改购-超出单品最大数量返回单品上限错误")
    void updateCartItem_singleLimitExceeded_returnsLimitError() {
        NewBeeMallShoppingCartItem input = buildCartItem(5L, 1L, 2L, 6);
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 1L, 2L, 1));

        String result = shoppingCartService.updateNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("改购-修改他人购物项返回无权限")
    void updateCartItem_otherUser_returnsNoPermission() {
        NewBeeMallShoppingCartItem input = buildCartItem(5L, 1L, 2L, 2);
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 2L, 2L, 1));

        String result = shoppingCartService.updateNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("改购-数量未变化不执行更新直接返回 success")
    void updateCartItem_sameCount_returnsSuccessWithoutUpdate() {
        NewBeeMallShoppingCartItem input = buildCartItem(5L, 1L, 2L, 3);
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 1L, 2L, 3));

        String result = shoppingCartService.updateNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        verify(shoppingCartItemMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("改购-数量变化且更新成功返回 success")
    void updateCartItem_success() {
        NewBeeMallShoppingCartItem input = buildCartItem(5L, 1L, 2L, 3);
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 1L, 2L, 1));
        when(shoppingCartItemMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        String result = shoppingCartService.updateNewBeeMallCartItem(input);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        verify(shoppingCartItemMapper).updateByPrimaryKeySelective(any());
    }

    // ---------- deleteById ----------

    @Test
    @DisplayName("删除购物项-记录不存在返回 false")
    void deleteById_itemNotExist_returnsFalse() {
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(null);

        assertFalse(shoppingCartService.deleteById(5L, 1L));
    }

    @Test
    @DisplayName("删除购物项-非本人购物项返回 false")
    void deleteById_otherUser_returnsFalse() {
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 2L, 2L, 1));

        assertFalse(shoppingCartService.deleteById(5L, 1L));
        verify(shoppingCartItemMapper, never()).deleteByPrimaryKey(anyLong());
    }

    @Test
    @DisplayName("删除购物项-本人且删除成功返回 true")
    void deleteById_success_returnsTrue() {
        when(shoppingCartItemMapper.selectByPrimaryKey(5L)).thenReturn(buildCartItem(5L, 1L, 2L, 1));
        when(shoppingCartItemMapper.deleteByPrimaryKey(5L)).thenReturn(1);

        assertTrue(shoppingCartService.deleteById(5L, 1L));
    }

    // ---------- getMyShoppingCartItems ----------

    @Test
    @DisplayName("查询我的购物车-正确组装商品信息与数量")
    void getMyShoppingCartItems_buildsVoWithGoodsInfoAndCount() {
        NewBeeMallShoppingCartItem item = buildCartItem(5L, 1L, 2L, 2);
        NewBeeMallGoods goods = new NewBeeMallGoods();
        goods.setGoodsId(2L);
        goods.setGoodsName("商品名称");
        goods.setGoodsCoverImg("cover.jpg");
        goods.setSellingPrice(50);
        when(shoppingCartItemMapper.selectByUserId(anyLong(), anyInt())).thenReturn(Collections.singletonList(item));
        when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.singletonList(goods));

        List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(1L);

        assertEquals(1, result.size());
        NewBeeMallShoppingCartItemVO vo = result.get(0);
        assertEquals(2L, vo.getGoodsId());
        assertEquals(2, vo.getGoodsCount());
        assertEquals(50, vo.getSellingPrice());
        assertEquals("商品名称", vo.getGoodsName());
    }

    @Test
    @DisplayName("查询我的购物车-无购物项返回空列表")
    void getMyShoppingCartItems_empty_returnsEmptyList() {
        when(shoppingCartItemMapper.selectByUserId(anyLong(), anyInt())).thenReturn(Collections.emptyList());

        List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(1L);

        assertTrue(result.isEmpty());
    }
}
