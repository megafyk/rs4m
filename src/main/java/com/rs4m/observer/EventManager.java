package com.rs4m.observer;

import com.rs4m.config.RateLimitProfileProperties.BucketProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class EventManager implements Publisher<Map<String, BucketProfile>> {
    private final List<Subscriber<Map<String, BucketProfile>>> subscribers = new ArrayList<>();

    public EventManager(DefaultRateLimitManager defaultRateLimitManager) {
        // Register this EventManager as a subscriber to DefaultRateLimitManager
        this.register(defaultRateLimitManager);
        log.info("EventManager registered with DefaultRateLimitManager");
    }

    @Override
    public void register(Subscriber<Map<String, BucketProfile>> subscriber) {
        subscribers.add(subscriber);
        log.info("Registered subscriber: {}", subscriber.getClass().getSimpleName());
    }

    @Override
    public void unregister(Subscriber<Map<String, BucketProfile>> subscriber) {
        subscribers.remove(subscriber);
        log.info("Unregistered subscriber: {}", subscriber.getClass().getSimpleName());
    }

    @Override
    public void notify(Map<String, BucketProfile> event) {
        for (Subscriber<Map<String, BucketProfile>> subscriber : subscribers) {
            try {
                subscriber.update(event);
                log.info("Notified subscriber: {}", subscriber.getClass().getSimpleName());
            } catch (Exception e) {
                log.error("Error notifying subscriber {}: {}", subscriber.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }
}

