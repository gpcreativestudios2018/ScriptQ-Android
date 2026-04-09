package com.gpcreativestudios.scriptq.ui

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gpcreativestudios.scriptq.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket

class RemoteControlActivity : AppCompatActivity() {

    private lateinit var nsdManager: NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var resolveListener: NsdManager.ResolveListener? = null

    private var socket: Socket? = null
    private var outWriter: PrintWriter? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    private lateinit var statusText: TextView
    private lateinit var controllerLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remote_control)

        statusText = findViewById(R.id.status_text)
        controllerLayout = findViewById(R.id.controller_layout)

        findViewById<Button>(R.id.btn_rc_play_pause).setOnClickListener { sendCommand("PLAY_PAUSE") }
        findViewById<Button>(R.id.btn_rc_slower).setOnClickListener { sendCommand("SLOWER") }
        findViewById<Button>(R.id.btn_rc_faster).setOnClickListener { sendCommand("FASTER") }
        findViewById<Button>(R.id.btn_rc_reset).setOnClickListener { sendCommand("RESET") }

        nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
        startDiscovery()
    }

    private fun startDiscovery() {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                runOnUiThread { statusText.text = "Searching for Prompter on WiFi..." }
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == "_scriptq._tcp" || service.serviceType == "_scriptq._tcp.") {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            connectToPrompter(serviceInfo.host.hostAddress, serviceInfo.port)
                        }
                    })
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        nsdManager.discoverServices("_scriptq._tcp", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    private fun connectToPrompter(ip: String?, port: Int) {
        if (ip == null) return
        scope.launch {
            try {
                socket = Socket(ip, port)
                outWriter = PrintWriter(socket!!.getOutputStream(), true)
                
                withContext(Dispatchers.Main) {
                    statusText.text = "Connected to Prompter"
                    controllerLayout.visibility = View.VISIBLE
                    // Stop discovering once connected for simplicity
                    nsdManager.stopServiceDiscovery(discoveryListener)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Connection Failed"
                }
            }
        }
    }

    private fun sendCommand(command: String) {
        scope.launch {
            outWriter?.println(command)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            discoveryListener?.let { nsdManager.stopServiceDiscovery(it) }
        } catch (e: Exception) {}
        try {
            outWriter?.close()
            socket?.close()
        } catch (e: Exception) {}
    }
}
