package com.xuecheng.framework.domain.course.ext;

import com.xuecheng.framework.domain.course.Teachplan;
import com.xuecheng.framework.domain.course.Teachplan1;
import lombok.Data;
import lombok.ToString;

import java.util.List;

/**
 * Created by admin on 2018/2/7.
 */
@Data
@ToString
public class TeachplanNode1 extends Teachplan1 {

    List<TeachplanNode1> children;

    // 媒资文件id
//    String mediaId;
//    // 媒资文件原始名称
//    String mediaFileoriginalname;

}
