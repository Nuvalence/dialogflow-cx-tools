package io.nuvalence.cx.tools.cxtestcore

import kotlin.reflect.KClass

class Properties {
    companion object {
        lateinit var activePropertySet: PropertySet

        fun init(path: String) {
            val rawProperties = java.util.Properties()
            val loader = Thread.currentThread().contextClassLoader
            val stream = loader.getResourceAsStream(path)
            rawProperties.load(stream)

            val className = rawProperties.getProperty("propertySetClassName")

            val propertySetClass : KClass<*> = Class.forName(className).kotlin

            if (PropertySet::class.java.isAssignableFrom(propertySetClass.java)) {
                activePropertySet = propertySetClass.objectInstance as PropertySet
            }

            activePropertySet.initialize(path)
        }

        inline fun <reified T> getProperty(key: String): T {
            return activePropertySet.getProperty(key)
        }
    }
}
