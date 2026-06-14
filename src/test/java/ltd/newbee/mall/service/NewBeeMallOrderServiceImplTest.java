package ltd.newbee.mall.service;

import ltd.newbee.mall.common.NewBeeMallException;
import ltd.newbee.mall.common.NewBeeMallOrderStatusEnum;
import ltd.newbee.mall.common.PayStatusEnum;
import ltd.newbee.mall.common.PayTypeEnum;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallShoppingCartItemVO;
import ltd.newbee.mall.controller.vo.NewBeeMallUserVO;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderItemMapper;
import ltd.newbee.mall.dao.NewBeeMallOrderMapper;
import ltd.newbee.mall.dao.NewBeeMallShoppingCartItemMapper;
import ltd.newbee.mall.entity.NewBeeMallGoods;
import ltd.newbee.mall.entity.NewBeeMallOrder;
import ltd.newbee.mall.entity.NewBeeMallOrderItem;
import ltd.newbee.mall.service.impl.NewBeeMallOrderServiceImpl;
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
 * NewBeeMallOrderService 单元测试，重点覆盖订单支付流程与状态流转。
 * 包含：支付成功(paySuccess)的状态/支付方式/支付状态变更、订单生成(saveOrder)的下架与库存校验、
 * 取消订单、确认收货、订单信息修改等关键路径。
 */
