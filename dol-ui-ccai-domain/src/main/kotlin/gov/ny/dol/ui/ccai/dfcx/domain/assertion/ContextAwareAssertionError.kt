package gov.ny.dol.ui.ccai.dfcx.domain.assertion

class ContextAwareAssertionError(message: String?, val sourceId: String, val sourceLocator: Any?) : Exception(message) {
}
