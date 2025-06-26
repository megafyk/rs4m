package com.rs4m.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DummyItem {
    private Long id;
    private String name;
    private String description;
    private boolean active;
}
