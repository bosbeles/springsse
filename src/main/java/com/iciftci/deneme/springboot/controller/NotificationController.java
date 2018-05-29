package com.iciftci.deneme.springboot.controller;

import com.iciftci.deneme.springboot.business.NotificationManager;
import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.regex.Pattern;

@RestController
public class NotificationController {

    // TODO
    // Concurrency
    // Connection Failure
    // Scalablity -
    // Wildcards +

    private static final Pattern CHANNEL_PATTERN = Pattern.compile("^\\w+(\\.\\w+)+$");

    @Autowired
    private NotificationManager notificationManager;


    @PostMapping("/notifications")
    public ResponseEntity<Notification> create(@RequestBody Notification notification) {
        Notification saved = notificationManager.create(notification);
        if (saved != null) {
            return ResponseEntity.ok(saved);

        } else {
            return ResponseEntity.badRequest().build();
        }
    }


    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Notification> remove(@PathVariable String id) {
        Notification removed = notificationManager.remove(id);
        if (removed != null) {
            return ResponseEntity.ok(removed);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/notifications/{id}")
    public ResponseEntity<Notification> update(@PathVariable String id, @RequestBody Notification notification) {
        notification.setId(id);
        Notification updated = notificationManager.update(notification);

        if (updated != null) {
            return ResponseEntity.ok(updated);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    @GetMapping("/notifications")
    public List<Notification> getNotifications(@RequestParam("channel") String channel, @RequestParam(defaultValue = "0") long after) {
        return notificationManager.getNotifications(channel, after);
    }


    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam(name = "ApiKey") final String apiKey, final @RequestParam(name = "channel") String[] channels, HttpServletResponse response) {
        System.out.println(apiKey + " connected.");
        response.setHeader("Cache-Control", "no-store");
        return notificationManager.subscribe(apiKey, channels);
    }

}