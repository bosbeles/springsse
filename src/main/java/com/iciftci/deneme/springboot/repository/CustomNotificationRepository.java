package com.iciftci.deneme.springboot.repository;

import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.data.domain.Sort;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface CustomNotificationRepository {
    List<Notification> findByChannels(Collection<String> channels, Date after, Sort sort, boolean recursive);
}
