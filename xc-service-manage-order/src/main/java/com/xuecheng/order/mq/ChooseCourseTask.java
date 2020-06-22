package com.xuecheng.order.mq;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class ChooseCourseTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChooseCourseTask.class);

    @Autowired
    private TaskService taskService;

    //每隔1分钟扫描消息表，向mq发送消息
    @Scheduled(cron="0 1 * * * *")
    public void sendChoosecourseTask(){
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(new Date());
        calendar.add(GregorianCalendar.MINUTE, -1);
        Date time = calendar.getTime();
        List<XcTask> taskList = taskService.findTaskList(time, 100);
        // 调用service发布消息，将选课信息发送给mq
        for (XcTask xcTask : taskList) {
            // 取任务,乐观锁控制
            if(taskService.getTask(xcTask.getId(),xcTask.getVersion()) >0){
                String ex = xcTask.getMqExchange();
                String routingkey = xcTask.getMqRoutingkey();
                taskService.publish(xcTask,ex,routingkey);
            }
        }

    }
    /**
     * 接收选课响应结果
     */
    @RabbitListener(queues = {RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE})
    public void receiveFinishChoosecourseTask(XcTask task) throws IOException {
        //接收到 的消息id
        String id = task.getId();
        //删除任务，添加历史任务
        taskService.finishTask(id);
    }



//    @Scheduled(cron="0/3 * * * * *")//每隔3秒执行一次
//    public void task1(){
//        LOGGER.info("===============测试定时任务1开始===============");
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        LOGGER.info("===============测试定时任务1结束===============");
//    }
//
//
//    @Scheduled(cron="0/3 * * * * *")//每隔3秒执行一次
//    public void task2(){
//        LOGGER.info("===============测试定时任务2开始===============");
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        LOGGER.info("===============测试定时任务2结束===============");
//    }
}
