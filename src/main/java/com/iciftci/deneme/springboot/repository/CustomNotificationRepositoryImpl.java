package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    @Autowired
    private MongoOperations operations;

    @Override
    public List<Notification> findNotificationByChannel(String channel, Date after, Sort sort) {
        Query query = new Query();
        query.addCriteria(where("expired").gt(after).orOperator(where("channel").is(channel), where("channel").regex( channel.replaceAll("\\.", "\\\\.") + "\\.")));
        query.with(sort);
        List<Notification> notifications = operations.find(query, Notification.class);

        return notifications;
    }

}
