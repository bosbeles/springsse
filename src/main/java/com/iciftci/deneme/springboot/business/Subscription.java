package com.iciftci.deneme.springboot.business;

import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.Set;

public class Subscription {

    @Getter
    private SseEmitter emitter;

    @Getter
    private Set<String> channels;

    private Runnable onCloseCallback;

    public Subscription(SseEmitter emitter, Set<String> channels) {
        this.emitter = emitter;
        this.channels = Collections.unmodifiableSet(channels);

    }

    public void onClose(Runnable callback) {
        this.onCloseCallback = callback;
        emitter.onCompletion(onCloseCallback);
        emitter.onTimeout(onCloseCallback);
    }

    public void close() {
        if(onCloseCallback != null) {
            onCloseCallback.run();
        }
        emitter.complete();
    }

    public boolean belongsTo(String channel) {
        for (String regex : channels) {
            if (channel.equals(regex) || channel.startsWith(regex + ".")) {
                return true;
            }
        }
        return false;
    }

}
