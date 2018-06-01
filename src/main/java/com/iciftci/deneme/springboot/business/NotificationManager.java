package com.iciftci.deneme.springboot.business;


import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.iciftci.deneme.springboot.model.Notification;
import com.iciftci.deneme.springboot.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationManager {

    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");


    @Autowired
    private NotificationRepository repository;
    private SetMultimap<String, Subscription> subscriptions;
    private Set<String> channelsToBeNotified;


    public NotificationManager() {
        subscriptions = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());
        channelsToBeNotified = ConcurrentHashMap.newKeySet();
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
            if (notification.getTtl() > 0) {
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


    public List<Notification> getNotifications(String[] channels, boolean recursive) {
        List<Notification> result = Collections.emptyList();
        final Set<String> validChannels = Arrays.stream(channels).filter(c -> CHANNEL_PATTERN.matcher(c).matches()).collect(Collectors.toSet());
        if (validChannels.isEmpty()) {
            return result;
        }
        //TODO ttl sorgusundan elenen notificationlar modified olmamis olacak.
        result = repository.findByChannels(validChannels, new Date(), new Sort(Sort.Direction.DESC, "modified"), recursive);

        return result;
    }


    public SseEmitter subscribe(final String user, final String[] channels) {

        final SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(1));
        final Set<String> validChannels = Arrays.stream(channels).filter(c -> CHANNEL_PATTERN.matcher(c).matches()).collect(Collectors.toSet());
        final Subscription subscription = new Subscription(emitter, validChannels);
        validChannels.stream().forEach(c -> subscriptions.put(c, subscription));
        subscription.onClose(() -> {
            log.info("On termination of user: {} with {} ", user, subscription);
            subscription.getChannels().stream().forEach(c -> subscriptions.remove(c, subscription));
        });


        return emitter;
    }


    private void notifyClusters(Object channelList) {
        // send to clusters
    }

    public void onClusterNotified(Object message) {
        if (message instanceof Set) {
            channelsToBeNotified.addAll((Set<String>) message);
        }
    }

    public void emit(Notification notification) {
        channelsToBeNotified.add(notification.getChannel());
    }

    @Scheduled(fixedDelay = 500)
    private void emitter() {
        Instant now = Instant.now();
        Set<String> channels = new HashSet<>(channelsToBeNotified);
        channelsToBeNotified.removeAll(channels);
        notifyClusters(channels);
        emit(channels);
        long time = Duration.between(now, Instant.now()).toMillis();
        if(time > 1000) {
            log.info("Emit job took {} ms.", time);
        }

    }

    private void emit(Set<String> channels) {

        Set<String> allPossibleChannels = getAllPossibleChannels(channels);
        Set<Subscription> allEmitters = new HashSet<>();

        synchronized (subscriptions) {
            for (String channelPart : allPossibleChannels) {
                log.info("Querying channel: {}", channelPart);
                Set<Subscription> subscriptionsToChannel = subscriptions.get(channelPart);
                allEmitters.addAll(subscriptionsToChannel);
            }
        }

        for (Subscription subscription : allEmitters) {
            try {
                Object message = channels.stream().filter(subscription::belongsTo).collect(Collectors.toList());
                subscription.getEmitter().send(message, MediaType.APPLICATION_JSON);
            } catch (IOException e) {
                e.printStackTrace();
                subscription.close();
            }
        }
    }

    private Set<String> getAllPossibleChannels(Set<String> channels) {
        // All possible channel subscriptions to be notified.
        // e.g. {com.milsoft.turkuaz, com.milsoft.otc} will produce "{com, com.milsoft, com.milsoft.turkuaz, com.milsoft.otc}"
        return channels.stream().collect(HashSet::new, (set, channel) -> {
            List<Integer> positions = new ArrayList<>();
            for (int i = 0; i < channel.length(); i++) {
                if (channel.charAt(i) == '.') {
                    positions.add(i);
                }
            }
            positions.add(channel.length());
            for (Integer position : positions) {
                set.add(channel.substring(0, position));
            }
        }, Collection::addAll);
    }

    private void copy(Notification source, Notification destination) {
        destination.setContent(source.getContent());
        destination.setTag(source.getTag());
        destination.setTtl(source.getTtl());
        if (source.getTtl() > 0) {
            destination.setExpired(Date.from(Instant.now().plusSeconds(source.getTtl())));
        } else {
            destination.setExpired(null);
        }
    }

}
