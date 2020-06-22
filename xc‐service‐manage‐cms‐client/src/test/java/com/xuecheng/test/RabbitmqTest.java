package com.xuecheng.test;

import com.mongodb.client.gridfs.GridFSBucket;
import com.xuecheng.manage_cms_client.ManageCmsClientApplication;
import com.xuecheng.manage_cms_client.config.RabbitmqConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest(classes = ManageCmsClientApplication.class)
@RunWith(SpringRunner.class)
public class RabbitmqTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void test(){
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,"5a751fab6abb5044e0d19eff", "4028858162e0bc0a0162e0bfdf140000");
    }

}