@ExtendWith(MockitoExtension.class)
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

    private NewBeeMallOrder buildOrder(Long orderId, Long userId, int orderStatus) {
        NewBeeMallOrder order = new NewBeeMallOrder();
        order.setOrderId(orderId);
        order.setOrderNo("202401010001");
        order.setUserId(userId);
        order.setOrderStatus((byte) orderStatus);
        order.setIsDeleted((byte) 0);
        return order;
    }

    private NewBeeMallShoppingCartItemVO buildCartItemVo(Long goodsId, int count, int price) {
        NewBeeMallShoppingCartItemVO vo = new NewBeeMallShoppingCartItemVO();
        vo.setCartItemId(5L);
        vo.setGoodsId(goodsId);
        vo.setGoodsCount(count);
        vo.setSellingPrice(price);
        return vo;
    }

    private NewBeeMallGoods buildGoods(Long goodsId, int sellStatus, int stockNum) {
        NewBeeMallGoods goods = new NewBeeMallGoods();
        goods.setGoodsId(goodsId);
        goods.setGoodsName("商品");
        goods.setGoodsSellStatus((byte) sellStatus);
        goods.setStockNum(stockNum);
        goods.setSellingPrice(50);
        return goods;
    }

    // ---------- paySuccess：支付流程核心 ----------

    @Test
    @DisplayName("支付成功-订单不存在返回订单不存在")
    void paySuccess_orderNotExist_returnsOrderNotExist() {
        when(orderMapper.selectByOrderNo("noExist")).thenReturn(null);

        String result = orderService.paySuccess("noExist", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("支付成功-订单非待支付状态返回状态异常")
    void paySuccess_notPrePayStatus_returnsStatusError() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);

        String result = orderService.paySuccess("202401010001", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        verify(orderMapper, never()).updateByPrimaryKeySelective(any());
    }

    @Test
    @DisplayName("支付成功-待支付订单成功更新状态为已支付/支付成功并记录支付方式")
    void paySuccess_success_updatesPayState() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        String result = orderService.paySuccess("202401010001", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus(), order.getOrderStatus().intValue());
        assertEquals(PayTypeEnum.ALI_PAY.getPayType(), order.getPayType().intValue());
        assertEquals(PayStatusEnum.PAY_SUCCESS.getPayStatus(), order.getPayStatus().intValue());
        assertNotNull(order.getPayTime());
    }

    @Test
    @DisplayName("支付成功-数据库更新失败返回数据库异常")
    void paySuccess_updateFails_returnsDbError() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(any())).thenReturn(0);

        String result = orderService.paySuccess("202401010001", PayTypeEnum.ALI_PAY.getPayType());

        assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
    }

    // ---------- saveOrder：订单生成 ----------

    @Test
    @DisplayName("生成订单-校验通过且各步骤成功返回订单号")
    void saveOrder_success_returnsOrderNo() {
        NewBeeMallUserVO user = new NewBeeMallUserVO();
        user.setUserId(1L);
        user.setAddress("收货地址");
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(buildCartItemVo(2L, 2, 50));

        when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.singletonList(buildGoods(2L, 0, 10)));
        when(shoppingCartItemMapper.deleteBatch(anyList())).thenReturn(1);
        when(goodsMapper.updateStockNum(anyList())).thenReturn(1);
        when(orderMapper.insertSelective(any())).thenReturn(1);
        when(orderItemMapper.insertBatch(anyList())).thenReturn(1);

        String result = orderService.saveOrder(user, cartItems);

        assertNotNull(result);
        assertTrue(result.matches("\\d+"), "订单号应为数字流水号，实际为: " + result);
    }

    @Test
    @DisplayName("生成订单-包含已下架商品时抛出业务异常")
    void saveOrder_containsOffShelfGoods_throwsException() {
        NewBeeMallUserVO user = new NewBeeMallUserVO();
        user.setUserId(1L);
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(buildCartItemVo(2L, 2, 50));
        // sellStatus=1 表示已下架
        when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.singletonList(buildGoods(2L, 1, 10)));

        assertThrows(NewBeeMallException.class, () -> orderService.saveOrder(user, cartItems));
        verify(orderMapper, never()).insertSelective(any());
    }

    @Test
    @DisplayName("生成订单-购买数量超过库存时抛出业务异常")
    void saveOrder_stockNotEnough_throwsException() {
        NewBeeMallUserVO user = new NewBeeMallUserVO();
        user.setUserId(1L);
        // 购买 2 件但库存仅 1 件
        List<NewBeeMallShoppingCartItemVO> cartItems = Collections.singletonList(buildCartItemVo(2L, 2, 50));
        when(goodsMapper.selectByPrimaryKeys(anyList())).thenReturn(Collections.singletonList(buildGoods(2L, 0, 1)));

        assertThrows(NewBeeMallException.class, () -> orderService.saveOrder(user, cartItems));
        verify(shoppingCartItemMapper, never()).deleteBatch(anyList());
    }

    // ---------- cancelOrder ----------

    @Test
    @DisplayName("取消订单-订单不存在返回订单不存在")
    void cancelOrder_orderNotExist_returnsOrderNotExist() {
        when(orderMapper.selectByOrderNo("noExist")).thenReturn(null);

        String result = orderService.cancelOrder("noExist", 1L);

        assertEquals(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("取消订单-非本人订单抛出业务异常")
    void cancelOrder_otherUser_throwsException() {
        NewBeeMallOrder order = buildOrder(1L, 2L, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);

        assertThrows(NewBeeMallException.class, () -> orderService.cancelOrder("202401010001", 1L));
    }

    @Test
    @DisplayName("取消订单-已交易成功的订单返回状态异常")
    void cancelOrder_alreadySuccess_returnsStatusError() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);

        String result = orderService.cancelOrder("202401010001", 1L);

        assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
    }

    @Test
    @DisplayName("取消订单-待支付订单关闭并恢复库存成功返回 success")
    void cancelOrder_success() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);
        when(orderMapper.closeOrder(anyList(), anyInt())).thenReturn(1);
        NewBeeMallOrderItem orderItem = new NewBeeMallOrderItem();
        orderItem.setGoodsId(2L);
        orderItem.setGoodsCount(2);
        when(orderItemMapper.selectByOrderIds(anyList())).thenReturn(Collections.singletonList(orderItem));
        when(goodsMapper.recoverStockNum(anyList())).thenReturn(1);

        String result = orderService.cancelOrder("202401010001", 1L);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    // ---------- finishOrder ----------

    @Test
    @DisplayName("确认收货-出库状态订单成功更新为交易成功")
    void finishOrder_success() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);
        when(orderMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        String result = orderService.finishOrder("202401010001", 1L);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        assertEquals(NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus(), order.getOrderStatus().intValue());
    }

    @Test
    @DisplayName("确认收货-非出库状态返回状态异常")
    void finishOrder_wrongStatus_returnsStatusError() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);

        String result = orderService.finishOrder("202401010001", 1L);

        assertEquals(ServiceResultEnum.ORDER_STATUS_ERROR.getResult(), result);
        verify(orderMapper, never()).updateByPrimaryKeySelective(any());
    }

    // ---------- updateOrderInfo / 查询 ----------

    @Test
    @DisplayName("修改订单信息-出库前订单更新成功返回 success")
    void updateOrderInfo_success() {
        NewBeeMallOrder dbOrder = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
        when(orderMapper.selectByPrimaryKey(1L)).thenReturn(dbOrder);
        when(orderMapper.updateByPrimaryKeySelective(any())).thenReturn(1);

        NewBeeMallOrder param = new NewBeeMallOrder();
        param.setOrderId(1L);
        param.setTotalPrice(100);
        param.setUserAddress("新地址");

        String result = orderService.updateOrderInfo(param);

        assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
    }

    @Test
    @DisplayName("修改订单信息-订单不存在返回数据不存在")
    void updateOrderInfo_orderNotExist_returnsDataNotExist() {
        when(orderMapper.selectByPrimaryKey(1L)).thenReturn(null);

        NewBeeMallOrder param = new NewBeeMallOrder();
        param.setOrderId(1L);

        String result = orderService.updateOrderInfo(param);

        assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
    }

    @Test
    @DisplayName("按订单号查询订单-返回对应订单")
    void getNewBeeMallOrderByOrderNo_returnsOrder() {
        NewBeeMallOrder order = buildOrder(1L, 1L, NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus());
        when(orderMapper.selectByOrderNo("202401010001")).thenReturn(order);

        NewBeeMallOrder result = orderService.getNewBeeMallOrderByOrderNo("202401010001");

        assertNotNull(result);
        assertEquals("202401010001", result.getOrderNo());
    }
}
