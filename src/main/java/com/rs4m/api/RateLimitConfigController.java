package com.rs4m.api;

import com.rs4m.config.RateLimitProfileProperties.BucketProfile;
import com.rs4m.observer.EventManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/rs4m/rate-limit")
public class RateLimitConfigController {
    private final EventManager eventManager;

    @Autowired
    public RateLimitConfigController(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @PostMapping("/buckets")
    public String updateBucketProfiles(@RequestBody Map<String, BucketProfile> newProfiles) {
        eventManager.notify(newProfiles);
        return "Bucket profiles updated successfully";
    }
}

