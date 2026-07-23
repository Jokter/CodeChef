package ai.deep.minicodex.model;

public record ModelResponse(String finalAnswer, ToolCall toolCall) {
    public static ModelResponse finalAnswer(String finalAnswer) {
        return new ModelResponse(finalAnswer, null);
    }

    public static ModelResponse toolCall(ToolCall toolCall) {
        return new ModelResponse(null, toolCall);
    }

    public boolean isFinalAnswer() {
        return finalAnswer != null;
    }
}
