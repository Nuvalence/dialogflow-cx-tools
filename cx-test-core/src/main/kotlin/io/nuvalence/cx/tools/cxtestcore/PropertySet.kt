package io.nuvalence.cx.tools.cxtestcore

interface PropertySet {
    val propertiesMap: MutableMap<String, Any?>
    fun initialize(path: String)
}

inline fun <reified T> PropertySet.getProperty(key: String): T {
    val prop = propertiesMap[key]!!
    return if (prop is T) prop else throw Error("Property $key was not initialized as expected type ${T::class.simpleName}")
}
