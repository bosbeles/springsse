package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface NotificationRepository extends MongoRepository<Notification, String>, CustomNotificationRepository {

    @Query("{ 'key' : ?0, 'deleted' : false, 'expired' : {$gt : ?1} }")
    Notification findNotificationByKey(String key, Date after);

    @Query("{ 'channel' : ?0, 'expired' : {$gt : ?1} }")
    List<Notification> findNotificationByChannel(String channel, Date after);
}
