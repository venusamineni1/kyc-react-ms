package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.*;
import com.venus.kyc.screening.batch.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "batch.work.dir=/tmp/screening-batch-test",
        "batch.privateKeyPath=/tmp/screening-batch-test/dummy_private.asc"
})
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
public class BatchScreeningTest {

    @org.junit.jupiter.api.BeforeEach
    public void setup() throws Exception {
        File dir = new File("/tmp/screening-batch-test");
        if (!dir.exists())
            dir.mkdirs();
        File key = new File(dir, "dummy_private.asc");
        if (!key.exists()) {
            key.createNewFile();
            Files.writeString(key.toPath(), "DUMMY KEY CONTENT");
        }
    }

    @Autowired
    private BatchScreeningService batchScreeningService;

    @MockBean
    private EncryptionService encryptionService;

    @MockBean
    private SftpService sftpService;

    // CompressionService is not mocked, we want to test it real
    @Autowired
    private CompressionService compressionService;

    @Test
    public void testInitiateBatch() throws Exception {
        // Mock encryption to just copy file or do nothing (simulate success)
        doAnswer(invocation -> {
            File inputFile = invocation.getArgument(0);
            File outputFile = invocation.getArgument(1);
            Files.copy(inputFile.toPath(), outputFile.toPath()); // Fake encryption by copy
            return null;
        }).when(encryptionService).encryptFile(any(File.class), any(File.class), any(InputStream.class));

        // Mock upload
        doNothing().when(sftpService).uploadFile(any(File.class), anyString());

        Client client1 = new Client(1L, null, "John", null, "Doe", null, null, null, null, null, null, null, "Male",
                null, null, null, null, null, null, null);
        Client client2 = new Client(2L, null, "Jane", null, "Smith", null, null, null, null, null, null, null, "Female",
                null, null, null, null, null, null, null);
        List<Client> clients = List.of(client1, client2);
        Long batchId = batchScreeningService.initiateBatch(clients);

        assertNotNull(batchId);
        // assertTrue(batchName.startsWith("2475_RC_DELTA_")); // No longer checked here
        // -> maybe check via repository verify? OR assume if ID returned it worked.
        // Actually, we can verify that saveBatchRun was called with a constructed name.
        verify(batchRepository).saveBatchRun(argThat(run -> run.batchName().startsWith("2475_RC_DELTA_")));

        // Verify calls
        verify(encryptionService, times(1)).encryptFile(any(File.class), any(File.class), any(InputStream.class));
        verify(sftpService, times(1)).uploadFile(any(File.class), anyString());
    }

    @MockBean
    private BatchRepository batchRepository;

    @Test
    public void testProcessResponse_Notification() throws Exception {
        // Setup
        String batchName = "BATCH_001";
        String remoteFileName = batchName + ".zip.gpg";
        BatchRun run = new BatchRun(1L, batchName, "INITIATED", null, null, null, null);
        when(batchRepository.findByBatchName(batchName)).thenReturn(run);

        // Prepare XML
        String xmlContent = "<Notification xmlns=\"http://www.db.com/NLSNotification\"><Meta><Stat>ACTC</Stat></Meta><RecordNoti><Rec><UniRcrdId>123</UniRcrdId><Err><ErrCode>E01</ErrCode><ErrDesc>Error</ErrDesc></Err></Rec></RecordNoti></Notification>";
        File xmlFile = File.createTempFile("test", ".xml");
        Files.writeString(xmlFile.toPath(), xmlContent);

        // Prepare Zip
        File zipFile = File.createTempFile("test", ".zip");
        compressionService.zipFiles(List.of(xmlFile), zipFile);

        // Mock download (create dummy local encrypted file)
        doAnswer(inv -> {
            File f = inv.getArgument(1);
            f.createNewFile();
            return null;
        }).when(sftpService).downloadFile(anyString(), any(File.class));

        // Mock decrypt (copy prepared zip to target)
        doAnswer(inv -> {
            File target = inv.getArgument(1);
            Files.copy(zipFile.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(encryptionService).decryptFile(any(File.class), any(File.class), any(InputStream.class), anyString());

        // Execute
        batchScreeningService.processResponse(remoteFileName);

        // Verify
        verify(batchRepository).updateBatchStatus(eq(1L), eq("NOTIFICATION_RECEIVED"), eq("ACTC"), any());
        verify(batchRepository).saveError(any(BatchRunError.class));
    }

    @Test
    public void testProcessResponse_Feedback() throws Exception {
        // Setup
        String batchName = "BATCH_002";
        String remoteFileName = batchName + ".zip.gpg";
        BatchRun run = new BatchRun(2L, batchName, "NOTIFICATION_RECEIVED", "ACTC", null, null, null);
        when(batchRepository.findByBatchName(batchName)).thenReturn(run);

        // Prepare XML
        String xmlContent = "<Feedback xmlns=\"http://www.db.com/NLSFeedback\"><Meta><Nor>5</Nor></Meta><FbRecs><FbRec><UniRcrdId>456</UniRcrdId><Mat><MatchId>M1</MatchId><MatchName>Name</MatchName><Score>100</Score><Stat>OK</Stat></Mat></FbRec></FbRecs></Feedback>";
        File xmlFile = File.createTempFile("test", ".xml");
        Files.writeString(xmlFile.toPath(), xmlContent);

        // Prepare Zip
        File zipFile = File.createTempFile("test", ".zip");
        compressionService.zipFiles(List.of(xmlFile), zipFile);

        // Mock download
        doAnswer(inv -> {
            File f = inv.getArgument(1);
            f.createNewFile();
            return null;
        }).when(sftpService).downloadFile(anyString(), any(File.class));

        // Mock decrypt
        doAnswer(inv -> {
            File target = inv.getArgument(1);
            Files.copy(zipFile.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return null;
        }).when(encryptionService).decryptFile(any(File.class), any(File.class), any(InputStream.class), anyString());

        // Execute
        batchScreeningService.processResponse(remoteFileName);

        // Verify
        verify(batchRepository).updateBatchStatus(eq(2L), eq("PROCESSED"), any(), eq(5));
        verify(batchRepository).saveFeedbackResult(any(BatchFeedbackResult.class));
    }

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    public void testInitiateBatch_Json() throws Exception {
        // Mock upload
        doNothing().when(sftpService).uploadFile(any(File.class), anyString());

        Client client1 = new Client(1L, null, "John", null, "Doe", null, null, null, null, null, null, null, "Male",
                null, null, null, null, null, null, null);
        List<Client> clients = List.of(client1);

        org.springframework.test.web.servlet.ResultActions result = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/screening/batch/initiate")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(clients)));

        result.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(org.hamcrest.Matchers.matchesPattern("\\d+")));

        // Verify service was called logic is implicitly covered by real service
        // execution and repository save
        verify(batchRepository).saveBatchRun(any(BatchRun.class));
    }
}
