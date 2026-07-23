package ai.deep.minicodex.safety;

import java.nio.file.Path;

public class WorkspacePolicy {
    private final Path workspaceRoot;

    public WorkspacePolicy(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    public Path resolveInsideWorkspace(String inputPath) {
        Path resolved = workspaceRoot.resolve(inputPath).normalize();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("路径越过了工作区边界: " + inputPath);
        }
        return resolved;
    }

    public Path workspaceRoot() {
        return workspaceRoot;
    }
}
