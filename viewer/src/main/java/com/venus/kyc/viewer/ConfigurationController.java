package com.venus.kyc.viewer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/config")
@Tag(name = "System Configuration", description = "Endpoints for retrieving system configuration and environment details")
public class ConfigurationController {

    @Value("${risk.external-api.url}")
    private String riskApiUrl;

    @Value("${spring.application.name:kyc-viewer}")
    private String appName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Operation(summary = "Get system configuration", description = "Returns application configuration properties and system environment details")
    @GetMapping
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();

        // Application Properties
        config.put("Application Name", appName);
        config.put("Server Port", serverPort);
        config.put("Risk API URL", riskApiUrl);

        // System Properties
        config.put("Java Version", System.getProperty("java.version"));
        config.put("OS Name", System.getProperty("os.name"));
        config.put("OS Arch", System.getProperty("os.arch"));
        config.put("OS Version", System.getProperty("os.version"));
        config.put("User Timezone", System.getProperty("user.timezone"));

        return config;
    }
}
