package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.pojo.PageResult;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IndexService {
    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsRepository repository;


    private static final Logger logger = LoggerFactory.getLogger(IndexService.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public Goods buildGoods(SpuBo spu) {
        Long id = spu.getId();
        //准备数据

        //商品分类名称
        List<String> names = this.categoryClient.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        String all = spu.getTitle() + " " + StringUtils.join(names, " ");

        //sku集合
        List<Sku> skus = this.goodsClient.querySkuBySpuId(id);
        //处理sku
        //把商品价格取出单独存放，便于展示
        List<Long> prices = new ArrayList<>();
        List<Map<String, Object>> skuList = new ArrayList<>();

        for (Sku sku : skus) {
            prices.add(sku.getPrice());
            Map<String, Object> skuMap = new HashMap<>();
            skuMap.put("id", sku.getId());
            skuMap.put("title", sku.getTitle());
            skuMap.put("image", StringUtils.isBlank(sku.getImages()) ? "" : sku.getImages().split(",")[0]);
            skuMap.put("price", sku.getPrice());
            skuList.add(skuMap);
        }

        //spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailById(id);
        //查询分类对应的规格参数
//        SpecParam specParam = new SpecParam();
//        specParam.setCid(spu.getCid3());
//        specParam.setSearching(true);
        List<SpecParam> params = this.specificationClient.querySpecParam(null, spu.getCid3(), true, null);
//        List<SpecParam> params = this.specificationClient.querySpecParam(specParam);

        //通用规格参数值
        Map<Long, String> genericMap = JsonUtils.parseMap(spuDetail.getGenericSpec(), Long.class, String.class);

        //特有规格参数的值
        Map<Long, List<String>> specialMap = JsonUtils.nativeRead(spuDetail.getSpecialSpec(), new TypeReference<Map<Long, List<String>>>() {
        });

        //处理规格参数显示问题，默认显示id+值，处理后显示id对应的名称+值
        Map<String, Object> specs = new HashMap<>();

        for (SpecParam param : params) {
            Long paramId = param.getId();
            String name = param.getName();
            //通用参数
            Object value = null;
            if (param.getGeneric()) {
                //通用参数
                value = genericMap.get(paramId);

                if (param.getNumeric()) {
                    //数值类型需要加分段
                    value = this.chooseSegment(value.toString(), param);
                }
            } else {//特有参数
                value = specialMap.get(paramId);

            }
            if (null == value) {
                value = "其他";
            }
            specs.put(name, value);
        }

        Goods goods = new Goods();
        goods.setId(spu.getId());
        //这里如果要加品牌，可以再写个BrandClient，根据id查品牌
        goods.setAll(all);
        goods.setSubTitle(spu.getSubTitle());
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setPrice(prices);
        goods.setSkus(JsonUtils.serialize(skuList));
        goods.setSpecs(specs);

        return goods;
    }



    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if (segs.length == 2) {
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if (val >= begin && val < end) {
                if (segs.length == 1) {
                    result = segs[0] + p.getUnit() + "以上";
                } else if (begin == 0) {
                    result = segs[1] + p.getUnit() + "以下";
                } else {
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(SearchRequest request) {
        String key = request.getKey();
        if (StringUtils.isBlank(key)) {
            //如果用户没搜索条件,我们可以给默认的,或者返回null
            return null;
        }

        //1 创建查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //查询
        //对结果进行筛选
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id", "skus", "subTitle"}, null));
        //1.1 基本查询
        QueryBuilder query = buildBasicQueryWithFilter(request);
        queryBuilder.withQuery(query);

        //1.2 分页排序
        searchWithPageAndSort(queryBuilder, request);

        //1.3 聚合
        String categoryAggName = "category"; //商品分类聚合名称
        String brandAggName = "brand"; //品牌聚合名称
        //对商品分类进行聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        //对品牌进行聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        //对规格参数进行聚合

        //2 查询,获取结果
        AggregatedPage<Goods> pageInfo = (AggregatedPage<Goods>) this.repository.search(queryBuilder.build());

        //3 解析查询结果
        //3.1 分页信息
        Long total = pageInfo.getTotalElements();
        Long totalPage = Long.valueOf((total.intValue() + request.getSize() - 1) / request.getSize());

        //3.2 商品分类的聚合结果
        List<Category> categories =
                getCategoryAggResult(pageInfo.getAggregation(categoryAggName));
        // 3.3、品牌的聚合结果
        List<Brand> brands = getBrandAggResult(pageInfo.getAggregation(brandAggName));

        //判断商品分类数量,看是否需要对规格参数进行聚合
        List<Map<String, Object>> specs = null;
        if (categories.size() == 1) {
            //如果分类只剩下一个,才进行规格参数过滤
            specs = getSpecs(categories.get(0).getId());
        }
        // 返回结果
        return new SearchResult(total, totalPage, pageInfo.getContent(), categories, brands, specs);
    }

    //构建基本查询条件
    private QueryBuilder buildBasicQueryWithFilter(SearchRequest request) {
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //基本查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND));
        //过滤条件构建器
        BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
        //整理过滤条件
        Map<String, String> filter = request.getFilter();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            //商品分类和品牌已经聚合
            if (key != "cid3" && key != "brandId") {
                key = "specs." + key + ".keyword";
            }
            //字符串类型,进行term查询
            filterQueryBuilder.must(QueryBuilders.termQuery(key, value));
        }
        //添加过滤条件
        queryBuilder.filter(filterQueryBuilder);
        return queryBuilder;
    }

    //聚合规格参数
    private List<Map<String, Object>> getSpecs(Long cid) {
        try {
            //根据分类查询规格
//            SpecParam specParam = new SpecParam();
//            specParam.setCid(cid);
//            specParam.setSearching(true);
            List<SpecParam> params = this.specificationClient.querySpecParam(null, cid, true, null);
//            List<SpecParam> params = this.specificationClient.querySpecParam(specParam);

            //创建集合,保存规格过滤条件
            List<Map<String, Object>> specs = new ArrayList<>();

            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

            //聚合规格参数
            params.forEach(p -> {
                String key = p.getName();
                queryBuilder.addAggregation(AggregationBuilders.terms(key).field("specs." + key + ".keyword"));

            });

            //查询
            Map<String, Aggregation> aggs = ((AggregatedPage<Goods>) this.repository.search(queryBuilder.build())).getAggregations().asMap();

            //解析聚合结果
            params.forEach(param -> {
                Map<String, Object> spec = new HashMap<>();
                String key = param.getName();
                spec.put("k", key);
                StringTerms terms = (StringTerms) aggs.get(key);
                spec.put("options", terms.getBuckets().stream().map(StringTerms.Bucket::getKeyAsString));
                specs.add(spec);
            });
            return specs;
        } catch (Exception e) {
            logger.error("规格聚合出现异常: ", e);
            return null;
        }
    }

    //解析品牌聚合结果
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        try {
            LongTerms brandAgg = (LongTerms) aggregation;
            List<Long> bids = new ArrayList<>();
            for (LongTerms.Bucket bucket : brandAgg.getBuckets()) {
                bids.add(bucket.getKeyAsNumber().longValue());
            }
            //根据id查询品牌
            return this.brandClient.queryBrandByIds(bids);
        } catch (Exception e) {
            logger.error("品牌聚合出现异常: ", e);
            return null;
        }
    }

    // 解析商品分类聚合结果
    private List<Category> getCategoryAggResult(Aggregation aggregation) {
        try {
            List<Category> categories = new ArrayList<>();
            LongTerms categoryAgg = (LongTerms) aggregation;
            List<Long> cids = new ArrayList<>();
            for (LongTerms.Bucket bucket : categoryAgg.getBuckets()) {
                cids.add(bucket.getKeyAsNumber().longValue());
            }
            // 根据id查询分类名称
            List<String> names = this.categoryClient.queryNameByIds(cids);

            for (int i = 0; i < names.size(); i++) {
                Category c = new Category();
                c.setId(cids.get(i));
                c.setName(names.get(i));
                categories.add(c);
            }
            return categories;
        } catch (Exception e) {
            logger.error("分类聚合出现异常：", e);
            return null;
        }
    }

    // 构建基本查询条件
    private void searchWithPageAndSort(NativeSearchQueryBuilder queryBuilder, SearchRequest request) {
        // 准备分页参数
        int page = request.getPage();
        int size = request.getSize();

        // 1、分页
        queryBuilder.withPageable(PageRequest.of(page - 1, size));
        // 2、排序
        String sortBy = request.getSortBy();
        Boolean desc = request.getDescending();
        if (StringUtils.isNotBlank(sortBy)) {
            // 如果不为空，则进行排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(desc ? SortOrder.DESC : SortOrder.ASC));
        }
    }

    public void createIndex(Long id){
        Spu spu = this.goodsClient.querySpuById(id);
        SpuBo spuBo = new SpuBo();
        BeanUtils.copyProperties(spu,spuBo);
        Goods goods = this.buildGoods(spuBo);
        this.repository.save(goods);
    }

    public void deleteIndex(Long id) {
        this.repository.deleteById(id);
    }
}