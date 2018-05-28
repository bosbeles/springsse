package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface NotificationRepository extends MongoRepository<Notification, String>, CustomNotificationRepository {

    @Query("{ 'channel' : ?0, 'tag' : ?1, 'state' : 0}")
    Notification findByTag(String channel, String tag);


    /*
    @Query("{ {$or: [ {'channel': ?0}, {'channel': {$regex: ?0\\.} } ] }, 'expired' : {$gt : ?1} }")
    List<Notification> findByChannel(String channel, Date after, Sort sort);
    */
}
