package ltd.newbee.mall.service.impl;

import ltd.newbee.mall.common.NewBeeMallCategoryLevelEnum;
import ltd.newbee.mall.common.NewBeeMallException;
import ltd.newbee.mall.common.ServiceResultEnum;
import ltd.newbee.mall.controller.vo.NewBeeMallSearchGoodsVO;
import ltd.newbee.mall.dao.GoodsCategoryMapper;
import ltd.newbee.mall.dao.NewBeeMallGoodsMapper;
import ltd.newbee.mall.entity.GoodsCategory;
import ltd.newbee.mall.entity.NewBeeMallGoods;
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
@DisplayName("NewBeeMallGoodsService 单元测试")
class NewBeeMallGoodsServiceImplTest {

    @Mock
    private NewBeeMallGoodsMapper goodsMapper;

    @Mock
    private GoodsCategoryMapper goodsCategoryMapper;

    @InjectMocks
    private NewBeeMallGoodsServiceImpl goodsService;

    private NewBeeMallGoods testGoods;
    private GoodsCategory testCategory;

    @BeforeEach
    void setUp() {
        testGoods = new NewBeeMallGoods();
        testGoods.setGoodsId(1L);
        testGoods.setGoodsName("测试商品");
        testGoods.setGoodsIntro("测试商品简介");
        testGoods.setGoodsCategoryId(100L);
        testGoods.setGoodsCoverImg("/img/test.jpg");
        testGoods.setOriginalPrice(200);
        testGoods.setSellingPrice(100);
        testGoods.setStockNum(50);
        testGoods.setTag("测试标签");
        testGoods.setGoodsSellStatus((byte) 0);

        testCategory = new GoodsCategory();
        testCategory.setCategoryId(100L);
        testCategory.setCategoryLevel((byte) NewBeeMallCategoryLevelEnum.LEVEL_THREE.getLevel());
        testCategory.setCategoryName("三级分类");
    }

    @Nested
    @DisplayName("商品分页查询")
    class GetGoodsPageTests {

        @Test
        @DisplayName("正常分页查询 - 应返回PageResult")
        void testGetGoodsPage_Success() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            List<NewBeeMallGoods> goodsList = Arrays.asList(testGoods);
            when(goodsMapper.findNewBeeMallGoodsList(any(PageQueryUtil.class))).thenReturn(goodsList);
            when(goodsMapper.getTotalNewBeeMallGoods(any(PageQueryUtil.class))).thenReturn(1);

            PageResult result = goodsService.getNewBeeMallGoodsPage(pageUtil);

            assertNotNull(result);
            assertEquals(1, result.getTotalCount());
            assertEquals(1, result.getCurrPage());
            assertEquals(1, result.getList().size());
            verify(goodsMapper).findNewBeeMallGoodsList(any(PageQueryUtil.class));
            verify(goodsMapper).getTotalNewBeeMallGoods(any(PageQueryUtil.class));
        }

        @Test
        @DisplayName("空结果分页查询 - 应返回空列表的PageResult")
        void testGetGoodsPage_Empty() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            when(goodsMapper.findNewBeeMallGoodsList(any(PageQueryUtil.class))).thenReturn(Collections.emptyList());
            when(goodsMapper.getTotalNewBeeMallGoods(any(PageQueryUtil.class))).thenReturn(0);

            PageResult result = goodsService.getNewBeeMallGoodsPage(pageUtil);

