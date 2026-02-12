package com.venus.kyc.viewer.controller;

import com.venus.kyc.viewer.service.ServiceControlService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:5173")
public class ServiceControlController {

    private final ServiceControlService serviceControlService;

    public ServiceControlController(ServiceControlService serviceControlService) {
        this.serviceControlService = serviceControlService;
    }

    @GetMapping
    public List<Map<String, Object>> getServices() {
        return serviceControlService.getAllServiceStatus();
    }

    @PostMapping("/{name}/{action}")
    public void performAction(@PathVariable String name, @PathVariable String action) throws Exception {
        switch (action.toLowerCase()) {
            case "start":
                serviceControlService.startService(name);
                break;
            case "stop":
                serviceControlService.stopService(name);
                break;
            case "restart":
                serviceControlService.restartService(name);
                break;
            default:
                throw new IllegalArgumentException("Invalid action: " + action);
        }
    }
}
