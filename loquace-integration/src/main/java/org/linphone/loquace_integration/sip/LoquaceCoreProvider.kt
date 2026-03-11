package org.linphone.loquace_integration.sip

import org.linphone.core.Core

object LoquaceCoreProvider {
    private var coreProvider: (() -> Core)? = null

    /**
     * Call this from the main app's Application.onCreate()
     * e.g. LoquaceCoreProvider.init { coreContext.core }
     */
    fun init(provider: () -> Core) {
        coreProvider = provider
    }

    fun getCore(): Core = coreProvider?.invoke()
        ?: error("LoquaceCoreProvider not initialized. Call init() in Application.onCreate().")
}