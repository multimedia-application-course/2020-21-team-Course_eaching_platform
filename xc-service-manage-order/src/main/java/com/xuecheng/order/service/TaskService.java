package com.xuecheng.order.service;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private XcTaskRepository xcTaskRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private XcTaskHisRepository xcTaskHisRepository;

    //取出前n条任务,取出指定时间之前处理的任务
    public List<XcTask> findTaskList(Date updateTime, int size) {
        // 设置分页参数

        Pageable pageable = PageRequest.of(0, size);
        Page<XcTask> all = xcTaskRepository.findByUpdateTimeBefore(pageable, updateTime);
        List<XcTask> XcTasks = all.getContent();
        return XcTasks;
    }

    // 发送消息
    @Transactional
    public void publish(XcTask xcTask,String ex,String routingKey) {
        // 查询任务
        Optional<XcTask> taskOptional = xcTaskRepository.findById(xcTask.getId());
        if(taskOptional.isPresent()){
            rabbitTemplate.convertAndSend(ex,routingKey, xcTask);
            // 更新时间
            XcTask one = taskOptional.get();
            one.setUpdateTime(new Date());
            xcTaskRepository.save(one);
        }
    }

    // 获取任务
    @Transactional
    public int getTask(String taskId,int version){
        int i = xcTaskRepository.updateTaskVersion(taskId, version);
        return i;
    }

    // 删除任务，保存到历史任务
    @Transactional
    public void finishTask(String taskId){
        Optional<XcTask> taskOptional = xcTaskRepository.findById(taskId);
        if(taskOptional.isPresent()){
            XcTask xcTask = taskOptional.get();
            xcTask.setDeleteTime(new Date());
            XcTaskHis xcTaskHis = new XcTaskHis();
            BeanUtils.copyProperties(xcTask, xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);
        }
    }


}