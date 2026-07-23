package ai.deep.minicodex.safety;

import java.nio.file.Path;

/**
 * 工作区路径安全策略。
 *
 * <p>所有文件工具在访问文件系统前都应通过该类解析路径。它会把输入路径解析到
 * 工作区根目录下，并阻止 {@code ..} 等路径逃逸访问工作区外部文件。</p>
 */
public class WorkspacePolicy {
    private final Path workspaceRoot;

    /**
     * 创建工作区安全策略。
     *
     * @param workspaceRoot 工作区根目录
     */
    public WorkspacePolicy(Path workspaceRoot) {
        this.workspaceRoot = workspaceRoot.toAbsolutePath().normalize();
    }

    /**
     * 将输入路径解析为工作区内的规范化路径。
     *
     * @param inputPath 用户或模型提供的相对路径
     * @return 位于工作区内的规范化路径
     * @throws IllegalArgumentException 当路径越过工作区边界时抛出
     */
    public Path resolveInsideWorkspace(String inputPath) {
        Path resolved = workspaceRoot.resolve(inputPath).normalize();
        if (!resolved.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("路径越过了工作区边界: " + inputPath);
        }
        return resolved;
    }

    /**
     * 获取工作区根目录。
     *
     * @return 规范化后的工作区根目录
     */
    public Path workspaceRoot() {
        return workspaceRoot;
    }
}
