package com.innova.launcher2kd.service

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class Obd2Manager(
    private val context: Context,
    private val onDataReceived: (voltage: Float, ect: Int, boostBar: Float, rpm: Int) -> Unit,
    private val onStatusChanged: (statusText: String, isConnected: Boolean) -> Unit
) {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null

    @Volatile
    private var isRunning = false
    @Volatile
    private var isConnected = false

    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return try {
            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        stop()
        thread(name = "OBD2_ConnectionThread") {
            try {
                postStatus("Menghubungkan ke ELM327 ($deviceAddress)...", false)
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                if (device == null) {
                    postStatus("Perangkat OBD2 tidak ditemukan", false)
                    return@thread
                }

                bluetoothAdapter?.cancelDiscovery()
                val sock = device.createRfcommSocketToServiceRecord(SPP_UUID)
                sock.connect()
                socket = sock
                inStream = sock.inputStream
                outStream = sock.outputStream

                // Initialize ELM327
                initElm327()

                isConnected = true
                isRunning = true
                postStatus("OBD2 TERHUBUNG (2KD ECU ONLINE)", true)

                // Start polling loop
                startDataLoop()

            } catch (e: Exception) {
                isConnected = false
                postStatus("OBD2 Terputus / Tidak Aktif", false)
                disconnectSocket()
            }
        }
    }

    private fun initElm327() {
        sendCommand("AT Z")
        Thread.sleep(800)
        sendCommand("AT E0")
        Thread.sleep(200)
        sendCommand("AT L0")
        Thread.sleep(200)
        sendCommand("AT SP 0")
        Thread.sleep(300)
    }

    private fun startDataLoop() {
        var voltage = 0.0f
        var ect = 0
        var boostBar = 0.0f
        var rpm = 0

        while (isRunning && isConnected) {
            try {
                // 1. Read Battery Voltage from OBD2 Pin 16
                val voltRes = sendCommand("AT RV")
                val parsedVolt = parseVoltage(voltRes)
                if (parsedVolt > 5.0f) voltage = parsedVolt

                // 2. Read Engine Coolant Temp (ECT)
                val ectRes = sendCommand("0105")
                val parsedEct = parseEct(ectRes)
                if (parsedEct != -999) ect = parsedEct

                // 3. Read Intake Manifold Absolute Pressure (MAP) -> Boost
                val mapRes = sendCommand("010B")
                val parsedBoost = parseBoost(mapRes)
                boostBar = parsedBoost

                // 4. Read Engine RPM
                val rpmRes = sendCommand("010C")
                val parsedRpm = parseRpm(rpmRes)
                if (parsedRpm >= 0) rpm = parsedRpm

                mainHandler.post {
                    onDataReceived(voltage, ect, boostBar, rpm)
                }

                Thread.sleep(250) // 4Hz update rate, ideal for automotive UI
            } catch (e: Exception) {
                break
            }
        }

        isConnected = false
        postStatus("OBD2 Terputus", false)
        disconnectSocket()
    }

    @Synchronized
    private fun sendCommand(cmd: String): String {
        val out = outStream ?: return ""
        val inS = inStream ?: return ""

        val rawCmd = "$cmd\r"
        out.write(rawCmd.toByteArray())
        out.flush()

        val sb = StringBuilder()
        var b: Int
        val start = System.currentTimeMillis()

        while (System.currentTimeMillis() - start < 1200) {
            if (inS.available() > 0) {
                b = inS.read()
                val c = b.toChar()
                if (c == '>') break // ELM327 prompt character
                sb.append(c)
            } else {
                Thread.sleep(15)
            }
        }
        return sb.toString().trim()
    }

    private fun parseVoltage(raw: String): Float {
        // e.g. "14.2V" or "13.8"
        val clean = raw.replace("V", "").replace("v", "").trim()
        return clean.toFloatOrNull() ?: 0.0f
    }

    private fun parseEct(raw: String): Int {
        // Response format: "41 05 XX"
        val parts = raw.split(" ").filter { it.isNotEmpty() }
        val idx = parts.indexOf("05")
        if (idx != -1 && idx + 1 < parts.size) {
            val hex = parts[idx + 1]
            val value = hex.toIntOrNull(16)
            if (value != null) {
                return value - 40
            }
        }
        return -999
    }

    private fun parseBoost(raw: String): Float {
        // Response format: "41 0B XX" -> MAP in kPa
        val parts = raw.split(" ").filter { it.isNotEmpty() }
        val idx = parts.indexOf("0B")
        if (idx != -1 && idx + 1 < parts.size) {
            val hex = parts[idx + 1]
            val mapKpa = hex.toIntOrNull(16)
            if (mapKpa != null) {
                // Atmospheric pressure ~ 101.3 kPa (1.0 bar)
                val boostKpa = (mapKpa - 101.3f).coerceAtLeast(0.0f)
                return boostKpa / 100.0f // Convert to Bar
            }
        }
        return 0.0f
    }

    private fun parseRpm(raw: String): Int {
        // Response format: "41 0C XX YY"
        val parts = raw.split(" ").filter { it.isNotEmpty() }
        val idx = parts.indexOf("0C")
        if (idx != -1 && idx + 2 < parts.size) {
            val a = parts[idx + 1].toIntOrNull(16) ?: 0
            val b = parts[idx + 2].toIntOrNull(16) ?: 0
            return ((a * 256) + b) / 4
        }
        return -1
    }

    fun stop() {
        isRunning = false
        isConnected = false
        disconnectSocket()
    }

    private fun disconnectSocket() {
        try { inStream?.close() } catch (e: Exception) {}
        try { outStream?.close() } catch (e: Exception) {}
        try { socket?.close() } catch (e: Exception) {}
        inStream = null
        outStream = null
        socket = null
    }

    private fun postStatus(text: String, connected: Boolean) {
        mainHandler.post {
            onStatusChanged(text, connected)
        }
    }
}
