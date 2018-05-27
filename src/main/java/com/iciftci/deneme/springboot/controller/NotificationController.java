package com.iciftci.deneme.springboot.controller;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.iciftci.deneme.springboot.model.Notification;
import com.iciftci.deneme.springboot.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@RestController
public class NotificationController {

    // TODO
    // Concurrency
    // Connection Failure
    // Scalablity
    // Wildcards


    @Autowired
    private NotificationRepository repository;
    private SetMultimap<String, SseEmitter> connectedUsers;
    private SetMultimap<String, String> subscriptions;
    private Map<String, AtomicLong> counterMap;

    public NotificationController() {
        connectedUsers = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());
        subscriptions = Multimaps.synchronizedSetMultimap(MultimapBuilder.hashKeys().hashSetValues().build());
        counterMap = new ConcurrentHashMap<>();
    }


    @PostMapping("/publish")
    public ResponseEntity<String> publish(@RequestBody Notification notification, @RequestParam(required = false) boolean replace) {
        Notification old = repository.findNotificationByKey(notification.getKey(), new Date());
        // Find according to key
        if (old != null) {
            if (replace) {
                // TODO There may be a race condition while adding
                old.setDeleted(true);
                repository.save(old);
            } else {
                // Reject this one
                return new ResponseEntity<String>("id: " + old.getId(), HttpStatus.CONFLICT);
            }
        }

        validate(notification);
        Notification saved = repository.save(notification);
        emit(saved);

        return ResponseEntity.ok("id: " + saved.getId());
    }

    @GetMapping("/notification/{channel}")
    public List<Notification> getNotifications(@PathVariable("channel") String channel, @RequestParam(defaultValue = "0") int sequence, @RequestParam(required = false) Date after) {
        List<Notification> notifications = repository.findNotificationByChannel(channel, new Date());
        return notifications.stream().filter(n -> n.getSequence() > sequence && (after == null || n.getCreated().after(after))).collect(Collectors.toList());
    }


    @PostMapping("/subscribe")
    public void subscribe(@RequestParam String apiKey, @RequestParam String channel) {
        subscriptions.put(channel, apiKey);
    }

    @PostMapping("/unsubscribe")
    public void unsubscribe(@RequestParam String apiKey, @RequestParam String channel) {
        subscriptions.remove(channel, apiKey);
    }


    @GetMapping("/connect")
    public SseEmitter connect(@RequestParam(name = "apiKey") final String apiKey, HttpServletResponse response) {
        System.out.println(apiKey + " connected.");
        response.setHeader("Cache-Control", "no-store");
        final SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(1));
        connectedUsers.put(apiKey, emitter);

        Runnable removeSseEmitter = () -> {
            System.out.println("On termination: " + apiKey);
            connectedUsers.remove(apiKey, emitter);
        };
        emitter.onCompletion(removeSseEmitter);
        emitter.onTimeout(removeSseEmitter);

        return emitter;
    }


    private void validate(@RequestBody Notification notification) {
        counterMap.putIfAbsent(notification.getChannel(), new AtomicLong());
        final long sequence = counterMap.get(notification.getChannel()).incrementAndGet();
        notification.setSequence(sequence);
        notification.setCreated(new Date());
        notification.setExpired(Date.from(notification.getCreated().toInstant().plusSeconds(notification.getTtl())));
    }

    private void emit(Notification notification) {
        // Synchronization
        String channel = notification.getChannel();
        Object message = "You've got mail.";
        // message = notification;

        // There should be no connectedUser+subscriptions synchronization. Deadlock may occur.
        synchronized (subscriptions) {
            Set<String> apiKeys = subscriptions.get(channel);
            for (String apiKey : apiKeys) {
                synchronized (connectedUsers) {
                    Set<SseEmitter> sseEmitters = connectedUsers.get(apiKey);
                    for (SseEmitter sseEmitter : sseEmitters) {
                        try {
                            sseEmitter.send(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

}