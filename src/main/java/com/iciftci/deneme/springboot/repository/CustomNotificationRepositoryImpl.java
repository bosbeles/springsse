package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    @Autowired
    private MongoOperations operations;

    @Override
    public List<Notification> findByChannel(String channel, Date expireDate, Sort sort) {
        Query query = new Query();

        query.addCriteria(where("state").is(Notification.ACTIVE).and("expired").gt(expireDate).orOperator(where("channel").is(channel), where("channel").regex(channel.replaceAll("\\.", "\\\\.") + "\\.")));
        query.with(sort);
        List<Notification> notifications = operations.find(query, Notification.class);

        return notifications;
    }

    @Override
    public List<Notification> findByChannelAfter(String channel, Date expireDate, Date fromDate, Sort sort) {
        Query query = new Query();
        query.addCriteria(where("state").is(Notification.ACTIVE).and("expired").lte(expireDate).orOperator(where("channel").is(channel), where("channel").regex(channel.replaceAll("\\.", "\\\\.") + "\\.")));

        Update update = new Update();
        update.set("state", Notification.EXPIRED);
        operations.updateMulti(query, update, Notification.class);


        query = new Query();
        query.addCriteria(Criteria.where("modified").gte(fromDate).orOperator(where("channel").is(channel), where("channel").regex(channel.replaceAll("\\.", "\\\\.") + "\\.")));
        query.with(sort);
        List<Notification> notifications = operations.find(query, Notification.class);

        return notifications;

    }


}
