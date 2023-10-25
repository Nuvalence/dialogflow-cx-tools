package gov.ny.dol.ui.ccai.dfcx.domain.listener

import gov.ny.dol.ui.ccai.dfcx.domain.assertion.ContextAwareAssertionError
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestWatcher
import java.util.*

class DynamicTestListener : TestWatcher {
    private val errorList : MutableList<ContextAwareAssertionError> = Collections.synchronizedList(mutableListOf<ContextAwareAssertionError>())

    override fun testFailed(context: ExtensionContext, cause: Throwable?) {
        if (cause is ContextAwareAssertionError) {
            val contextError = cause as ContextAwareAssertionError
            val errorList = context.root.getStore(ExtensionContext.Namespace.GLOBAL).getOrComputeIfAbsent("errors", { errorList }, List::class.java)
                as MutableList<ContextAwareAssertionError>
            errorList.add(contextError)
            context.root.getStore(ExtensionContext.Namespace.GLOBAL).put("errors", errorList)
        }
    }
}
