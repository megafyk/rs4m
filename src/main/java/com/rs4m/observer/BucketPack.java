package com.rs4m.observer;

import io.github.bucket4j.BucketConfiguration;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BucketPack {
    private String name;
    private boolean enabled;
    private String version;
    private BucketConfiguration bucketConfiguration;
}
