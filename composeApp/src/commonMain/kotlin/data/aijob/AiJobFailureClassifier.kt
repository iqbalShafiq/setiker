package data.aijob

import domain.model.aijob.AiJobFailureKind

object AiJobFailureClassifier {
    fun classify(throwable: Throwable): Pair<AiJobFailureKind, Boolean> {
        val root = throwable.cause ?: throwable
        val className = root::class.simpleName.orEmpty()
        val message = (root.message ?: throwable.message).orEmpty().lowercase()
        return when {
            className.contains("UnknownHost", ignoreCase = true) ||
                className.contains("ConnectException", ignoreCase = true) ||
                className.contains("SocketTimeout", ignoreCase = true) ||
                message.contains("timeout") ||
                message.contains("unable to resolve host") ||
                message.contains("network") ||
                message.contains("connection") -> AiJobFailureKind.NETWORK to true

            message.contains("unsupported") && message.contains("platform") ->
                AiJobFailureKind.UNSUPPORTED_PLATFORM to false

            message.contains("not found") && message.contains("file") ->
                AiJobFailureKind.MISSING_FILE to false

            message.contains("onnx") ||
                message.contains("model") ||
                message.contains("background") && message.contains("processor") ->
                AiJobFailureKind.LOCAL_PROCESSOR to false

            message.contains("401") ||
                message.contains("403") ||
                message.contains("400") ||
                message.contains("validation") ||
                message.contains("required") -> AiJobFailureKind.VALIDATION to false

            message.contains("429") ||
                message.contains("500") ||
                message.contains("502") ||
                message.contains("503") ||
                message.contains("504") ||
                message.contains("server") -> AiJobFailureKind.SERVER to true

            else -> AiJobFailureKind.UNKNOWN to true
        }
    }
}
