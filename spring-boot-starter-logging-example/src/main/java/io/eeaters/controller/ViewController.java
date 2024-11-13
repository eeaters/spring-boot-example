package io.eeaters.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author eeaters
 * @since 0.0.1-SNAPSHOT
 */
@RequestMapping("view")
public class ViewController {

    @GetMapping("")
    public String index() {
        return "index";
    }
}
