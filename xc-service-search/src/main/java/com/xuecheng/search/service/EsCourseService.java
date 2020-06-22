package com.xuecheng.search.service;

import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.directory.SearchResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsCourseService {

    @Value("${xuecheng.elasticsearch.course.index}")
    private String es_index;
    @Value("${xuecheng.elasticsearch.course.type}")
    private String es_type;
    @Value("${xuecheng.elasticsearch.course.source_field}")
    private String source_field;
    @Value("${xuecheng.elasticsearch.media.index}")
    private String media_index;
    @Value("${xuecheng.elasticsearch.media.type}")
    private String media_type;
    @Value("${xuecheng.elasticsearch.media.source_field}")
    private String media_source_field;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        // 设置索引
        SearchRequest searchRequest = new SearchRequest(es_index);
        // 设置类型
        searchRequest.types(es_type);

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建布尔查询对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // source原字段过滤
        String[] source_fields = source_field.split(",");
        searchSourceBuilder.fetchSource(source_fields, new String[]{});

        // 搜索条件
        // 关键字
        if(StringUtils.isNotEmpty(courseSearchParam.getKeyword())){
            MultiMatchQueryBuilder multiMatchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(), "name", "teachplan", "description")
                    .minimumShouldMatch("70%")
                    .field("name", 10);
            boolQueryBuilder.must(multiMatchQueryBuilder);
        }

        // 过滤
        // 根据分类
        if(StringUtils.isNotEmpty(courseSearchParam.getMt())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", courseSearchParam.getMt()));
        }
        if(StringUtils.isNotEmpty(courseSearchParam.getSt())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("st",courseSearchParam.getSt()));
        }
        // 根据难度等级
        if(StringUtils.isNotEmpty(courseSearchParam.getGrade())){
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade",courseSearchParam.getGrade()));
        }


        // 分页
        if(page<=0){
            page = 1;
        }
        if(size<=0){
            size = 20;
        }
        int start = (page- 1)*size;
        searchSourceBuilder.from(start);
        searchSourceBuilder.size(size);

        // 布尔查询
        searchSourceBuilder.query(boolQueryBuilder);

        // 高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        // 设置高亮字段
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);


        // 请求搜索
        searchRequest.source(searchSourceBuilder);

        QueryResult<CoursePub> queryResult = new QueryResult<>();
        List<CoursePub> list = new ArrayList<>();

        try {
            // 执行搜索
            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
            // 获取响应结果
            SearchHits hits = searchResponse.getHits();
            //记录总数
            long totalHits = hits.totalHits;
            queryResult.setTotal(totalHits);

            SearchHit[] searchHits = hits.getHits();
            for (SearchHit hit : searchHits) {
                CoursePub coursePub = new CoursePub();
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String id = (String) sourceAsMap.get("id");
                coursePub.setId(id);

                //取出名称
                String name = (String) sourceAsMap.get("name");

                //取出高亮字段内容
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                if(highlightFields!=null){
                    HighlightField nameField = highlightFields.get("name");
                    if(nameField!=null){
                        Text[] fragments = nameField.getFragments();
                        StringBuffer stringBuffer = new StringBuffer();
                        for (Text str : fragments) {
                            stringBuffer.append(str.string());
                        }
                        name = stringBuffer.toString();
                    }
                }

                coursePub.setName(name);
                //图片
                String pic = (String) sourceAsMap.get("pic");
                coursePub.setPic(pic);
                //价格
                Double price = null;
                try {
                    if(sourceAsMap.get("price")!=null ){
                        price = (Double) sourceAsMap.get("price");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                coursePub.setPrice(price);
                Double price_old = null;
                try {
                    if(sourceAsMap.get("price_old")!=null ){
                        price_old = (Double) sourceAsMap.get("price_old");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                coursePub.setPrice_old(price_old);

                list.add(coursePub);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        queryResult.setList(list);
        QueryResponseResult<CoursePub> coursePubQueryResponseResult = new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
        return coursePubQueryResponseResult;
    }

    // 使用ES客户端向ES请求查询索引信息
    public Map<String, CoursePub> getall(String id) {
        // 定义一个搜索请求对象
        SearchRequest searchRequest = new SearchRequest(es_index);
        // 指定类型
        searchRequest.types(es_type);

        // 定义SearchBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 设置使用termQuery
        searchSourceBuilder.query(QueryBuilders.termQuery("id", id));
        searchRequest.source(searchSourceBuilder);

        // 接收搜索课程信息
        Map<String,CoursePub> map = new HashMap<>();
        try {
            SearchResponse search = restHighLevelClient.search(searchRequest);
            SearchHits searchHits = search.getHits();
            for (SearchHit hit : searchHits) {
                CoursePub coursePub = new CoursePub();
                // 获取源码档内容
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String courseId = (String) sourceAsMap.get("id");
                String name = (String) sourceAsMap.get("name");
                String grade = (String) sourceAsMap.get("grade");
                String charge = (String) sourceAsMap.get("charge");
                String pic = (String) sourceAsMap.get("pic");
                String description = (String) sourceAsMap.get("description");
                String teachplan = (String) sourceAsMap.get("teachplan");
                coursePub.setId(courseId);
                coursePub.setName(name);
                coursePub.setPic(pic);
                coursePub.setGrade(grade);
                coursePub.setTeachplan(teachplan);
                coursePub.setDescription(description);
                map.put(courseId,coursePub);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    public QueryResponseResult<TeachplanMediaPub> getmedia(String[] teachplanIds) {
        // 定义搜索请求对象
        SearchRequest searchRequest = new SearchRequest(media_index);

        searchRequest.types(media_type);

        // 定义searchBuilder
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // source源字段过滤
        String[] souce_fields = media_source_field.split(",");
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id", teachplanIds));
        searchSourceBuilder.fetchSource(souce_fields, new String[]{});
        searchRequest.source(searchSourceBuilder);

        // 数据列表
        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        long total = 0;
        // 执行搜索
        try {
            SearchResponse search = restHighLevelClient.search(searchRequest);
            SearchHits hits = search.getHits();
            SearchHit[] searchHits = hits.getHits();
            total = hits.getTotalHits();
            for (SearchHit searchHit : searchHits) {
                TeachplanMediaPub teachplanMediaPub =new TeachplanMediaPub();
                Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
                //取出课程计划媒资信息
                String courseid = (String) sourceAsMap.get("courseid");
                String media_id = (String) sourceAsMap.get("media_id");
                String media_url = (String) sourceAsMap.get("media_url");
                String teachplan_id = (String) sourceAsMap.get("teachplan_id");
                String media_fileoriginalname = (String) sourceAsMap.get("media_fileoriginalname");
                teachplanMediaPub.setCourseId(courseid);
                teachplanMediaPub.setMediaUrl(media_url);
                teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
                teachplanMediaPub.setMediaId(media_id);
                teachplanMediaPub.setTeachplanId(teachplan_id);
                //将数据加入列表
                teachplanMediaPubList.add(teachplanMediaPub);
            }
        } catch (IOException e) {

            e.printStackTrace();
        }

        // 数据结合
        QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();
        queryResult.setList(teachplanMediaPubList);
        queryResult.setTotal(total);
        QueryResponseResult<TeachplanMediaPub> queryResponseResult = new QueryResponseResult<>(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;
    }
}
