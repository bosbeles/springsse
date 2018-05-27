package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.validation.constraints.NotNull;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    @Autowired
    private MongoOperations operations;

    /*@Override
    public List<Notification> findNotificationsByChannel(String channel) {
        Query query = new Query();
        query.addCriteria(where("channel").is(channel).and("deleted").is(false).and());
        List<Notification> notifications = operations.find(query, Notification.class);

        return notifications;
    }*/
}
