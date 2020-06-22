package com.xuecheng.manage_course.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.ext.TeachplanNode1;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.domain.course.response.CoursePublishResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseService {
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanMapper1 teachplanMapper1;
    @Autowired
    private TeachplanRepository teachplanRepository;
    @Autowired
    private CourseBaseRepository courseBaseRepository;
    @Autowired
    private CoursePicRepository coursePicRepository;
    @Autowired
    private CourseMarketRepository courseMarketRepository;
    @Autowired
    private CmsPageClient cmsPageClient;
    @Autowired
    private TeachplanMediaRepository teachplanMediaRepository;
    @Autowired
    private TeachplanMediaPubRepository teachplanMediaPubRepository;
    @Autowired
    private CourseMapper courseMapper;


    @Value("${course-publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course-publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course-publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course-publish.siteId}")
    private String publish_siteId;
    @Value("${course-publish.templateId}")
    private String publish_templateId;
    @Value("${course-publish.previewUrl}")
    private String previewUrl;


    // 查询课程计划
    public TeachplanNode findTeachplanList(String courseId){
        return teachplanMapper.selectList(courseId);
    }

    public TeachplanNode1 findTeachplanList1(String courseId){
        return teachplanMapper1.selectList(courseId);
    }
    // 添加课程计划
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        // 校验课程id和课程计划名称
        if(teachplan == null || StringUtils.isEmpty(teachplan.getCourseid()) || StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        // 取出课程id
        String courseid = teachplan.getCourseid();
        // 取出父节点id
        String parentid = teachplan.getParentid();
        if(StringUtils.isEmpty(parentid)){
            parentid = getgetTeachplanRoot(courseid);
        }
        // 取出父节点信息
        Optional<Teachplan> teachplanOptional = teachplanRepository.findById(parentid);
        if(!teachplanOptional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        // 父节点
        Teachplan teachPlanParent = teachplanOptional.get();
        // 父节点级别
        String parentGrade = teachPlanParent.getGrade();
        // 设置父节点
        teachplan.setParentid(parentid);
        teachplan.setStatus("0");   // 未发布
        // 子节点级别，根据父节点判断
        // 子节点级别
        if(parentGrade.equals("1")){
            teachplan.setGrade("2");
        }else if(parentGrade.equals("2")){
            teachplan.setGrade("3");
        }
        //设置课程id
        teachplan.setCourseid(teachPlanParent.getCourseid());
        teachplanRepository.save(teachplan);
        return new ResponseResult(CommonCode.SUCCESS);

    }

    private String getgetTeachplanRoot(String courseId) {
         // 校验课程id
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(!optional.isPresent()){
            return null;
        }
        CourseBase courseBase = optional.get();
        // 取出课程计划根节点
        List<Teachplan> teachplanList = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        if(teachplanList == null || teachplanList.size() == 0){
            // 新增一个结点
            Teachplan teachplan = new Teachplan();
            teachplan.setCourseid(courseId);
            teachplan.setPname(courseBase.getName());
            teachplan.setParentid("0");
            teachplan.setGrade("1");//1级
            teachplan.setStatus("0");//未发布
            teachplanRepository.save(teachplan);
            return teachplan.getId();
        }
        Teachplan teachplan = teachplanList.get(0);
        return teachplan.getId();
    }

    // 添加课程图片
    @Transactional
    public ResponseResult saveCoursePic(String courseId, String pic) {
        CoursePic coursePic = null;
        // 查询课程图片
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        if(picOptional.isPresent()){
            coursePic = picOptional.get();
        }
        // 课程图片不存在则新建对象
        if(coursePic == null){
            coursePic = new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }


    // 查询课程图片
    public CoursePic findCoursepic(String courseId) {
        Optional<CoursePic> picOptional = coursePicRepository.findById(courseId);
        if(picOptional.isPresent()){
            return picOptional.get();
        }
        return null;
    }

    // 删除课程图片
    @Transactional
    public ResponseResult deleteCoursePic(String courseId) {
        // 执行删除， 1表示成功，0表示失败
        long result = coursePicRepository.deleteByCourseid(courseId);
        if(result>0){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);

    }

    // 课程视图查询
    public CourseView getCoruseView(String id) {

        CourseView courseView = new CourseView();

        // 课程信息
        Optional<CourseBase> optional = courseBaseRepository.findById(id);
        if(optional.isPresent()){
            CourseBase courseBase = optional.get();
            courseView.setCourseBase(courseBase);
        }
        //营销信息
        Optional<CourseMarket> courseMarketOptional = courseMarketRepository.findById(id);
        if(courseMarketOptional.isPresent()){
            CourseMarket courseMarket = courseMarketOptional.get();
            courseView.setCourseMarket(courseMarket);
        }
        // 图片信息
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if(picOptional.isPresent()){
            CoursePic coursePic = picOptional.get();
            courseView.setCoursePic(coursePic);
        }
        // 课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(teachplanNode);
        return courseView;
    }



    // 课程预览
    public CoursePublishResult preview(String courseId) {
        // 查询课程
        CourseBase courseBase = findCourseBaseById(courseId);

        // 请求cms添加页面
        // 准备cmsPage信息
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setTemplateId(publish_templateId);
        cmsPage.setPageName(courseId+".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        cmsPage.setDataUrl(publish_dataUrlPre+courseId);
        // 远程调用cms
        CmsPageResult cmsPageResult = cmsPageClient.save(cmsPage);
        if(!cmsPageResult.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        CmsPage one = cmsPageResult.getCmsPage();
        String pageId = one.getPageId();
        //页面url
        String pageUrl = previewUrl+pageId;
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }


    //根据id查询课程基本信息
    public CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(courseId);
        if(baseOptional.isPresent()){
            CourseBase courseBase = baseOptional.get();
            return courseBase;
        }
        ExceptionCast.cast(CourseCode.COURSE_GET_NOTEXISTS);
        return null;
    }

    @Transactional
    public CoursePublishResult publish(String courseId) {
        // 调用cms一键发布课程详情页面到服务器
        // 准备页面信息
        // 查询课程
        CourseBase courseBase = findCourseBaseById(courseId);
        CmsPage cmsPage = new CmsPage();
        cmsPage.setSiteId(publish_siteId);
        cmsPage.setTemplateId(publish_templateId);
        cmsPage.setPageName(courseId+".html");
        cmsPage.setPageAliase(courseBase.getName());
        cmsPage.setPageWebPath(publish_page_webpath);
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);
        cmsPage.setDataUrl(publish_dataUrlPre+courseId);

        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if(!cmsPostPageResult.isSuccess()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        // 保存课程发布状态为"已发布"
        CourseBase courseBaseStatus = saveCoursePubState(courseId);
        // 保存课程索引信息
        // 缓存课程信息

        // 得到页面url
        String pageUrl = cmsPostPageResult.getPageUrl();
        //保存课程计划媒资信息到待索引表
        saveTeachplanMediaPub(courseId);
        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);

    }


    //保存课程计划媒资信息
    private void saveTeachplanMediaPub(String courseId){
        // 先删除TeachplanMediaPub的数据
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        // 从teachplanMedia中查询
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);

        List<TeachplanMediaPub> teachplanMediaPubList = new ArrayList<>();
        // 将teachplanMediaList数据放到teachplanMediaPub中
        for (TeachplanMedia teachplanMedia : teachplanMediaList) {
            TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
            BeanUtils.copyProperties(teachplanMedia, teachplanMediaPub);
            // 添加时间戳
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubList.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubList);
    }

    // 更新课程发布状态
    private CourseBase saveCoursePubState(String courseId) {
        CourseBase courseBase = this.findCourseBaseById(courseId);
        // 更新发布状态
        courseBase.setStatus("202002");
        CourseBase save = courseBaseRepository.save(courseBase);
        return save;
    }


    // 保存课程信息与媒资信息关联
    @Transactional
    public ResponseResult savemedia(TeachplanMedia teachplanMedia) {
        if(teachplanMedia == null || StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        // 校验课程计划结点是否是3级
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if(!optional.isPresent()){
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_ISNULL);
        }
        // 查询教学计划
        Teachplan teachplan = optional.get();
        // 取出课程等级
        String grade = teachplan.getGrade();
        if (StringUtils.isEmpty(grade)|| !grade.equals("3")) {
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }
        // 查询teachplanMedia
        Optional<TeachplanMedia> mediaOptional = teachplanMediaRepository.findById(teachplanId);
        TeachplanMedia one = null;
        if(mediaOptional.isPresent()){
            one = mediaOptional.get();
        }else {
            one = new TeachplanMedia();
        }
        // 将teanplanMedia保存到数据库
        one.setTeachplanId(teachplanId);
        one.setCourseId(teachplanMedia.getCourseId());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        teachplanMediaRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    // 查询课程列表
    public QueryResponseResult<CourseInfo> findCourseList(String companyId, int page, int size, CourseListRequest courseListRequest) {
        if(courseListRequest == null){
            courseListRequest = new CourseListRequest();
        }
        // 将公司id传入
        courseListRequest.setCompanyId(companyId);
        // 分页
        if(page<=0){
            page = 0;
        }
        if(size<=0){
            size = 20;
        }
        PageHelper.startPage(page, size);
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);

        List<CourseInfo> list = courseListPage.getResult();
        long total = courseListPage.getTotal();
        QueryResult<CourseInfo> courseIncfoQueryResult = new QueryResult<CourseInfo>();
        courseIncfoQueryResult.setList(list);
        courseIncfoQueryResult.setTotal(total);
        return new QueryResponseResult<CourseInfo>(CommonCode.SUCCESS, courseIncfoQueryResult);
    }
}

