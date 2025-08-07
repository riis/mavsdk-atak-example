package com.atakmap.android.plugintemplate.plugin

import android.content.Context
import com.atak.plugins.impl.AbstractPluginTool
import com.atakmap.android.plugintemplate.PluginTemplateDropDownReceiver
import gov.tak.api.util.Disposable

class PluginTemplateTool(context: Context) : AbstractPluginTool(
    context,
    context.getString(R.string.app_name),
    context.getString(R.string.app_name),
    context.getResources().getDrawable(R.drawable.ic_launcher),
    PluginTemplateDropDownReceiver.Companion.SHOW_PLUGIN
), Disposable {
    override fun dispose() {
    }
}
