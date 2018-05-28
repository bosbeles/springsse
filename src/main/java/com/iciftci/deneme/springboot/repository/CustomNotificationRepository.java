package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.data.domain.Sort;

import java.util.Date;
import java.util.List;

public interface CustomNotificationRepository {

    List<Notification> findByChannel(String channel, Date after, Sort sort);

    List<Notification> findByChannelAfter(String channel, Date expireDate, Date fromDate, Sort sort);
}
