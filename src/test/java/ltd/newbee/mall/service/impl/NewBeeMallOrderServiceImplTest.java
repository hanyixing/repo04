package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.*;
import ltd.newbee.mall.controller.vo.*;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderItemMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallOrder;
import ltd.newbee.mall.entity.NewBeeMallOrderItem;
import ltd.newbee.mall.entity.StockNumDTO;
import ltd.newbee.mall.util.PageQueryUtil;
import ltd.newbee.mall.util.PageResult;
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
@DisplayName("NewBeeMallOrderService 单元测试")
class NewBeeMallOrderServiceImplTest {

    @Mock
    private NewBeeMallOrderMapper orderMapper;

    @Mock
    private NewBeeMallOrderItemMapper orderItemMapper;

    @Mock
    private NewBeeMallShoppingCartItemMapper shoppingCartItemMapper;

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @InjectMocks
    private NewBeeMallOrderServiceImpl orderService;

    private NewBeeMallOrder testOrder;
    private NewBeeMallOrderItem testOrderItem;
    private NewBeeMallGoods testGoods;
    private NewBeeMallUserVO testUser;

    @BeforeEach
    void setUp() {
        testOrder = new NewBeeMallOrder();
        testOrder.setOrderId(1L);
        testOrder.setOrderNo("202301010001");
        testOrder.setUserId(100L);
        testOrder.setTotalPrice(200);
        testOrder.setPayStatus((byte) 0);
        testOrder.setPayType((byte) 0);
        testOrder.setOrderStatus((byte) 0);
        testOrder.setExtraInfo("");
        testOrder.setUserAddress("测试地址");
        testOrder.setIsDeleted((byte) 0);

        testOrderItem = new NewBeeMallOrderItem();
        testOrderItem.setOrderItemId(1L);
        testOrderItem.setOrderId(1L);
        testOrderItem.setGoodsId(10L);
        testOrderItem.setGoodsName("测试商品");
        testOrderItem.setGoodsCoverImg("/img/test.jpg");
        testOrderItem.setSellingPrice(100);
        testOrderItem.setGoodsCount(2);

        testGoods = new NewBeeMallGoods();
        testGoods.setGoodsId(10L);
        testGoods.setGoodsName("测试商品");
        testGoods.setSellingPrice(100);
        testGoods.setStockNum(50);
        testGoods.setGoodsSellStatus((byte) 0);
        testGoods.setGoodsCoverImg("/img/test.jpg");

        testUser = new NewBeeMallUserVO();
        testUser.setUserId(100L);
        testUser.setAddress("测试地址");
        testUser.setNickName("测试用户");
    }

    @Nested
    @DisplayName("订单分页查询")
    class GetOrdersPageTests {

        @Test
        @DisplayName("正常分页查询 - 应返回PageResult")
        void testGetOrdersPage_Success() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            when(orderMapper.findNewBeeMallOrderList(any(PageQueryUtil.class))).thenReturn(Arrays.asList(testOrder));
            when(orderMapper.getTotalNewBeeMallOrders(any(PageQueryUtil.class))).thenReturn(1);

            PageResult result = orderService.getNewBeeMallOrdersPage(pageUtil);

