package com.venus.kyc.viewer.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServiceControlService {

    private static final Map<String, ServiceInfo> SERVICES = new HashMap<>();

    static {
        SERVICES.put("registry", new ServiceInfo("service-registry", 8761, "registry.log", "Service Registry"));
        SERVICES.put("auth", new ServiceInfo("auth-service", 8084, "auth.log", "Auth Service"));
        SERVICES.put("risk", new ServiceInfo("risk-service", 8081, "risk.log", "Risk Service"));
        SERVICES.put("screening", new ServiceInfo("screening-service", 8082, "screening.log", "Screening Service"));
        SERVICES.put("document", new ServiceInfo("document-service", 8085, "document.log", "Document Service"));
        SERVICES.put("viewer", new ServiceInfo("viewer", 8083, "viewer.log", "Viewer Service"));
        SERVICES.put("gateway", new ServiceInfo("api-gateway", 8080, "gateway.log", "API Gateway"));
    }

    public List<Map<String, Object>> getAllServiceStatus() {
        List<Map<String, Object>> statusList = new ArrayList<>();
        for (Map.Entry<String, ServiceInfo> entry : SERVICES.entrySet()) {
            Map<String, Object> status = new HashMap<>();
            status.put("key", entry.getKey());
            status.put("name", entry.getValue().name);
            status.put("port", entry.getValue().port);
            status.put("status", isPortOpen(entry.getValue().port) ? "UP" : "DOWN");
            statusList.add(status);
        }
        return statusList;
    }

    public void restartService(String serviceKey) throws IOException {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
        String script = isWindows ? "restart-service.bat" : "./restart-service.sh";
        
        File workingDir = getRootDir();

        ProcessBuilder pb = new ProcessBuilder(script, serviceKey);
        pb.directory(workingDir);
        // We start it and let it run. For viewer service itself, this might kill the current process.
        pb.start();
    }
    
    public void stopService(String serviceKey) throws IOException, InterruptedException {
         ServiceInfo info = SERVICES.get(serviceKey);
         if (info == null) throw new IllegalArgumentException("Unknown service");
         
         if (System.getProperty("os.name").toLowerCase().contains("win")) {
             // Windows Stop using taskkill with Java-based PID finding
             Integer pid = findPidOnPort(info.port);
             if (pid != null) {
                 ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/PID", pid.toString());
                 pb.start().waitFor();
             }
         } else {
             // Unix Stop
             String command = String.format("lsof -t -i:%d -sTCP:LISTEN | xargs kill", info.port);
             ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
             pb.start().waitFor();
         }
    }

    private Integer findPidOnPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder("netstat", "-aon");
            Process process = pb.start();
            
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(":" + port) && line.contains("LISTENING")) {
                        // Line format: TCP    0.0.0.0:8081           0.0.0.0:0              LISTENING       1234
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length > 0) {
                            try {
                                return Integer.parseInt(parts[parts.length - 1]);
                            } catch (NumberFormatException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void startService(String serviceKey) throws IOException {
         ServiceInfo info = SERVICES.get(serviceKey);
         if (info == null) throw new IllegalArgumentException("Unknown service");

         File rootDir = getRootDir();
         File serviceDir = new File(rootDir, info.dir);
         
         boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
         String mvnCommand = isWindows ? "..\\mvnw.cmd" : "../mvnw";
         String logFile = "../" + info.log;
         
         ProcessBuilder pb;
         if (isWindows) {
             // start "NAME" /MIN cmd /c "mvnw..."
             String cmd = String.format("start \"%s\" /MIN cmd /c \"%s spring-boot:run > %s 2>&1\"", 
                 info.name, mvnCommand, logFile);
             pb = new ProcessBuilder("cmd", "/c", cmd);
         } else {
             // nohup ../mvnw spring-boot:run > ../log 2>&1 &
             String cmd = String.format("nohup %s spring-boot:run > %s 2>&1 &", mvnCommand, logFile);
             pb = new ProcessBuilder("bash", "-c", cmd);
         }
         
         pb.directory(serviceDir);
         pb.start();
    }

    private File getRootDir() {
        File workingDir = new File(System.getProperty("user.dir"));
        // If running inside viewer directory, move up to root where scripts are
        if (workingDir.getName().equals("viewer")) {
            workingDir = workingDir.getParentFile();
        }
        return workingDir;
    }

    private boolean isPortOpen(int port) {
        try (Socket socket = new Socket("localhost", port)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    static class ServiceInfo {
        String dir;
        int port;
        String log;
        String name;

        public ServiceInfo(String dir, int port, String log, String name) {
            this.dir = dir;
            this.port = port;
            this.log = log;
            this.name = name;
        }
    }
}
