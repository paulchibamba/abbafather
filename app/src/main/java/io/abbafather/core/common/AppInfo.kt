package io.abbafather.core.common

/**
 * What the build calls itself. Injected rather than read from a `Context`, so the About screen's
 * ViewModel can say which version this is without holding one.
 */
data class AppInfo(val versionName: String)
