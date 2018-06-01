package com.iciftci.deneme.springboot.controller;

import com.iciftci.deneme.springboot.business.NotificationManager;
import com.iciftci.deneme.springboot.model.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class NotificationController {

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


    //TODO Channel listesi icin tek query calistirilabilir ya da
    //ayri query'lerin sonuclari birlestirilebilir (ya da kesişmeyen query'lerin sonuclari birlestirilir)
    @GetMapping("/notifications")
    public Map<String, List<Notification>> getNotifications(@RequestParam("channel") String[] channels, @RequestParam(required = false) boolean recursive) {
        List<Notification> notifications = notificationManager.getNotifications(channels, recursive);
        Map<String, List<Notification>> groupedByChannel = notifications.stream().collect(Collectors.groupingBy(Notification::getChannel));
        return groupedByChannel;
    }


    @GetMapping("/subscribe")
    public SseEmitter subscribe(@RequestParam(name = "ApiKey") final String apiKey, final @RequestParam(name = "channel") String[] channels, HttpServletResponse response) {
        System.out.println(apiKey + " connected.");
        response.setHeader("Cache-Control", "no-store");
        return notificationManager.subscribe(apiKey, channels);
    }

}