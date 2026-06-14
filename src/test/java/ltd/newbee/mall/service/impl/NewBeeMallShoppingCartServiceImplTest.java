package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.Constants;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallShoppingCartItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NewBeeMallShoppingCartService 单元测试")
class NewBeeMallShoppingCartServiceImplTest {

    @Mock
    private NewBeeMallShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallShoppingCartServiceImpl shoppingCartService;

    private NewBeeMallShoppingCartItem testCartItem;
    private NewBeeMallGoods testGoods;

    @BeforeEach
    void setUp() {
        testCartItem = new NewBeeMallShoppingCartItem();
        testCartItem.setCartItemId(1L);
        testCartItem.setUserId(100L);
        testCartItem.setGoodsId(10L);
        testCartItem.setGoodsCount(2);
        testCartItem.setIsDeleted((byte) 0);

        testGoods = new NewBeeMallGoods();
        testGoods.setGoodsId(10L);
        testGoods.setGoodsName("测试商品");
        testGoods.setGoodsCoverImg("/img/test.jpg");
        testGoods.setSellingPrice(100);
        testGoods.setStockNum(50);
        testGoods.setGoodsSellStatus((byte) 0);
    }

    @Nested
    @DisplayName("添加购物车项")
    class SaveCartItemTests {

        @Test
        @DisplayName("正常添加新购物车项 - 应返回success")
        void testSaveNewItem_Success() {
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(testGoods);
            when(shoppingCartItemMapper.selectCountByUserId(100L)).thenReturn(5);
            when(shoppingCartItemMapper.insertSelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(1);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(shoppingCartItemMapper).insertSelective(any(NewBeeMallShoppingCartItem.class));
        }

        @Test
        @DisplayName("商品已存在购物车中 - 应转为更新操作")
        void testSaveItem_ExistingInCart() {
            NewBeeMallShoppingCartItem existingItem = new NewBeeMallShoppingCartItem();
            existingItem.setCartItemId(1L);
            existingItem.setUserId(100L);
            existingItem.setGoodsId(10L);
            existingItem.setGoodsCount(1);

            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(existingItem);
            lenient().when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(existingItem);
            lenient().when(shoppingCartItemMapper.updateByPrimaryKeySelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(1);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(shoppingCartItemMapper, never()).insertSelective(any());
            verify(goodsMapper, never()).selectByPrimaryKey(anyLong());
        }

        @Test
        @DisplayName("商品不存在 - 应返回商品不存在")
        void testSaveItem_GoodsNotExist() {
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(null);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), result);
            verify(shoppingCartItemMapper, never()).insertSelective(any());
        }

