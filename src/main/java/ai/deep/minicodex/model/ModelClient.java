package ai.deep.minicodex.model;

import java.util.List;

public interface ModelClient {
    ModelResponse next(String userTask, List<String> observations);
}
