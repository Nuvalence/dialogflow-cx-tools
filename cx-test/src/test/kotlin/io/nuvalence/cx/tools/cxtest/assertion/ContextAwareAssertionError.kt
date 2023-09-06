package io.nuvalence.cx.tools.cxtest.assertion

class ContextAwareAssertionError(message: String?, val sourceId: String, val sourceLocator: Any?) : Exception(message) {
}
