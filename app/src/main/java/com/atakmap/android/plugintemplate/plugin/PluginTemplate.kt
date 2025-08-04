package com.atakmap.android.plugintemplate.plugin

import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.TextView
import com.atak.plugins.impl.PluginContextProvider
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.coremap.log.Log
import gov.tak.api.commons.graphics.Bitmap
import gov.tak.api.plugin.IPlugin
import gov.tak.api.plugin.IServiceController
import gov.tak.api.ui.IHostUIService
import gov.tak.api.ui.Pane
import gov.tak.api.ui.PaneBuilder
import gov.tak.api.ui.ToolbarItem
import gov.tak.api.ui.ToolbarItemAdapter
import gov.tak.platform.marshal.MarshalManager
import io.mavsdk.MavsdkEventQueue
import io.mavsdk.System
import io.mavsdk.mavsdkserver.MavsdkServer
import io.mavsdk.telemetry.Telemetry
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginTemplate(serviceController: IServiceController) : IPlugin {
    var serviceController: IServiceController?
    var pluginContext: Context? = null
    var uiService: IHostUIService?
    var toolbarItem: ToolbarItem?
    var templatePane: Pane? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var positionTextView: TextView? = null

    private val mavSdkServer = MavsdkServer()
    private var drone: System? = null
    private var isMavSdkServerRunning = false
    private val disposables: MutableList<Disposable> = mutableListOf()

    private val _position = MutableStateFlow(Pair(0.0, 0.0))
    private val position = _position.asStateFlow()

    init {
        this.serviceController = serviceController
        val ctxProvider = serviceController
            .getService<PluginContextProvider?>(PluginContextProvider::class.java)
        if (ctxProvider != null) {
            pluginContext = ctxProvider.getPluginContext()
            pluginContext!!.setTheme(R.style.ATAKPluginTheme)
        }

        // obtain the UI service
        uiService = serviceController.getService<IHostUIService?>(IHostUIService::class.java)

        // initialize the toolbar button for the plugin

        // create the button
        toolbarItem = ToolbarItem.Builder(
            pluginContext!!.getString(R.string.app_name),
            MarshalManager.marshal<Bitmap?, Drawable?>(
                pluginContext!!.getResources().getDrawable(R.drawable.ic_launcher),
                Drawable::class.java,
                Bitmap::class.java
            )
        )
            .setListener(object : ToolbarItemAdapter() {
                override fun onClick(item: ToolbarItem?) {
                    showPane()
                }
            })
            .build()
    }

    override fun onStart() {
        // the plugin is starting, add the button to the toolbar
        if (uiService == null) return

        uiService!!.addToolbarItem(toolbarItem)
        if (!isMavSdkServerRunning) {
            runMavSdkServer()
        }
    }

    override fun onStop() {
        // the plugin is stopping, remove the button from the toolbar
        if (uiService == null) return

        uiService!!.removeToolbarItem(toolbarItem)
        if (isMavSdkServerRunning) {
            destroyMavsdkServer()
        }
        coroutineScope.cancel()
    }

    private fun showPane() {
        // instantiate the plugin view if necessary
        if (templatePane == null) {
            // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
            // In this case, using it is not necessary - but I am putting it here to remind
            // developers to look at this Inflator

            val view = PluginLayoutInflater.inflate(
                pluginContext,
                R.layout.main_layout,
                null
            )
            positionTextView = view.findViewById(R.id.positionTextView)

            templatePane = PaneBuilder(view) // relative location is set to default; pane will switch location dependent on
                // current orientation of device screen
                .setMetaValue(
                    Pane.RELATIVE_LOCATION,
                    Pane.Location.Default
                ) // pane will take up 50% of screen width in landscape mode
                .setMetaValue(
                    Pane.PREFERRED_WIDTH_RATIO,
                    0.5
                ) // pane will take up 50% of screen height in portrait mode
                .setMetaValue(Pane.PREFERRED_HEIGHT_RATIO, 0.5)
                .build()

            coroutineScope.launch {
                position.collect { pair ->
                    val positionString = "Position: (${pair.first}, ${pair.second})"
                    Log.d(TAG, "position collected: $positionString")
                    withContext(Dispatchers.Main) {
                        if (positionTextView == null) {
                            Log.e(TAG, "positionTextView is null!")
                        } else {
                            Log.d(TAG, "updating TextView with: $positionString")
                            positionTextView?.text = positionString
                        }
                    }
                }
            }
        }

        // if the plugin pane is not visible, show it!
        if (!uiService!!.isPaneVisible(templatePane)) {
            uiService!!.showPane(templatePane, null)
        }
    }

    private fun runMavSdkServer() {
        MavsdkEventQueue.executor().execute {
            val mavSdkServerPort: Int = mavSdkServer.run("udp://:14550")

            drone = System(
                BACKEND_IP_ADDRESS,
                mavSdkServerPort
            )

            disposables.add(
                drone!!.telemetry.flightMode.distinctUntilChanged()
                    .subscribe(Consumer<Telemetry.FlightMode> { flightMode ->
                        Log.d(TAG, "Flight mode: $flightMode")
                    })
            )
            disposables.add(
                drone!!.telemetry.getArmed().distinctUntilChanged()
                    .subscribe(Consumer<Boolean> { armed: Boolean ->
                        Log.d(TAG,"Armed: $armed")
                    })
            )
            disposables.add(
                drone!!.telemetry.getPosition()
                    .subscribe(Consumer<Telemetry.Position> { position: Telemetry.Position ->
                        val latLng: Pair<Double, Double> = Pair(position.latitudeDeg, position.longitudeDeg)
                        Log.d(TAG, "position: (${latLng.first}, ${latLng.second})")
                        _position.update { latLng }
                    })
            )
            isMavSdkServerRunning = true
        }
    }

    private fun destroyMavsdkServer() {
        MavsdkEventQueue.executor().execute {
            for (disposable in disposables) {
                disposable.dispose()
            }
            disposables.clear()
            drone?.dispose()
            drone = null
            mavSdkServer.stop()
            mavSdkServer.destroy()

            isMavSdkServerRunning = false
        }
    }

    companion object {
        private const val TAG = "PluginTemplate"
        // phone 192.168.2.15
        // pc
        private const val BACKEND_IP_ADDRESS: String = "127.0.0.1"
    }
}
