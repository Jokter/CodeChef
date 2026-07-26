package ai.deep.minicodex.safety;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkspacePolicyTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesRelativePathInsideWorkspace() {
        WorkspacePolicy policy = new WorkspacePolicy(tempDir);

        Path resolved = policy.resolveInsideWorkspace("docs/../README.md");

        assertEquals(tempDir.toAbsolutePath().normalize().resolve("README.md"), resolved);
    }

    @Test
    void rejectsPathOutsideWorkspace() {
        WorkspacePolicy policy = new WorkspacePolicy(tempDir);

        assertThrows(IllegalArgumentException.class, () -> policy.resolveInsideWorkspace("../outside.txt"));
    }
}