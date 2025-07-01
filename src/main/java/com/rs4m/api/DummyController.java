package com.rs4m.api;

import com.rs4m.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/rs4m/dummy")
@Slf4j
public class DummyController {

    private final Map<Long, DummyItem> itemsDb = new ConcurrentHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    // Get all items
    @GetMapping
    @RateLimiter(value="dummy_bucket_2")
    public List<DummyItem> getAllItems() {
        log.info("Fetching all dummy items");
        return new ArrayList<>(itemsDb.values());
    }

    // Get item by ID
    @GetMapping("/{id}")
    public ResponseEntity<DummyItem> getItemById(@PathVariable Long id) {
        log.info("Fetching dummy item with id: {}", id);
        DummyItem item = itemsDb.get(id);
        if (item == null) {
            log.warn("Dummy item with id: {} not found", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(item);
    }

    // Create new item
    @PostMapping
    public ResponseEntity<DummyItem> createItem(@RequestBody DummyItem item) {
        log.info("Creating a new dummy item");
        Long id = idCounter.getAndIncrement();
        item.setId(id);
        itemsDb.put(id, item);
        log.info("Created dummy item with id: {}", id);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    // Update existing item
    @PutMapping("/{id}")
    public ResponseEntity<DummyItem> updateItem(@PathVariable Long id, @RequestBody DummyItem item) {
        log.info("Updating dummy item with id: {}", id);
        if (!itemsDb.containsKey(id)) {
            log.warn("Cannot update. Dummy item with id: {} not found", id);
            return ResponseEntity.notFound().build();
        }
        item.setId(id);
        itemsDb.put(id, item);
        log.info("Updated dummy item with id: {}", id);
        return ResponseEntity.ok(item);
    }

    // Delete item
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        log.info("Deleting dummy item with id: {}", id);
        if (!itemsDb.containsKey(id)) {
            log.warn("Cannot delete. Dummy item with id: {} not found", id);
            return ResponseEntity.notFound().build();
        }
        itemsDb.remove(id);
        log.info("Deleted dummy item with id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
