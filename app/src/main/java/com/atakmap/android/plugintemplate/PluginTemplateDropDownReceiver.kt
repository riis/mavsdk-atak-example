package com.atakmap.android.plugintemplate

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.TextView
import com.atak.plugins.impl.PluginLayoutInflater
import com.atakmap.android.dropdown.DropDown.OnStateListener
import com.atakmap.android.dropdown.DropDownReceiver
import com.atakmap.android.maps.MapView
import com.atakmap.android.plugintemplate.plugin.R
import com.atakmap.coremap.log.Log
import io.mavsdk.System
import io.mavsdk.MavsdkEventQueue
import io.mavsdk.mavsdkserver.MavsdkServer
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginTemplateDropDownReceiver(
    mapView: MapView,
    private val pluginContext: Context
) : DropDownReceiver(mapView), OnStateListener {

    private val TAG = "PluginTemplateDropDownReceiver"
    private val templateView: View =
        PluginLayoutInflater.inflate(pluginContext, R.layout.main_layout, null)

    private val positionTextView: TextView? = templateView.findViewById(R.id.positionTextView)

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val mavSdkServer = MavsdkServer()
    private var drone: System? = null
    private var isMavSdkServerRunning = false
    private val disposables: MutableList<Disposable> = mutableListOf()

    private val _position = MutableStateFlow(Pair(0.0, 0.0))
    private val position = _position.asStateFlow()

    init {
        startMavSdk()

        coroutineScope.launch {
            position.collect { pair ->
                val positionString = "Position: (${pair.first}, ${pair.second})"
                Log.d(TAG, "position collected: $positionString")
                withContext(Dispatchers.Main) {
                    positionTextView?.text = positionString
                }
            }
        }
    }

    private fun startMavSdk() {
        if (isMavSdkServerRunning) return

        MavsdkEventQueue.executor().execute {
            val mavSdkServerPort = mavSdkServer.run("udp://:14550")
            drone = System(BACKEND_IP_ADDRESS, mavSdkServerPort)

            disposables.add(
                drone!!.telemetry.flightMode.distinctUntilChanged()
                    .subscribe { flightMode ->
                        Log.d(TAG, "Flight mode: $flightMode")
                    }
            )

            disposables.add(
                drone!!.telemetry.getArmed().distinctUntilChanged()
                    .subscribe { armed ->
                        Log.d(TAG, "Armed: $armed")
                    }
            )

            disposables.add(
                drone!!.telemetry.getPosition()
                    .subscribe { position ->
                        _position.update { Pair(position.latitudeDeg, position.longitudeDeg) }
                    }
            )

            isMavSdkServerRunning = true
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == SHOW_PLUGIN) {
            showDropDown(
                templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                HALF_HEIGHT, false, this
            )
        }
    }

    override fun onDropDownClose() {
        // Optional: Add disconnect logic here if you want to tear down MAVSDK
    }

    override fun disposeImpl() {
        coroutineScope.cancel()
        if (isMavSdkServerRunning) {
            destroyMavsdkServer()
        }
    }

    private fun destroyMavsdkServer() {
        MavsdkEventQueue.executor().execute {
            disposables.forEach { it.dispose() }
            disposables.clear()
            drone?.dispose()
            drone = null
            mavSdkServer.stop()
            mavSdkServer.destroy()
            isMavSdkServerRunning = false
        }
    }

    override fun onDropDownVisible(v: Boolean) {}
    override fun onDropDownSelectionRemoved() {}
    override fun onDropDownSizeChanged(width: Double, height: Double) {}

    companion object {
        const val SHOW_PLUGIN = "com.atakmap.android.plugintemplate.SHOW_PLUGIN"
        private const val BACKEND_IP_ADDRESS = "127.0.0.1"
    }
}

