package com.module.facerecognition.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/")
    public String authPage() {
        return "auth";
    }

    @GetMapping("/live")
    public String livePage() {
        return "live";
    }

    @GetMapping("/enroll")
    public String enrollPage() {
        return "enroll";
    }

    @GetMapping("/live-enroll")
    public String liveEnrollPage() {
        return "live-enroll";
    }
}