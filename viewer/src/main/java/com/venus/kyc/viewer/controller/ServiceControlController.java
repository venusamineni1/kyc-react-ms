package com.venus.kyc.viewer.controller;

import com.venus.kyc.viewer.service.ServiceControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Service Control", description = "Endpoints for monitoring and controlling backend service instances")
public class ServiceControlController {

    private final ServiceControlService serviceControlService;

    public ServiceControlController(ServiceControlService serviceControlService) {
        this.serviceControlService = serviceControlService;
    }

    @Operation(summary = "Get all service statuses", description = "Returns the running status of all managed backend services")
    @GetMapping
    public List<Map<String, Object>> getServices() {
        return serviceControlService.getAllServiceStatus();
    }

    @Operation(summary = "Perform service action", description = "Starts, stops, or restarts a specific backend service")
    @PostMapping("/{name}/{action}")
    public void performAction(@Parameter(description = "Service name") @PathVariable String name,
            @Parameter(description = "Action: start, stop, or restart") @PathVariable String action) throws Exception {
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