            assertNotNull(result);
            assertEquals(1, result.getTotalCount());
            assertEquals(1, result.getList().size());
        }
    }

    @Nested
    @DisplayName("更新订单信息")
    class UpdateOrderInfoTests {

        @Test
        @DisplayName("正常更新 - 订单状态为待支付(0)应成功")
        void testUpdateOrderInfo_Success() {
            testOrder.setOrderStatus((byte) 0);
            when(orderMapper.selectByPrimaryKey(1L)).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(1);

            NewBeeMallOrder updateOrder = new NewBeeMallOrder();
            updateOrder.setOrderId(1L);
            updateOrder.setTotalPrice(300);
            updateOrder.setUserAddress("新地址");

            String result = orderService.updateOrderInfo(updateOrder);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("订单状态为已支付(1) - 应成功更新")
        void testUpdateOrderInfo_PaidOrder() {
            testOrder.setOrderStatus((byte) 1);
            when(orderMapper.selectByPrimaryKey(1L)).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(1);

            NewBeeMallOrder updateOrder = new NewBeeMallOrder();
            updateOrder.setOrderId(1L);
            updateOrder.setTotalPrice(300);
            updateOrder.setUserAddress("新地址");

            String result = orderService.updateOrderInfo(updateOrder);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("订单状态为出库(3) - 应返回数据不存在(不允许修改)")
        void testUpdateOrderInfo_ShippedOrder() {
            testOrder.setOrderStatus((byte) 3);
            when(orderMapper.selectByPrimaryKey(1L)).thenReturn(testOrder);

            NewBeeMallOrder updateOrder = new NewBeeMallOrder();
            updateOrder.setOrderId(1L);
            updateOrder.setTotalPrice(300);

            String result = orderService.updateOrderInfo(updateOrder);

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }

        @Test
        @DisplayName("订单不存在 - 应返回数据不存在")
        void testUpdateOrderInfo_OrderNotExist() {
            when(orderMapper.selectByPrimaryKey(anyLong())).thenReturn(null);

            NewBeeMallOrder updateOrder = new NewBeeMallOrder();
            updateOrder.setOrderId(999L);

            String result = orderService.updateOrderInfo(updateOrder);

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }

        @Test
        @DisplayName("数据库更新失败 - 应返回database error")
        void testUpdateOrderInfo_DbError() {
            testOrder.setOrderStatus((byte) 0);
            when(orderMapper.selectByPrimaryKey(1L)).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(0);

            NewBeeMallOrder updateOrder = new NewBeeMallOrder();
            updateOrder.setOrderId(1L);
            updateOrder.setTotalPrice(300);
            updateOrder.setUserAddress("新地址");

            String result = orderService.updateOrderInfo(updateOrder);

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("配货完成 checkDone")
    class CheckDoneTests {

        @Test
        @DisplayName("所有订单状态正常 - 应返回success")
        void testCheckDone_Success() {
            testOrder.setOrderStatus((byte) 1);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));
            when(orderMapper.checkDone(anyList())).thenReturn(1);

            String result = orderService.checkDone(new Long[]{1L});

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("订单状态不是已支付 - 应返回错误信息")
        void testCheckDone_WrongStatus() {
            testOrder.setOrderStatus((byte) 0);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));

            String result = orderService.checkDone(new Long[]{1L});

            assertTrue(result.contains("订单的状态不是支付成功无法执行出库操作"));
        }

        @Test
        @DisplayName("订单已删除 - 应返回错误信息")
        void testCheckDone_DeletedOrder() {
            testOrder.setIsDeleted((byte) 1);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));

            String result = orderService.checkDone(new Long[]{1L});

            assertTrue(result.contains("订单的状态不是支付成功无法执行出库操作"));
        }

        @Test
        @DisplayName("未查询到订单 - 应返回数据不存在")
        void testCheckDone_NoOrders() {
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.emptyList());

            String result = orderService.checkDone(new Long[]{999L});

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }

        @Test
        @DisplayName("数据库操作失败 - 应返回database error")
        void testCheckDone_DbError() {
            testOrder.setOrderStatus((byte) 1);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));
            when(orderMapper.checkDone(anyList())).thenReturn(0);

            String result = orderService.checkDone(new Long[]{1L});

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("出库 checkOut")
    class CheckOutTests {

        @Test
        @DisplayName("已支付订单出库 - 应返回success")
        void testCheckOut_PaidOrder() {
            testOrder.setOrderStatus((byte) 1);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));
            when(orderMapper.checkOut(anyList())).thenReturn(1);

            String result = orderService.checkOut(new Long[]{1L});

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("配货完成订单出库 - 应返回success")
        void testCheckOut_PackagedOrder() {
            testOrder.setOrderStatus((byte) 2);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));
            when(orderMapper.checkOut(anyList())).thenReturn(1);

            String result = orderService.checkOut(new Long[]{1L});

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("待支付订单出库 - 应返回错误信息")
        void testCheckOut_PrePayOrder() {
            testOrder.setOrderStatus((byte) 0);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));

            String result = orderService.checkOut(new Long[]{1L});

            assertTrue(result.contains("订单的状态不是支付成功或配货完成无法执行出库操作"));
        }

        @Test
        @DisplayName("未查询到订单 - 应返回数据不存在")
        void testCheckOut_NoOrders() {
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.emptyList());

            String result = orderService.checkOut(new Long[]{999L});

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }
    }

    @Nested
    @DisplayName("关闭订单 closeOrder")
    class CloseOrderTests {

        @Test
        @DisplayName("正常关闭订单并恢复库存 - 应返回success")
        void testCloseOrder_Success() {
            testOrder.setOrderStatus((byte) 1);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));
            when(orderMapper.closeOrder(anyList(), anyInt())).thenReturn(1);
            // mock recoverStockNum
            when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(Arrays.asList(testOrderItem));
            when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

            String result = orderService.closeOrder(new Long[]{1L});

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("已完成订单不能关闭 - 应返回错误信息")
        void testCloseOrder_CompletedOrder() {
            testOrder.setOrderStatus((byte) 4);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));

            String result = orderService.closeOrder(new Long[]{1L});

            assertTrue(result.contains("订单不能执行关闭操作"));
        }

        @Test
        @DisplayName("已关闭订单不能再次关闭 - 应返回错误信息")
        void testCloseOrder_AlreadyClosed() {
            testOrder.setOrderStatus((byte) -1);
            testOrder.setIsDeleted((byte) 0);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));

            String result = orderService.closeOrder(new Long[]{1L});

            assertTrue(result.contains("订单不能执行关闭操作"));
        }

        @Test
        @DisplayName("已删除订单不能关闭 - 应返回错误信息")
        void testCloseOrder_DeletedOrder() {
            testOrder.setIsDeleted((byte) 1);
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testOrder));

            String result = orderService.closeOrder(new Long[]{1L});

            assertTrue(result.contains("订单不能执行关闭操作"));
        }

        @Test
        @DisplayName("未查询到订单 - 应返回数据不存在")
        void testCloseOrder_NoOrders() {
            when(orderMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.emptyList());

            String result = orderService.closeOrder(new Long[]{999L});

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }
    }

    @Nested
    @DisplayName("创建订单 saveOrder")
    class SaveOrderTests {

        private NewBeeMallShoppingCartItemVO cartItemVO;

        @BeforeEach
        void setUpCartItems() {
            cartItemVO = new NewBeeMallShoppingCartItemVO();
            cartItemVO.setCartItemId(1L);
            cartItemVO.setGoodsId(10L);
            cartItemVO.setGoodsCount(2);
            cartItemVO.setSellingPrice(100);
            cartItemVO.setGoodsName("测试商品");
            cartItemVO.setGoodsCoverImg("/img/test.jpg");
        }

        @Test
        @DisplayName("正常创建订单 - 应返回订单号")
        void testSaveOrder_Success() {
            List<NewBeeMallShoppingCartItemVO> cartItems = Arrays.asList(cartItemVO);
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));
            when(shoppingCartItemMapper.deleteBatch(anyList())).thenReturn(1);
            when(goodsMapper.updateStockNum(anyList())).thenReturn(1);
            when(orderMapper.insertSelective(any(NewBeeMallOrder.class))).thenReturn(1);
            when(orderItemMapper.insertBatch(anyList())).thenReturn(1);

            String result = orderService.saveOrder(testUser, cartItems);

            assertNotNull(result);
            assertTrue(result.length() > 0);
            // 验证购物车项被删除
            verify(shoppingCartItemMapper).deleteBatch(anyList());
            // 验证库存被扣减
            verify(goodsMapper).updateStockNum(anyList());
            // 验证订单被创建
            verify(orderMapper).insertSelective(any(NewBeeMallOrder.class));
            // 验证订单项被创建
            verify(orderItemMapper).insertBatch(anyList());
        }

        @Test
        @DisplayName("商品已下架 - 应抛出异常")
        void testSaveOrder_GoodsOffSale() {
            testGoods.setGoodsSellStatus((byte) 1); // 下架
            List<NewBeeMallShoppingCartItemVO> cartItems = Arrays.asList(cartItemVO);
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> orderService.saveOrder(testUser, cartItems));

            assertTrue(exception.getMessage().contains("已下架"));
        }

        @Test
        @DisplayName("库存不足 - 应抛出异常")
        void testSaveOrder_InsufficientStock() {
            testGoods.setStockNum(1); // 库存只有1，但需要2
            List<NewBeeMallShoppingCartItemVO> cartItems = Arrays.asList(cartItemVO);
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> orderService.saveOrder(testUser, cartItems));

            assertEquals(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult(), exception.getMessage());
        }

        @Test
        @DisplayName("删除购物车项失败 - 应抛出异常")
        void testSaveOrder_DeleteCartFailed() {
            List<NewBeeMallShoppingCartItemVO> cartItems = Arrays.asList(cartItemVO);
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));
            when(shoppingCartItemMapper.deleteBatch(anyList())).thenReturn(0);

            assertThrows(NewBeeMallException.class,
                    () -> orderService.saveOrder(testUser, cartItems));
        }

        @Test
        @DisplayName("扣减库存失败 - 应抛出异常")
        void testSaveOrder_UpdateStockFailed() {
            List<NewBeeMallShoppingCartItemVO> cartItems = Arrays.asList(cartItemVO);
            when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Arrays.asList(testGoods));
            when(shoppingCartItemMapper.deleteBatch(anyList())).thenReturn(1);
            when(goodsMapper.updateStockNum(anyList())).thenReturn(0);

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> orderService.saveOrder(testUser, cartItems));

            assertEquals(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult(), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("订单详情 getOrderDetailByOrderNo")
    class GetOrderDetailTests {

        @Test
        @DisplayName("正常获取订单详情 - 应返回OrderDetailVO")
        void testGetOrderDetail_Success() {
            testOrder.setOrderStatus((byte) 1);
            testOrder.setPayType((byte) 1);
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderItemMapper.selectByOrderId(1L)).thenReturn(Arrays.asList(testOrderItem));

            NewBeeMallOrderDetailVO result = orderService.getOrderDetailByOrderNo("202301010001", 100L);

            assertNotNull(result);
            assertEquals("202301010001", result.getOrderNo());
            assertNotNull(result.getNewBeeMallOrderItemVOS());
            assertEquals(1, result.getNewBeeMallOrderItemVOS().size());
        }

        @Test
        @DisplayName("订单不存在 - 应抛出异常")
        void testGetOrderDetail_OrderNotExist() {
            when(orderMapper.selectByOrderNo("nonexistent")).thenReturn(null);

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> orderService.getOrderDetailByOrderNo("nonexistent", 100L));

            assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), exception.getMessage());
        }

        @Test
        @DisplayName("无权访问他人订单 - 应抛出异常")
        void testGetOrderDetail_NoPermission() {
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> orderService.getOrderDetailByOrderNo("202301010001", 999L));

            assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), exception.getMessage());
        }

        @Test
        @DisplayName("订单项为空 - 应抛出异常")
        void testGetOrderDetail_NoItems() {
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.emptyList());

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> orderService.getOrderDetailByOrderNo("202301010001", 100L));

            assertEquals(ServiceResultEnum.ORDER_ITEM_NOT_EXIST_ERROR.getResult(), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("支付成功 paySuccess - 订单支付流程测试")
    class PaySuccessTests {

        @Test
        @DisplayName("支付宝支付成功 - 订单状态从待支付变为已支付")
        void testPaySuccess_AliPay() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(1);

            String result = orderService.paySuccess("202301010001", PayTypeEnum.ALI_PAY.getPayType());

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(orderMapper).updateByPrimaryKeySelective(argThat(order -> {
                assertEquals((byte) NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus(), order.getOrderStatus());
                assertEquals((byte) PayTypeEnum.ALI_PAY.getPayType(), order.getPayType());
                assertEquals((byte) PayStatusEnum.PAY_SUCCESS.getPayStatus(), order.getPayStatus());
                assertNotNull(order.getPayTime());
                assertNotNull(order.getUpdateTime());
                return true;
            }));
        }

        @Test
        @DisplayName("微信支付成功 - 订单状态从待支付变为已支付")
        void testPaySuccess_WeixinPay() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(1);

            String result = orderService.paySuccess("202301010001", PayTypeEnum.WEIXIN_PAY.getPayType());

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(orderMapper).updateByPrimaryKeySelective(argThat(order -> {
                assertEquals((byte) PayTypeEnum.WEIXIN_PAY.getPayType(), order.getPayType());
                return true;
            }));
        }

        @Test
        @DisplayName("非待支付状态不能标记支付成功 - 应返回订单状态异常")
        void testPaySuccess_WrongStatus() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.paySuccess("202301010001", PayTypeEnum.ALI_PAY.getPayType());

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
            verify(orderMapper, never()).updateByPrimaryKeySelective(any());
        }

        @Test
        @DisplayName("已完成订单不能标记支付 - 应返回订单状态异常")
        void testPaySuccess_CompletedOrder() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.paySuccess("202301010001", PayTypeEnum.ALI_PAY.getPayType());

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("订单不存在 - 应返回订单不存在")
        void testPaySuccess_OrderNotExist() {
            when(orderMapper.selectByOrderNo("nonexistent")).thenReturn(null);

            String result = orderService.paySuccess("nonexistent", PayTypeEnum.ALI_PAY.getPayType());

            assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("数据库更新失败 - 应返回database error")
        void testPaySuccess_DbError() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(0);

            String result = orderService.paySuccess("202301010001", PayTypeEnum.ALI_PAY.getPayType());

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("取消订单 cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("正常取消订单并恢复库存 - 应返回success")
        void testCancelOrder_Success() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderMapper.closeOrder(anyList(), eq(NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus()))).thenReturn(1);
            when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(Arrays.asList(testOrderItem));
            when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

            String result = orderService.cancelOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("取消他人订单 - 应抛出无权限异常")
        void testCancelOrder_NoPermission() {
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            assertThrows(NewBeeMallException.class,
                    () -> orderService.cancelOrder("202301010001", 999L));
        }

        @Test
        @DisplayName("已完成订单不能取消 - 应返回状态异常")
        void testCancelOrder_CompletedOrder() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.cancelOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("已手动关闭的订单不能再次取消 - 应返回状态异常")
        void testCancelOrder_AlreadyClosedByUser() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.cancelOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("已过期关闭的订单不能取消 - 应返回状态异常")
        void testCancelOrder_ExpiredOrder() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.cancelOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("商家关闭的订单不能取消 - 应返回状态异常")
        void testCancelOrder_ClosedByJudge() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.cancelOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("订单不存在 - 应返回订单不存在")
        void testCancelOrder_OrderNotExist() {
            when(orderMapper.selectByOrderNo("nonexistent")).thenReturn(null);

            String result = orderService.cancelOrder("nonexistent", 100L);

            assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("确认收货 finishOrder")
    class FinishOrderTests {

        @Test
        @DisplayName("出库状态下确认收货 - 应返回success")
        void testFinishOrder_Success() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(1);

            String result = orderService.finishOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("非出库状态不能确认收货 - 应返回状态异常")
        void testFinishOrder_WrongStatus() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.finishOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("确认他人订单收货 - 应返回无权限")
        void testFinishOrder_NoPermission() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            String result = orderService.finishOrder("202301010001", 999L);

            assertEquals(ServiceResultEnum.NO_PERMISSION_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("订单不存在 - 应返回订单不存在")
        void testFinishOrder_OrderNotExist() {
            when(orderMapper.selectByOrderNo("nonexistent")).thenReturn(null);

            String result = orderService.finishOrder("nonexistent", 100L);

            assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("数据库更新失败 - 应返回database error")
        void testFinishOrder_DbError() {
            testOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus());
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);
            when(orderMapper.updateByPrimaryKeySelective(any(NewBeeMallOrder.class))).thenReturn(0);

            String result = orderService.finishOrder("202301010001", 100L);

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("获取订单项 getOrderItems")
    class GetOrderItemsTests {

        @Test
        @DisplayName("正常获取订单项 - 应返回OrderItemVO列表")
        void testGetOrderItems_Success() {
            when(orderMapper.selectByPrimaryKey(1L)).thenReturn(testOrder);
            when(orderItemMapper.selectByOrderId(1L)).thenReturn(Arrays.asList(testOrderItem));

            List<NewBeeMallOrderItemVO> result = orderService.getOrderItems(1L);

            assertNotNull(result);
            assertEquals(1, result.size());
        }

        @Test
        @DisplayName("订单不存在 - 应返回null")
        void testGetOrderItems_OrderNotExist() {
            when(orderMapper.selectByPrimaryKey(999L)).thenReturn(null);

            List<NewBeeMallOrderItemVO> result = orderService.getOrderItems(999L);

            assertNull(result);
        }

        @Test
        @DisplayName("订单项为空 - 应返回null")
        void testGetOrderItems_NoItems() {
            when(orderMapper.selectByPrimaryKey(1L)).thenReturn(testOrder);
            when(orderItemMapper.selectByOrderId(1L)).thenReturn(Collections.emptyList());

            List<NewBeeMallOrderItemVO> result = orderService.getOrderItems(1L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("根据订单号查询 getNewBeeMallOrderByOrderNo")
    class GetOrderByOrderNoTests {

        @Test
        @DisplayName("正常查询 - 应返回订单对象")
        void testGetOrderByOrderNo_Success() {
            when(orderMapper.selectByOrderNo("202301010001")).thenReturn(testOrder);

            NewBeeMallOrder result = orderService.getNewBeeMallOrderByOrderNo("202301010001");

            assertNotNull(result);
            assertEquals("202301010001", result.getOrderNo());
        }

        @Test
        @DisplayName("订单不存在 - 应返回null")
        void testGetOrderByOrderNo_NotExist() {
            when(orderMapper.selectByOrderNo("nonexistent")).thenReturn(null);

            NewBeeMallOrder result = orderService.getNewBeeMallOrderByOrderNo("nonexistent");

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("我的订单列表 getMyOrders")
    class GetMyOrdersTests {

        @Test
        @DisplayName("正常获取我的订单 - 应返回包含订单项的PageResult")
        void testGetMyOrders_Success() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            when(orderMapper.getTotalNewBeeMallOrders(any(PageQueryUtil.class))).thenReturn(1);
            when(orderMapper.findNewBeeMallOrderList(any(PageQueryUtil.class))).thenReturn(Arrays.asList(testOrder));
            when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(Arrays.asList(testOrderItem));

            PageResult result = orderService.getMyOrders(pageUtil);

            assertNotNull(result);
            assertEquals(1, result.getTotalCount());
            assertFalse(result.getList().isEmpty());
        }

        @Test
        @DisplayName("无订单记录 - 应返回空的PageResult")
        void testGetMyOrders_Empty() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            when(orderMapper.getTotalNewBeeMallOrders(any(PageQueryUtil.class))).thenReturn(0);
            when(orderMapper.findNewBeeMallOrderList(any(PageQueryUtil.class))).thenReturn(Collections.emptyList());

            PageResult result = orderService.getMyOrders(pageUtil);

            assertNotNull(result);
            assertEquals(0, result.getTotalCount());
            assertTrue(result.getList().isEmpty());
        }
    }

    @Nested
    @DisplayName("恢复库存 recoverStockNum")
    class RecoverStockNumTests {

        @Test
        @DisplayName("正常恢复库存 - 应返回true")
        void testRecoverStockNum_Success() {
            when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(Arrays.asList(testOrderItem));
            when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

            Boolean result = orderService.recoverStockNum(Arrays.asList(1L));

            assertTrue(result);
            verify(goodsMapper).recoverStockNum(anyList());
        }

        @Test
        @DisplayName("恢复库存失败 - 应抛出异常并返回false")
        void testRecoverStockNum_Failed() {
            when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(Arrays.asList(testOrderItem));
            when(goodsMapper.recoverStockNum(anyList())).thenReturn(0);

            assertThrows(NewBeeMallException.class,
                    () -> orderService.recoverStockNum(Arrays.asList(1L)));
        }
    }
}
