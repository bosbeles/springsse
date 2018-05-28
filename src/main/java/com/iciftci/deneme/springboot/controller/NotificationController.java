package com.iciftci.deneme.springboot.controller;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.iciftci.deneme.springboot.model.Notification;
import com.iciftci.deneme.springboot.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class NotificationController {

    // TODO
    // Concurrency
    // Connection Failure
    // Scalablity -
    // Wildcards +

    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");

    @Autowired
    private NotificationRepository repository;
    private SetMultimap<String, SseEmitter> subscriptions;


    public NotificationController() {
        subscriptions = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());
    }


    @PostMapping("/notifications")
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        if (!CHANNEL_PATTERN.matcher(notification.getChannel()).matches()) {
            return ResponseEntity.badRequest().build();
        }

        Notification saved = null;
        if (!StringUtils.isEmpty(notification.getTag())) {
            saved = repository.findByTag(notification.getChannel(), notification.getTag());
        }

        // Find according to key
        if (saved == null) {
            notification.setCreated(new Date());
            notification.setExpired(Date.from(notification.getCreated().toInstant().plusSeconds(notification.getTtl())));
            saved = repository.save(notification);
        } else {
            copy(notification, saved);
        }

        saved = repository.save(saved);
        emit(saved);

        return ResponseEntity.ok(saved);
    }


    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Notification> remove(@PathVariable String id) {
        Optional<Notification> old = repository.findById(id);
        if (old.isPresent()) {
            Notification notification = old.get();
            notification.setState(Notification.DELETED);
            Notification saved = repository.save(notification);
            emit(saved);
            return ResponseEntity.ok(saved);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/notifications/{id}")
    public ResponseEntity<Notification> update(@PathVariable String id, @RequestBody Notification notification) {
        Optional<Notification> old = repository.findById(id);
        if (old.isPresent()) {
            Notification oldNotification = old.get();
            if (oldNotification.getState() != Notification.DELETED) {
                copy(notification, oldNotification);
                Notification saved = repository.save(oldNotification);
                emit(saved);
                return ResponseEntity.ok(saved);
            }
        }

        return ResponseEntity.notFound().build();
    }


    @GetMapping("/notifications")
    public List<Notification> getNotifications(@RequestParam("channel") String channel, @RequestParam(defaultValue = "0") long after) {
        List<Notification> result = Collections.emptyList();
        if (!CHANNEL_PATTERN.matcher(channel).matches()) {
            return result;
        }
        //TODO ttl sorgusundan elenen notificationlar modified olmamis olacak.

        if(after == 0) {
            result = repository.findByChannel(channel, new Date(), new Sort(Sort.Direction.DESC, "modified"));
        } else {
            result = repository.findByChannelAfter(channel, new Date(), new Date(after),  new Sort(Sort.Direction.DESC, "modified"));
        }

        return result;
    }


    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam(name = "ApiKey") final String apiKey, final @RequestParam(name = "channel") String[] channels, HttpServletResponse response) {
        System.out.println(apiKey + " connected.");
        response.setHeader("Cache-Control", "no-store");
        final SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(1));
        for (String channel : channels) {
            if (CHANNEL_PATTERN.matcher(channel).matches()) {
                subscriptions.put(channel, emitter);
            }
        }

        Runnable removeSseEmitter = () -> {
            System.out.println("On termination: " + apiKey);
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


    @Async
    private void emit(Notification notification) {
        // Synchronization
        String channel = notification.getChannel();
        Object message = channel;
        // message = notification;


        Set<SseEmitter> allEmitters = new HashSet<>();

        // wildcard impl: tur, tur.sgrs, tur.sgrs.alert all listen tur.sgrs.alert
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < channel.length(); i++) {
            if (channel.charAt(i) == '.') {
                positions.add(i);
            }
        }
        positions.add(channel.length());

        // There should be no connectedUser+subscriptions synchronization. Deadlock may occur.
        synchronized (subscriptions) {
            for (Integer position : positions) {
                String channelToQuery = channel.substring(0, position);
                System.out.println("Querying channel: " + channelToQuery);
                Set<SseEmitter> sseEmitters = subscriptions.get(channelToQuery);
                allEmitters.addAll(sseEmitters);
            }
        }

        for (SseEmitter sseEmitter : allEmitters) {
            try {
                sseEmitter.send(message);
            } catch (IOException e) {
                e.printStackTrace();
                subscriptions.remove(notification.getChannel(), sseEmitter);
            }
        }
    }


    private void copy(Notification source, Notification destination) {
        destination.setContent(source.getContent());
        destination.setTag(source.getTag());
        destination.setTtl(source.getTtl());
        destination.setExpired(Date.from(Instant.now().plusSeconds(source.getTtl())));
    }

}