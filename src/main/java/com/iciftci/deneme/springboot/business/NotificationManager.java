package com.iciftci.deneme.springboot.business;


import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.iciftci.deneme.springboot.model.Notification;
import com.iciftci.deneme.springboot.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class NotificationManager {

    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");

    @Autowired
    private NotificationRepository repository;
    private SetMultimap<String, SseEmitter> subscriptions;


    public NotificationManager() {
        subscriptions = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());
    }

    public Notification create(Notification notification) {
        if (!CHANNEL_PATTERN.matcher(notification.getChannel()).matches()) {
            return null;
        }

        Notification saved = null;
        if (!StringUtils.isEmpty(notification.getTag())) {
            saved = repository.findByTag(notification.getChannel(), notification.getTag());
        }

        // Find according to key
        if (saved == null) {
            notification.setCreated(new Date());
            if(notification.getTtl() > 0) {
                notification.setExpired(Date.from(notification.getCreated().toInstant().plusSeconds(notification.getTtl())));
            } else {
                notification.setExpired(null);
            }

            saved = repository.save(notification);
        } else {
            copy(notification, saved);
        }

        saved = repository.save(saved);
        emit(saved);

        return saved;
    }


    public Notification remove(String id) {
        Notification notification = null;
        Optional<Notification> old = repository.findById(id);
        if (old.isPresent()) {
            notification = old.get();
            notification.setState(Notification.DELETED);
            notification = repository.save(notification);
            emit(notification);
        }
        return notification;
    }


    public Notification update(Notification notification) {
        Notification updated = null;
        Optional<Notification> old = repository.findById(notification.getId());
        if (old.isPresent()) {
            Notification oldNotification = old.get();
            if (oldNotification.getState() != Notification.DELETED) {
                copy(notification, oldNotification);
                updated = repository.save(oldNotification);
                emit(updated);

            }
        }

        return updated;
    }


    public List<Notification> getNotifications(String channel, long after) {
        List<Notification> result = Collections.emptyList();
        if (!CHANNEL_PATTERN.matcher(channel).matches()) {
            return result;
        }
        //TODO ttl sorgusundan elenen notificationlar modified olmamis olacak.

        if (after == 0) {
            result = repository.findByChannel(channel, new Date(), new Sort(Sort.Direction.DESC, "modified"));
        } else {
            result = repository.findByChannelAfter(channel, new Date(), new Date(after), new Sort(Sort.Direction.DESC, "modified"));
        }

        return result;
    }


    public SseEmitter subscribe(final String user, final String[] channels) {

        final SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(1));
        for (String channel : channels) {
            if (CHANNEL_PATTERN.matcher(channel).matches()) {
                subscriptions.put(channel, emitter);
            }
        }

        Runnable removeSseEmitter = () -> {
            log.info("On termination of user: ", user);
            for (String channel : channels) {
                if (CHANNEL_PATTERN.matcher(channel).matches()) {
                    subscriptions.remove(channel, emitter);
                }
            }
        };
        emitter.onCompletion(removeSseEmitter);
        emitter.onTimeout(removeSseEmitter);

        return emitter;
    }


    private void notify(Notification notification) {
        // send to clusters
    }

    public void onNotify(Object message) {
        if (message instanceof Notification) {
            emit((Notification) message);
        }
    }


    @Async
    public void emit(Notification notification) {
        notify(notification);

        String channel = notification.getChannel();
        emit(channel, notification);
    }


    private void emit(String channel, Object message) {

        Set<SseEmitter> allEmitters = new HashSet<>();

        // wildcard impl: tur, tur.sgrs, tur.sgrs.alert all listen tur.sgrs.alert
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < channel.length(); i++) {
            if (channel.charAt(i) == '.') {
                positions.add(i);
            }
        }
        positions.add(channel.length());

        synchronized (subscriptions) {
            for (Integer position : positions) {
                String channelToQuery = channel.substring(0, position);
                log.info("Querying channel: {}", channelToQuery);
                Set<SseEmitter> sseEmitters = subscriptions.get(channelToQuery);
                allEmitters.addAll(sseEmitters);
            }
        }

        for (SseEmitter sseEmitter : allEmitters) {
            try {
                sseEmitter.send(message);
            } catch (IOException e) {
                e.printStackTrace();
                for (Integer position : positions) {
                    String channelParts = channel.substring(0, position);
                    subscriptions.remove(channelParts, sseEmitter);

                }

            }
        }
    }

    private void copy(Notification source, Notification destination) {
        destination.setContent(source.getContent());
        destination.setTag(source.getTag());
        destination.setTtl(source.getTtl());
        if(source.getTtl() > 0) {
            destination.setExpired(Date.from(Instant.now().plusSeconds(source.getTtl())));
        }
        else {
            destination.setExpired(null);
        }
    }

}