            assertNotNull(result);
            assertEquals(0, result.getTotalCount());
            assertTrue(result.getList().isEmpty());
        }
    }

    @Nested
    @DisplayName("保存商品")
    class SaveGoodsTests {

        @Test
        @DisplayName("正常保存商品 - 应返回success")
        void testSaveGoods_Success() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
            when(goodsMapper.insertSelective(any(NewBeeMallGoods.class))).thenReturn(1);

            String result = goodsService.saveNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(goodsMapper).insertSelective(any(NewBeeMallGoods.class));
        }

        @Test
        @DisplayName("分类不存在 - 应返回分类数据异常")
        void testSaveGoods_CategoryNotExist() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(null);

            String result = goodsService.saveNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
            verify(goodsMapper, never()).insertSelective(any(NewBeeMallGoods.class));
        }

        @Test
        @DisplayName("分类不是三级分类 - 应返回分类数据异常")
        void testSaveGoods_CategoryNotLevelThree() {
            testCategory.setCategoryLevel((byte) NewBeeMallCategoryLevelEnum.LEVEL_ONE.getLevel());
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);

            String result = goodsService.saveNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
            verify(goodsMapper, never()).insertSelective(any(NewBeeMallGoods.class));
        }

        @Test
        @DisplayName("同类名下已存在商品 - 应返回已存在相同商品")
        void testSaveGoods_DuplicateGoods() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(new NewBeeMallGoods());

            String result = goodsService.saveNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.SAME_GOODS_EXIST.getResult(), result);
            verify(goodsMapper, never()).insertSelective(any(NewBeeMallGoods.class));
        }

        @Test
        @DisplayName("数据库插入失败 - 应返回database error")
        void testSaveGoods_DbError() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
            when(goodsMapper.insertSelective(any(NewBeeMallGoods.class))).thenReturn(0);

            String result = goodsService.saveNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("批量保存商品")
    class BatchSaveGoodsTests {

        @Test
        @DisplayName("批量保存非空列表 - 应调用batchInsert")
        void testBatchSave_Success() {
            List<NewBeeMallGoods> goodsList = Arrays.asList(testGoods);
            when(goodsMapper.batchInsert(anyList())).thenReturn(1);

            goodsService.batchSaveNewBeeMallGoods(goodsList);

            verify(goodsMapper).batchInsert(goodsList);
        }

        @Test
        @DisplayName("批量保存空列表 - 不应调用batchInsert")
        void testBatchSave_EmptyList() {
            goodsService.batchSaveNewBeeMallGoods(Collections.emptyList());

            verify(goodsMapper, never()).batchInsert(anyList());
        }

        @Test
        @DisplayName("批量保存null - 不应调用batchInsert")
        void testBatchSave_NullList() {
            goodsService.batchSaveNewBeeMallGoods(null);

            verify(goodsMapper, never()).batchInsert(anyList());
        }
    }

    @Nested
    @DisplayName("更新商品")
    class UpdateGoodsTests {

        @Test
        @DisplayName("正常更新商品 - 应返回success")
        void testUpdateGoods_Success() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(testGoods);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
            when(goodsMapper.updateByPrimaryKeySelective(any(NewBeeMallGoods.class))).thenReturn(1);

            String result = goodsService.updateNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
            verify(goodsMapper).updateByPrimaryKeySelective(any(NewBeeMallGoods.class));
        }

        @Test
        @DisplayName("分类不存在 - 应返回分类数据异常")
        void testUpdateGoods_CategoryNotExist() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(null);

            String result = goodsService.updateNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.GOODS_CATEGORY_ERROR.getResult(), result);
        }

        @Test
        @DisplayName("商品不存在 - 应返回未查询到记录")
        void testUpdateGoods_GoodsNotExist() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(null);

            String result = goodsService.updateNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.DATA_NOT_EXIST.getResult(), result);
        }

        @Test
        @DisplayName("同名商品已存在(不同ID) - 应返回已存在相同商品")
        void testUpdateGoods_SameNameExistsDifferentId() {
            NewBeeMallGoods existingGoods = new NewBeeMallGoods();
            existingGoods.setGoodsId(2L); // 不同的ID

            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(testGoods);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(existingGoods);

            String result = goodsService.updateNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.SAME_GOODS_EXIST.getResult(), result);
        }

        @Test
        @DisplayName("同名商品但相同ID - 应允许更新")
        void testUpdateGoods_SameNameSameId() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(testGoods);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(testGoods);
            when(goodsMapper.updateByPrimaryKeySelective(any(NewBeeMallGoods.class))).thenReturn(1);

            String result = goodsService.updateNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.SUCCESS.getResult(), result);
        }

        @Test
        @DisplayName("数据库更新失败 - 应返回database error")
        void testUpdateGoods_DbError() {
            when(goodsCategoryMapper.selectByPrimaryKey(100L)).thenReturn(testCategory);
            when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(testGoods);
            when(goodsMapper.selectByCategoryIdAndName(anyString(), anyLong())).thenReturn(null);
            when(goodsMapper.updateByPrimaryKeySelective(any(NewBeeMallGoods.class))).thenReturn(0);

            String result = goodsService.updateNewBeeMallGoods(testGoods);

            assertEquals(ServiceResultEnum.DB_ERROR.getResult(), result);
        }
    }

    @Nested
    @DisplayName("根据ID获取商品")
    class GetGoodsByIdTests {

        @Test
        @DisplayName("商品存在 - 应返回商品对象")
        void testGetGoodsById_Success() {
            when(goodsMapper.selectByPrimaryKey(1L)).thenReturn(testGoods);

            NewBeeMallGoods result = goodsService.getNewBeeMallGoodsById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getGoodsId());
            assertEquals("测试商品", result.getGoodsName());
        }

        @Test
        @DisplayName("商品不存在 - 应抛出NewBeeMallException")
        void testGetGoodsById_NotFound() {
            when(goodsMapper.selectByPrimaryKey(999L)).thenReturn(null);

            NewBeeMallException exception = assertThrows(NewBeeMallException.class,
                    () -> goodsService.getNewBeeMallGoodsById(999L));

            assertEquals(ServiceResultEnum.GOODS_NOT_EXIST.getResult(), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("批量更新售卖状态")
    class BatchUpdateSellStatusTests {

        @Test
        @DisplayName("批量上架 - 应返回true")
        void testBatchUpdateSellStatus_Success() {
            Long[] ids = {1L, 2L, 3L};
            when(goodsMapper.batchUpdateSellStatus(ids, 0)).thenReturn(3);

            Boolean result = goodsService.batchUpdateSellStatus(ids, 0);

            assertTrue(result);
            verify(goodsMapper).batchUpdateSellStatus(ids, 0);
        }

        @Test
        @DisplayName("批量下架无匹配记录 - 应返回false")
        void testBatchUpdateSellStatus_NoRecords() {
            Long[] ids = {999L};
            when(goodsMapper.batchUpdateSellStatus(ids, 1)).thenReturn(0);

            Boolean result = goodsService.batchUpdateSellStatus(ids, 1);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("搜索商品")
    class SearchGoodsTests {

        @Test
        @DisplayName("搜索商品列表 - 正常返回并截断长名称")
        void testSearchGoods_Success() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            NewBeeMallGoods longNameGoods = new NewBeeMallGoods();
            longNameGoods.setGoodsId(2L);
            longNameGoods.setGoodsName("这是一个超过二十八个字符的非常长的商品名称用于测试截断功能是否正常工作");
            longNameGoods.setGoodsIntro("这是一个超过三十个字符的非常长的商品简介用于测试截断功能是否正常工作的内容");
            longNameGoods.setSellingPrice(99);
            longNameGoods.setGoodsCoverImg("/img/test.jpg");

            List<NewBeeMallGoods> goodsList = Arrays.asList(testGoods, longNameGoods);
            when(goodsMapper.findNewBeeMallGoodsListBySearch(any(PageQueryUtil.class))).thenReturn(goodsList);
            when(goodsMapper.getTotalNewBeeMallGoodsBySearch(any(PageQueryUtil.class))).thenReturn(2);

            PageResult result = goodsService.searchNewBeeMallGoods(pageUtil);

            assertNotNull(result);
            assertEquals(2, result.getTotalCount());
            List<?> list = result.getList();
            assertEquals(2, list.size());

            // 验证长名称被截断
            NewBeeMallSearchGoodsVO vo = (NewBeeMallSearchGoodsVO) list.get(1);
            assertTrue(vo.getGoodsName().endsWith("..."));
            assertTrue(vo.getGoodsIntro().endsWith("..."));
        }

        @Test
        @DisplayName("搜索商品列表 - 短名称不截断")
        void testSearchGoods_ShortName() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            NewBeeMallGoods shortGoods = new NewBeeMallGoods();
            shortGoods.setGoodsId(3L);
            shortGoods.setGoodsName("短名称");
            shortGoods.setGoodsIntro("短简介");
            shortGoods.setSellingPrice(50);
            shortGoods.setGoodsCoverImg("/img/short.jpg");

            when(goodsMapper.findNewBeeMallGoodsListBySearch(any(PageQueryUtil.class))).thenReturn(Arrays.asList(shortGoods));
            when(goodsMapper.getTotalNewBeeMallGoodsBySearch(any(PageQueryUtil.class))).thenReturn(1);

            PageResult result = goodsService.searchNewBeeMallGoods(pageUtil);

            List<?> list = result.getList();
            NewBeeMallSearchGoodsVO vo = (NewBeeMallSearchGoodsVO) list.get(0);
            assertEquals("短名称", vo.getGoodsName());
            assertEquals("短简介", vo.getGoodsIntro());
        }

        @Test
        @DisplayName("搜索结果为空 - 应返回空列表")
        void testSearchGoods_Empty() {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            params.put("limit", 10);
            PageQueryUtil pageUtil = new PageQueryUtil(params);

            when(goodsMapper.findNewBeeMallGoodsListBySearch(any(PageQueryUtil.class))).thenReturn(Collections.emptyList());
            when(goodsMapper.getTotalNewBeeMallGoodsBySearch(any(PageQueryUtil.class))).thenReturn(0);

            PageResult result = goodsService.searchNewBeeMallGoods(pageUtil);

            assertNotNull(result);
            assertEquals(0, result.getTotalCount());
            assertTrue(result.getList().isEmpty());
        }
    }
}
