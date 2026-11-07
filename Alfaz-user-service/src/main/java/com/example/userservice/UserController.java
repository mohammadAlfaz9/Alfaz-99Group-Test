package com.example.userservice;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    private final UserRepository repository;

    public UserController(UserRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        if (pageNum < 1 || pageSize < 1) {
            return error(400, "pageNum and pageSize must be at least 1");
        }
        return ResponseEntity.ok(Map.of("result", true, "users", repository.findAll(pageNum, pageSize)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable int id) {
        return repository.findById(id)
                .<ResponseEntity<Map<String, Object>>>map(u -> ResponseEntity.ok(Map.of("result", true, "user", u)))
                .orElseGet(() -> error(404, "user not found"));
    }

    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, Object>> create(@RequestParam(required = false) String name) {
        if (name == null || name.isBlank()) {
            return error(400, "name is required");
        }
        User user = repository.save(name.trim());
        return ResponseEntity.ok(Map.of("result", true, "user", user));
    }

    private ResponseEntity<Map<String, Object>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", false, "errors", List.of(message)));
    }
}
