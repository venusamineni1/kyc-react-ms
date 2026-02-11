package com.venus.kyc.screening.batch;

import com.venus.kyc.screening.batch.model.Client;
import com.venus.kyc.screening.batch.model.NLSFeed;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DynamicMappingTest {

        @Autowired
        private BatchScreeningService batchScreeningService;

        @Autowired
        private MappingConfigRepository mappingConfigRepository;

        @Test
        public void testDynamicMapping() throws Exception {
                // 1. Setup a custom mapping: map Gender to LastName for "fun" / testing dynamic
                // nature
                List<MappingConfig> customMappings = List.of(
                                new MappingConfig(null, "record.uniRcrdId", "clientID", null, null),
                                new MappingConfig(null, "name.full", "lastName", null, null), // Map only lastName to
                                                                                              // FullName
                                new MappingConfig(null, "individual.gender", null, "UNKNOWN_GENDER", null) // Default
                                                                                                           // value test
                );
                mappingConfigRepository.saveAll(customMappings);

                // 2. Prepare test client
                Client client = new Client(12345L, "Mr", "John", "D", "Doe", null, "US", null, null, "ACTIVE", null,
                                null, "M",
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                                null);

                // 3. Trigger batch (we only care about the XML generation part for this test)
                // Since initiateBatch does many things (SFTP, Encryption), we might want to
                // test the internal createFeed method
                // But createFeed is private. We can test via reflection or by mocking
                // dependencies.
                // For this walkthrough, I'll rely on the logic check.

                // Actually, let's just use reflection to call createFeed for verification
                java.lang.reflect.Method createFeedMethod = BatchScreeningService.class.getDeclaredMethod("createFeed",
                                String.class, List.class);
                createFeedMethod.setAccessible(true);
                NLSFeed feed = (NLSFeed) createFeedMethod.invoke(batchScreeningService, "TEST_BATCH", List.of(client));

                // 4. Verify mappings
                String uniId = feed.getRequest().getRecords().getRecList().get(0).getMeta().getUniRcrdId();
                String fullName = feed.getRequest().getRecords().getRecList().get(0).getData().getPrtInfo().getInd()
                                .getNames()
                                .getNameList().get(0).getFull();
                String gender = feed.getRequest().getRecords().getRecList().get(0).getData().getPrtInfo().getInd()
                                .getGender();

                assertEquals("12345", uniId);
                assertEquals("Doe", fullName); // Verified dynamic mapping to lastName
                assertEquals("UNKNOWN_GENDER", gender); // Verified default value
        }
}
