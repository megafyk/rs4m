package com.rs4m.rule;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RuleSource {
    private String sourceType; // e.g., "file", "string", "resource"
    private String content;    // file path, rule string, etc.
}
