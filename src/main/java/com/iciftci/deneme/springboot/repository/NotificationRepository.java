package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;

public interface NotificationRepository extends MongoRepository<Notification, String>, CustomNotificationRepository {

    @Query("{ 'channel' : ?0, 'tag' : ?1, 'state' : 0}")
    Notification findByTag(String channel, String tag);
}
