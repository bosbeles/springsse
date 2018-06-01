package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.springframework.data.mongodb.core.query.Criteria.where;

public class CustomNotificationRepositoryImpl implements CustomNotificationRepository {

    @Autowired
    private MongoOperations operations;


    @Override
    public List<Notification> findByChannels(Collection<String> channels, Date expireDate, Sort sort, boolean recursive) {
        Query query = new Query();


        query.addCriteria(where("state").is(Notification.ACTIVE).andOperator(new Criteria().orOperator(where("expired").exists(false), where("expired").gt(expireDate)), getChannelRegex(channels, recursive)));
        query.with(sort);
        List<Notification> notifications = operations.find(query, Notification.class);

        return notifications;
    }


    private Criteria getChannelRegex(Collection<String> channels, boolean recursive) {
        Criteria[] inCriteria = new Criteria[recursive ? channels.size() + 1 : 1];
        inCriteria[0] = where("channel").in(channels);
        if (recursive) {
            int i = 1;
            for (String channel : channels) {
                inCriteria[i++] = where("channel").regex(channel.replaceAll("\\.", "\\\\.") + "\\.");
            }
        }

        return new Criteria().orOperator(inCriteria);
    }


}
