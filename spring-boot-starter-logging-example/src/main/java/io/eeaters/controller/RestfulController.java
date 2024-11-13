package io.eeaters.controller;


import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * restful风格
 *
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@RestController
@RequestMapping("rest")
public class RestfulController {

    @GetMapping("get")
    public ResponseEntity<String> get() {
        return ResponseEntity.ok("pong");
    }

    @PostMapping("post")
    public ResponseEntity<Object> post(@RequestBody User user) {
        return ResponseEntity.ok(user);
    }


    @Data
    public static class User{
        private String name;
        private Integer age;
    }
}
