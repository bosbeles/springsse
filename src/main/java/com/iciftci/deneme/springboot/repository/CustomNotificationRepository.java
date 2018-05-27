package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;

public interface CustomNotificationRepository {

    List<Notification> findNotificationByChannel(String channel, Date after, Sort sort);
}
