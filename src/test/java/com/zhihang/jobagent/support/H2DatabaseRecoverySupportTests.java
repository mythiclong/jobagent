package com.zhihang.jobagent.support;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class H2DatabaseRecoverySupportTests {

    @Test
    void detectsCorruptedH2FailureByNestedCauseMessage() {
        RuntimeException failure = new RuntimeException("startup failed",
                new IllegalStateException("File corrupted while reading record: data/jobagent.mv.db [90030-232]"));

        assertTrue(H2DatabaseRecoverySupport.isCorruptedH2Failure(failure));
    }

    @Test
    void ignoresNonCorruptedFailures() {
        RuntimeException failure = new RuntimeException("port 8081 already in use");

        assertFalse(H2DatabaseRecoverySupport.isCorruptedH2Failure(failure));
    }

    @Test
    void parsesRelativeH2FileDatasourceUrl() {
        String url = "jdbc:h2:file:./data/jobagent;AUTO_SERVER=TRUE";

        H2DatabaseRecoverySupport.H2FileDatabaseSpec spec = H2DatabaseRecoverySupport.parseFileDatabaseSpec(url)
                .orElseThrow();

        assertEquals("jobagent", spec.databaseName());
        assertTrue(spec.directory().endsWith(Path.of("data")));
        assertTrue(spec.mvStoreFile().endsWith(Path.of("data", "jobagent.mv.db")));
        assertEquals("jdbc:h2:file:" + spec.basePath().toString().replace('\\', '/'), spec.restoreUrl());
    }

    @Test
    void rejectsNonFileBasedDatasourceUrl() {
        assertTrue(H2DatabaseRecoverySupport.parseFileDatabaseSpec("jdbc:h2:mem:testdb").isEmpty());
        assertTrue(H2DatabaseRecoverySupport.parseFileDatabaseSpec("jdbc:mysql://localhost/test").isEmpty());
    }
}
