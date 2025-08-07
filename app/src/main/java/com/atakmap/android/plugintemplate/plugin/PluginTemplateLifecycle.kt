package com.atakmap.android.plugintemplate.plugin

import com.atak.plugins.impl.AbstractPlugin
import com.atak.plugins.impl.PluginContextProvider
import com.atakmap.android.plugintemplate.PluginTemplateMapComponent
import gov.tak.api.plugin.IServiceController

/**
 *
 * AbstractPluginLifeCycle shipped with
 * the plugin.
 */
class PluginTemplateLifecycle(serviceController: IServiceController) : AbstractPlugin(
    serviceController,
    PluginTemplateTool(
        serviceController.getService<PluginContextProvider?>(PluginContextProvider::class.java)
            .pluginContext
    ),
    PluginTemplateMapComponent()
) {
    init {
        PluginNativeLoader.init(
            serviceController.getService<PluginContextProvider?>(
                PluginContextProvider::class.java
            ).pluginContext
        )
    }

    companion object {
        private const val TAG = "PluginTemplateLifecycle"
    }
}