        @Test
        @DisplayName("超出单个商品最大数量限制 - 应返回数量限制错误")
        void testSaveItem_ExceedItemLimit() {
            testCartItem.setGoodsCount(Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER + 1);
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(testGoods);
            when(shoppingCartItemMapper.selectCountByUserId(100L)).thenReturn(5);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("单个商品数量等于最大限制 - 应正常保存")
        void testSaveItem_ExactlyAtItemLimit() {
            testCartItem.setGoodsCount(Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER);
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(testGoods);
            when(shoppingCartItemMapper.selectCountByUserId(100L)).thenReturn(5);
            when(shoppingCartItemMapper.insertSelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(1);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("超出购物车最大容量 - 应返回容量错误")
        void testSaveItem_ExceedTotalCapacity() {
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(testGoods);
            when(shoppingCartItemMapper.selectCountByUserId(100L)).thenReturn(Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_TOTAL_NUMBER_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("购物车容量刚好满 - 应允许添加最后一件")
        void testSaveItem_ExactlyAtTotalCapacity() {
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(testGoods);
            when(shoppingCartItemMapper.selectCountByUserId(100L)).thenReturn(Constants.SHOPPING_CART_ITEM_TOTAL_NUMBER - 1);
            when(shoppingCartItemMapper.insertSelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(1);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("数据库插入失败 - 应返回database error")
        void testSaveItem_DbError() {
            when(shoppingCartItemMapper.selectByUserIdAndGoodsId(100L, 10L)).thenReturn(null);
            when(goodsMapper.selectByPrimaryKey(10L)).thenReturn(testGoods);
            when(shoppingCartItemMapper.selectCountByUserId(100L)).thenReturn(5);
            when(shoppingCartItemMapper.insertSelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(0);

            String result = shoppingCartService.saveNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("更新购物车项")
    class UpdateCartItemTests {

        @Test
        @DisplayName("正常更新数量 - 应返回success")
        void testUpdateItem_Success() {
            testCartItem.setGoodsCount(3);
            NewBeeMallShoppingCartItem dbItem = new NewBeeMallShoppingCartItem();
            dbItem.setCartItemId(1L);
            dbItem.setUserId(100L);
            dbItem.setGoodsId(10L);
            dbItem.setGoodsCount(2);

            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(dbItem);
            when(shoppingCartItemMapper.updateByPrimaryKeySelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(1);

            String result = shoppingCartService.updateNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("购物车项不存在 - 应返回数据不存在")
        void testUpdateItem_NotExist() {
            when(shoppingCartItemMapper.selectByPrimaryKey(anyLong())).thenReturn(null);

            String result = shoppingCartService.updateNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }

        @Test
        @DisplayName("超出单个商品最大数量 - 应返回数量限制错误")
        void testUpdateItem_ExceedLimit() {
            testCartItem.setGoodsCount(Constants.SHOPPING_CART_ITEM_LIMIT_NUMBER + 1);
            NewBeeMallShoppingCartItem dbItem = new NewBeeMallShoppingCartItem();
            dbItem.setCartItemId(1L);
            dbItem.setUserId(100L);
            dbItem.setGoodsCount(2);

            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(dbItem);

            String result = shoppingCartService.updateNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SHOPPING_CART_ITEM_LIMIT_NUMBER_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("修改他人购物车项 - 应返回无权限")
        void testUpdateItem_NoPermission() {
            testCartItem.setUserId(999L); // 不同的userId
            NewBeeMallShoppingCartItem dbItem = new NewBeeMallShoppingCartItem();
            dbItem.setCartItemId(1L);
            dbItem.setUserId(100L);
            dbItem.setGoodsCount(2);

            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(dbItem);

            String result = shoppingCartService.updateNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("数量未变化 - 应直接返回success不执行更新")
        void testUpdateItem_SameCount() {
            testCartItem.setGoodsCount(2);
            NewBeeMallShoppingCartItem dbItem = new NewBeeMallShoppingCartItem();
            dbItem.setCartItemId(1L);
            dbItem.setUserId(100L);
            dbItem.setGoodsCount(2);

            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(dbItem);

            String result = shoppingCartService.updateNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(shoppingCartItemMapper, never()).updateByPrimaryKeySelective(any());
        }

        @Test
        @DisplayName("数据库更新失败 - 应返回database error")
        void testUpdateItem_DbError() {
            testCartItem.setGoodsCount(3);
            NewBeeMallShoppingCartItem dbItem = new NewBeeMallShoppingCartItem();
            dbItem.setCartItemId(1L);
            dbItem.setUserId(100L);
            dbItem.setGoodsCount(2);

            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(dbItem);
            when(shoppingCartItemMapper.updateByPrimaryKeySelective(any(NewBeeMallShoppingCartItem.class))).thenReturn(0);

            String result = shoppingCartService.updateNewBeeMallCartItem(testCartItem);

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("获取购物车项")
    class GetCartItemTests {

        @Test
        @DisplayName("正常获取 - 应返回购物车项")
        void testGetCartItemById_Success() {
            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(testCartItem);

            NewBeeMallShoppingCartItem result = shoppingCartService.getNewBeeMallCartItemById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getCartItemId());
        }

        @Test
        @DisplayName("项不存在 - 应返回null")
        void testGetCartItemById_NotExist() {
            when(shoppingCartItemMapper.selectByPrimaryKey(999L)).thenReturn(null);

            NewBeeMallShoppingCartItem result = shoppingCartService.getNewBeeMallCartItemById(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("删除购物车项")
    class DeleteCartItemTests {

        @Test
        @DisplayName("正常删除 - 应返回true")
        void testDelete_Success() {
            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(testCartItem);
            when(shoppingCartItemMapper.deleteByPrimaryKey(1L)).thenReturn(1);

            Boolean result = shoppingCartService.deleteById(1L, 100L);

            assertTrue(result);
        }

        @Test
        @DisplayName("购物车项不存在 - 应返回false")
        void testDelete_NotExist() {
            when(shoppingCartItemMapper.selectByPrimaryKey(999L)).thenReturn(null);

            Boolean result = shoppingCartService.deleteById(999L, 100L);

            assertFalse(result);
        }

        @Test
        @DisplayName("删除他人购物车项 - 应返回false")
        void testDelete_WrongUser() {
            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(testCartItem);

            Boolean result = shoppingCartService.deleteById(1L, 999L);

            assertFalse(result);
            verify(shoppingCartItemMapper, never()).deleteByPrimaryKey(anyLong());
        }

        @Test
        @DisplayName("数据库删除失败 - 应返回false")
        void testDelete_DbError() {
            when(shoppingCartItemMapper.selectByPrimaryKey(1L)).thenReturn(testCartItem);
            when(shoppingCartItemMapper.deleteByPrimaryKey(1L)).thenReturn(0);

            Boolean result = shoppingCartService.deleteById(1L, 100L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("获取我的购物车列表")
    class GetMyShoppingCartItemsTests {

        @Test
        @DisplayName("正常获取购物车列表 - 应返回包含商品信息的VO列表")
        void testGetMyShoppingCartItems_Success() {
            when(shoppingCartItemMapper.selectByUserId(eq(100L), anyInt())).thenReturn(Arrays.asList(testCartItem));
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));

            List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(100L);

            assertNotNull(result);
            assertEquals(1, result.size());
            NewBeeMallShoppingCartItemVO vo = result.get(0);
            assertEquals(10L, vo.getGoodsId());
            assertEquals("测试商品", vo.getGoodsName());
            assertEquals("/img/test.jpg", vo.getGoodsCoverImg());
            assertEquals(100, vo.getSellingPrice());
        }

        @Test
        @DisplayName("购物车为空 - 应返回空列表")
        void testGetMyShoppingCartItems_Empty() {
            when(shoppingCartItemMapper.selectByUserId(eq(100L), anyInt())).thenReturn(Collections.emptyList());

            List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(100L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("商品名称超长 - 应截断并添加省略号")
        void testGetMyShoppingCartItems_LongName() {
            testGoods.setGoodsName("这是一个超过二十八个字符的非常长的商品名称用于测试截断功能是否正常工作");
            when(shoppingCartItemMapper.selectByUserId(eq(100L), anyInt())).thenReturn(Arrays.asList(testCartItem));
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));

            List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(100L);

            assertTrue(result.get(0).getGoodsName().endsWith("..."));
            // 截断后长度应为28+3=31
            assertEquals(31, result.get(0).getGoodsName().length());
        }

        @Test
        @DisplayName("商品数据不匹配(已删除) - 对应项不在返回列表中")
        void testGetMyShoppingCartItems_GoodsNotFound() {
            when(shoppingCartItemMapper.selectByUserId(eq(100L), anyInt())).thenReturn(Arrays.asList(testCartItem));
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.emptyList());

            List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(100L);

            // 商品数据不存在，购物车项不会出现在返回列表中
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("多个购物车项 - 应正确关联商品数据")
        void testGetMyShoppingCartItems_MultipleItems() {
            NewBeeMallShoppingCartItem cartItem2 = new NewBeeMallShoppingCartItem();
            cartItem2.setCartItemId(2L);
            cartItem2.setUserId(100L);
            cartItem2.setGoodsId(20L);
            cartItem2.setGoodsCount(1);

            NewBeeMallGoods goods2 = new NewBeeMallGoods();
            goods2.setGoodsId(20L);
            goods2.setGoodsName("商品二");
            goods2.setGoodsCoverImg("/img/test2.jpg");
            goods2.setSellingPrice(200);

            when(shoppingCartItemMapper.selectByUserId(eq(100L), anyInt()))
                    .thenReturn(Arrays.asList(testCartItem, cartItem2));
            when(goodsMapper.selectByPrimaryKeys(anyList()))
                    .thenReturn(Arrays.asList(testGoods, goods2));

            List<NewBeeMallShoppingCartItemVO> result = shoppingCartService.getMyShoppingCartItems(100L);

            assertEquals(2, result.size());
        }
    }
}
