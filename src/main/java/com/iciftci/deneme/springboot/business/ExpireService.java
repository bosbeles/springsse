package com.iciftci.deneme.springboot.business;


import com.iciftci.deneme.springboot.business.NotificationManager;
import com.iciftci.deneme.springboot.model.Notification;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;


@Component
@Slf4j
public class ExpireService {

    @Autowired
    private MongoOperations operations;

    @Autowired
    private NotificationManager manager;

    @Scheduled(fixedDelay = 1000)
    public void expireCheck(){
        Date expireDate = new Date();
        Criteria criteria = where("state").is(Notification.ACTIVE).and("expired").lte(expireDate);

        Query query = new Query();
        query.addCriteria(criteria);
        Update update = new Update();
        update.set("state", Notification.EXPIRED);
        update.set("modified", expireDate);
        operations.updateMulti(query, update, Notification.class);

        query = new Query();
        query.addCriteria(where("state").is(Notification.EXPIRED).and("expired").lte(expireDate).and("modified").is(expireDate));
        List<Notification> notifications = operations.find(query, Notification.class);
        if(notifications.size() > 0) {
            log.info("{} notifications expired.", notifications.size());
        }

        for (Notification notification : notifications) {
            manager.emit(notification);
        }

    }


}
