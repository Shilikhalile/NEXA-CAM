package com.nexa.cam

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraEffect
import androidx.camera.core.UseCaseGroup
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.extensions.ExtensionMode
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.camera.effects.Frame
import androidx.camera.effects.OverlayEffect
import androidx.arch.core.util.Function
import androidx.core.util.Consumer
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private fun renderColorEffect(canvas: Canvas, filter: Int, width: Float, height: Float) {
    when (filter) {
        1 -> canvas.drawColor(Color.argb(42, 255, 145, 45))
        2 -> canvas.drawColor(Color.argb(38, 35, 125, 255))
        3 -> {
            canvas.drawColor(Color.argb(28, 12, 0, 0))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, width, height, Color.argb(55, 120, 20, 20), Color.TRANSPARENT, Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        4 -> canvas.drawColor(Color.argb(48, 190, 20, 35))
        5 -> canvas.drawColor(Color.argb(32, 220, 205, 170))
        6 -> {
            canvas.drawColor(Color.argb(58, 20, 20, 20))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, height, Color.TRANSPARENT, Color.argb(85, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        7 -> { // Vivid
            canvas.drawColor(Color.argb(24, 255, 30, 30))
            canvas.drawColor(Color.argb(18, 20, 80, 255))
        }
        8 -> { // Golden Hour
            canvas.drawColor(Color.argb(62, 255, 145, 35))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, height, Color.argb(65, 255, 205, 90), Color.TRANSPARENT, Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        9 -> { // Teal and Orange
            canvas.drawColor(Color.argb(38, 0, 105, 115))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, height, width, 0f, Color.argb(70, 255, 100, 30), Color.TRANSPARENT, Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        10 -> { // Matte
            canvas.drawColor(Color.argb(50, 35, 25, 20))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, height, Color.argb(30, 255, 255, 255), Color.argb(80, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        11 -> { // Vintage
            canvas.drawColor(Color.argb(76, 120, 70, 32))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, width, height, Color.argb(45, 255, 210, 130), Color.argb(30, 45, 20, 5), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        12 -> { // Cyberpunk
            canvas.drawColor(Color.argb(55, 20, 0, 95))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, width, height, Color.argb(75, 255, 0, 130), Color.argb(55, 0, 180, 255), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        13 -> { // Cinematic
            canvas.drawColor(Color.argb(38, 5, 20, 22))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, height, Color.argb(45, 0, 55, 65), Color.argb(70, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
            paint.shader = null
            paint.color = Color.BLACK
            val bar = height * 0.075f
            canvas.drawRect(0f, 0f, width, bar, paint)
            canvas.drawRect(0f, height - bar, width, height, paint)
        }
        14 -> { // Noir
            canvas.drawColor(Color.argb(105, 8, 8, 8))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, width, height, Color.argb(20, 255, 255, 255), Color.argb(95, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, paint)
        }
        15 -> { // Solar Horror
            canvas.drawColor(Color.argb(92, 12, 2, 4))
            val sunX = width * 0.72f
            val sunY = height * 0.18f
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(sunX, sunY, width * 0.42f, intArrayOf(Color.argb(145, 255, 55, 5), Color.argb(65, 150, 0, 0), Color.TRANSPARENT), floatArrayOf(0f, 0.22f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, glow)
            val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(sunX, sunY, width * 0.10f, intArrayOf(Color.argb(255, 255, 238, 160), Color.argb(255, 255, 75, 0), Color.argb(0, 65, 0, 0)), floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP)
            }
            canvas.drawCircle(sunX, sunY, width * 0.16f, sun)
            val rays = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(80, 255, 35, 5); strokeWidth = width * 0.006f }
            for (i in 0 until 12) {
                val a = Math.toRadians((i * 30).toDouble())
                val r1 = width * 0.18f
                val r2 = width * 0.31f
                canvas.drawLine(sunX + kotlin.math.cos(a).toFloat() * r1, sunY + kotlin.math.sin(a).toFloat() * r1, sunX + kotlin.math.cos(a).toFloat() * r2, sunY + kotlin.math.sin(a).toFloat() * r2, rays)
            }
            val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(width / 2f, height / 2f, width * 0.78f, Color.TRANSPARENT, Color.argb(160, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, vignette)
            val bar = height * 0.045f
            val bars = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
            canvas.drawRect(0f, 0f, width, bar, bars)
            canvas.drawRect(0f, height - bar, width, height, bars)
        }
        17 -> { // Cyberpunk Night
            canvas.drawColor(Color.argb(138, 2, 5, 24))
            val grade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, height, width, 0f, Color.argb(100, 0, 140, 210), Color.argb(95, 200, 0, 150), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, grade)
            val cyanGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(width * 0.22f, height * 0.38f, width * 0.30f, Color.argb(115, 0, 235, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, cyanGlow)
            val magentaGlow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(width * 0.82f, height * 0.30f, width * 0.34f, Color.argb(110, 255, 0, 185), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, magentaGlow)
            val lines = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(24, 90, 220, 255); strokeWidth = 1f }
            var y = 0f
            while (y < height) { canvas.drawLine(0f, y, width, y, lines); y += 8f }
            val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(width / 2f, height / 2f, width * 0.72f, Color.TRANSPARENT, Color.argb(180, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, vignette)
        }
        18 -> { // Neon Night
            canvas.drawColor(Color.argb(112, 3, 8, 16))
            val cool = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(0f, 0f, 0f, height, Color.argb(70, 8, 90, 155), Color.argb(95, 2, 3, 20), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, cool)
            val neon = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(width * 0.52f, height * 0.42f, width * 0.44f, intArrayOf(Color.argb(82, 0, 255, 210), Color.argb(30, 40, 35, 180), Color.TRANSPARENT), floatArrayOf(0f, 0.48f, 1f), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, neon)
            val hot = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(width * 0.76f, height * 0.20f, width * 0.24f, Color.argb(125, 255, 50, 155), Color.TRANSPARENT, Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, hot)
            val bloom = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(38, 220, 245, 255); strokeWidth = width * 0.012f }
            canvas.drawLine(0f, height * 0.60f, width, height * 0.60f, bloom)
            val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = RadialGradient(width / 2f, height / 2f, width * 0.80f, Color.TRANSPARENT, Color.argb(155, 0, 0, 0), Shader.TileMode.CLAMP) }
            canvas.drawRect(0f, 0f, width, height, vignette)
        }
        16 -> { // Film Stock
            // Lifted blacks + warm highlights + cool shadows for a scanned-film character.
            canvas.drawColor(Color.argb(34, 22, 15, 12))
            val grade = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, height, Color.argb(58, 255, 195, 112), Color.argb(52, 26, 43, 60), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, grade)
            val halation = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(width * 0.5f, height * 0.35f, width * 0.62f, Color.argb(28, 255, 120, 45), Color.TRANSPARENT, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, halation)
            val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(width / 2f, height / 2f, width * 0.78f, Color.TRANSPARENT, Color.argb(105, 0, 0, 0), Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width, height, vignette)
            val grain = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(30, 245, 230, 205) }
            for (i in 0 until 180) {
                val gx = ((i * 83 + 41) % 997) / 997f * width
                val gy = ((i * 197 + 17) % 991) / 991f * height
                val size = if (i % 5 == 0) 1.6f else 0.8f
                canvas.drawCircle(gx, gy, size, grain)
            }
            val bar = height * 0.05f
            val bars = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(235, 8, 7, 6) }
            canvas.drawRect(0f, 0f, width, bar, bars)
            canvas.drawRect(0f, height - bar, width, height, bars)
        }
    }
}

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var modeButton: Button
    private lateinit var shutterButton: Button
    private lateinit var flashButton: Button
    private lateinit var qualityButton: Button
    private lateinit var exposureText: TextView
    private lateinit var exposureBar: SeekBar
    private lateinit var filterButton: Button
    private lateinit var filterOverlay: FilterOverlay
    private lateinit var galleryButton: Button
    private lateinit var proControls: LinearLayout
    private lateinit var shutterValueText: TextView
    private lateinit var isoValueText: TextView
    private lateinit var whiteBalanceButton: Button
    private lateinit var portraitControls: LinearLayout
    private lateinit var blurValueText: TextView
    private lateinit var focusValueText: TextView
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var camera: Camera? = null
    private var videoMode = false
    private var proPhotoMode = false
    private var photoEnhancementMode = 0 // 0 auto, 1 HDR, 2 Night, 3 Bokeh, 4 Face Retouch
    private var forceLowPower = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashEnabled = false
    private var recordingNow = false
    private var recordingStartedAt = 0L
    private var lowPowerProfile = false
    private var request4K = true
    private var eisEnabled = false
    private var currentFilter = 0
    private var savedFilterEffect: OverlayEffect? = null
    private val shutterSpeedsNs = longArrayOf(33333333L, 16666666L, 8333333L, 4166666L, 2000000L, 1000000L)
    private val shutterLabels = arrayOf("1/30", "1/60", "1/120", "1/240", "1/500", "1/1000")
    private val isoValues = intArrayOf(100, 200, 400, 800, 1600, 3200)
    private var shutterIndex = 1
    private var isoIndex = 1
    private val whiteBalanceModes = intArrayOf(
        CaptureRequest.CONTROL_AWB_MODE_AUTO,
        CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT,
        CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT,
        CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT,
        CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT,
        CaptureRequest.CONTROL_AWB_MODE_SHADE
    )
    private val whiteBalanceLabels = arrayOf("WB AUTO", "DAYLIGHT", "CLOUDY", "FLUORESCENT", "TUNGSTEN", "SHADE")
    private var whiteBalanceIndex = 0
    private var blurStrength = 70
    private var focusIndex = 0
    private var minimumFocusDistance = 0f
    private var extensionControl: Any? = null
    private var activeExtensionsManager: ExtensionsManager? = null
    private var focusLocked = false
    private var longPressTriggered = false
    private var touchDownX = 0f
    private var touchDownY = 0f
    private val focusHandler = Handler(Looper.getMainLooper())
    private val timerHandler = Handler(Looper.getMainLooper())
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (recordingNow) {
                val seconds = (System.currentTimeMillis() - recordingStartedAt) / 1000
                statusText.text = "● REC  ${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
                timerHandler.postDelayed(this, 500)
            }
        }
    }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermissions()) startCamera() else showError("Camera and microphone permissions are required.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(8, 8, 8)
        window.navigationBarColor = Color.rgb(8, 8, 8)
        lowPowerProfile = detectLowPowerDevice()
        buildUI()
        if (hasPermissions()) startCamera() else permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(10)); setBackgroundColor(Color.rgb(8, 8, 8)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = TextView(this).apply { text = "NEXA CAM"; textSize = 23f; letterSpacing = .15f; typeface = android.graphics.Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE) }
        statusText = TextView(this).apply { text = "● READY"; textSize = 11f; setTextColor(Color.rgb(229, 57, 53)); gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        header.addView(logo, LinearLayout.LayoutParams(0, dp(48), 1f)); header.addView(statusText, LinearLayout.LayoutParams(-2, dp(48))); root.addView(header)

        val viewfinder = FrameLayout(this)
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(Color.BLACK)
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        touchDownX = event.x
                        touchDownY = event.y
                        longPressTriggered = false
                        focusHandler.postDelayed({
                            longPressTriggered = true
                            focusAt(touchDownX, touchDownY, lock = true)
                        }, 550)
                    }
                    MotionEvent.ACTION_UP -> {
                        focusHandler.removeCallbacksAndMessages(null)
                        if (!longPressTriggered) {
                            focusLocked = false
                            focusAt(event.x, event.y, lock = false)
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> focusHandler.removeCallbacksAndMessages(null)
                }
                true
            }
        }
        viewfinder.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        filterOverlay = FilterOverlay(this)
        filterOverlay.isClickable = false
        viewfinder.addView(filterOverlay, FrameLayout.LayoutParams(-1, -1))
        val grid = GridOverlay(this)
        viewfinder.addView(grid, FrameLayout.LayoutParams(-1, -1))
        root.addView(viewfinder, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })

        modeButton = makeButton("MODE: PHOTO AUTO")
        modeButton.setOnClickListener { if (!recordingNow) showModePicker() else showError("Stop recording before switching mode.") }
        qualityButton = makeButton("4K AUTO")
        qualityButton.setOnClickListener {
            if (!recordingNow) { request4K = !request4K; updateUI(); if (videoMode) startCamera() }
            else showError("Stop recording before changing quality.")
        }
        val modeRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        modeRow.addView(modeButton, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(5) })
        modeRow.addView(qualityButton, LinearLayout.LayoutParams(dp(96), dp(44)))
        root.addView(modeRow, LinearLayout.LayoutParams(-1, dp(44)).apply { bottomMargin = dp(5) })

        val zoomRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val zoomText = TextView(this).apply { text = "ZOOM 1.0x"; setTextColor(Color.LTGRAY); textSize = 11f }
        zoomRow.addView(zoomText, LinearLayout.LayoutParams(dp(78), dp(36)))
        zoomRow.addView(SeekBar(this).apply {
            max = 100
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    val state = camera?.cameraInfo?.zoomState?.value ?: return
                    val ratio = state.minZoomRatio + (state.maxZoomRatio - state.minZoomRatio) * progress / 100f
                    camera?.cameraControl?.setZoomRatio(ratio)
                    zoomText.text = "ZOOM ${String.format(Locale.US, "%.1f", ratio)}x"
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }, LinearLayout.LayoutParams(0, dp(36), 1f))
        val gridButton = makeButton("GRID ON").apply { setOnClickListener { grid.visibility = if (grid.visibility == View.VISIBLE) View.GONE else View.VISIBLE; text = if (grid.visibility == View.VISIBLE) "GRID ON" else "GRID OFF" } }
        filterButton = makeButton("FILTER").apply { setOnClickListener { showFilterPicker() } }
        zoomRow.addView(gridButton, LinearLayout.LayoutParams(dp(72), dp(36)))
        zoomRow.addView(filterButton, LinearLayout.LayoutParams(dp(72), dp(36)))
        root.addView(zoomRow)

        val exposureRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        exposureText = TextView(this).apply { text = "EV 0.0"; setTextColor(Color.LTGRAY); textSize = 11f }
        exposureRow.addView(exposureText, LinearLayout.LayoutParams(dp(58), dp(32)))
        exposureBar = SeekBar(this).apply {
            max = 100
            progress = 50
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    val state = camera?.cameraInfo?.exposureState ?: return
                    if (!state.isExposureCompensationSupported) return
                    val range = state.exposureCompensationRange
                    val index = range.lower + ((range.upper - range.lower) * progress / 100f).toInt()
                    camera?.cameraControl?.setExposureCompensationIndex(index)
                    exposureText.text = "EV ${String.format(Locale.US, "%.1f", index * state.exposureCompensationStep.toDouble())}"
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }
        exposureRow.addView(exposureBar, LinearLayout.LayoutParams(0, dp(32), 1f))
        exposureRow.addView(TextView(this).apply { text = "EXPOSURE"; textSize = 9f; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(dp(68), dp(32)))
        root.addView(exposureRow)

        proControls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val shutterRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        shutterValueText = TextView(this).apply { text = "1/60"; setTextColor(Color.LTGRAY); textSize = 11f }
        shutterRow.addView(shutterValueText, LinearLayout.LayoutParams(dp(58), dp(32)))
        val shutterBar = SeekBar(this).apply {
            max = shutterSpeedsNs.lastIndex; progress = shutterIndex
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { shutterIndex = progress.coerceIn(0, shutterSpeedsNs.lastIndex); shutterValueText.text = shutterLabels[shutterIndex] }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) { if (manualExposureEnabled() && !recordingNow) startCamera() }
            })
        }
        shutterRow.addView(shutterBar, LinearLayout.LayoutParams(0, dp(32), 1f))
        shutterRow.addView(TextView(this).apply { text = "SHUTTER"; textSize = 9f; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(dp(68), dp(32)))
        proControls.addView(shutterRow)

        val isoRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        isoValueText = TextView(this).apply { text = "ISO 200"; setTextColor(Color.LTGRAY); textSize = 11f }
        isoRow.addView(isoValueText, LinearLayout.LayoutParams(dp(58), dp(32)))
        val isoBar = SeekBar(this).apply {
            max = isoValues.lastIndex; progress = isoIndex
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { isoIndex = progress.coerceIn(0, isoValues.lastIndex); isoValueText.text = "ISO ${isoValues[isoIndex]}" }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) { if (manualExposureEnabled() && !recordingNow) startCamera() }
            })
        }
        isoRow.addView(isoBar, LinearLayout.LayoutParams(0, dp(32), 1f))
        isoRow.addView(TextView(this).apply { text = "ISO"; textSize = 9f; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(dp(68), dp(32)))
        proControls.addView(isoRow)
        whiteBalanceButton = makeButton("WB AUTO")
        whiteBalanceButton.setOnClickListener { showWhiteBalancePicker() }
        proControls.addView(whiteBalanceButton, LinearLayout.LayoutParams(-1, dp(36)).apply { topMargin = dp(2); bottomMargin = dp(3) })
        val focusRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        focusValueText = TextView(this).apply { text = "FOCUS AUTO"; setTextColor(Color.LTGRAY); textSize = 11f }
        focusRow.addView(focusValueText, LinearLayout.LayoutParams(dp(78), dp(32)))
        val focusBar = SeekBar(this).apply {
            max = 100; progress = focusIndex
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) {
                    focusIndex = progress
                    focusValueText.text = if (progress == 0) "FOCUS INF" else "FOCUS ${progress}%"
                }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) { if (manualExposureEnabled() && !recordingNow) startCamera() }
            })
        }
        focusRow.addView(focusBar, LinearLayout.LayoutParams(0, dp(32), 1f))
        focusRow.addView(TextView(this).apply { text = "FOCUS"; textSize = 9f; setTextColor(Color.GRAY) }, LinearLayout.LayoutParams(dp(68), dp(32)))
        proControls.addView(focusRow)
        root.addView(proControls)

        portraitControls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        val blurRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        blurValueText = TextView(this).apply { text = "BLUR 70%"; setTextColor(Color.LTGRAY); textSize = 11f }
        blurRow.addView(blurValueText, LinearLayout.LayoutParams(dp(72), dp(32)))
        val blurBar = SeekBar(this).apply {
            max = 100; progress = blurStrength
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar, progress: Int, fromUser: Boolean) { blurStrength = progress; val label = when (photoEnhancementMode) { 1 -> "HDR"; 2 -> "NIGHT"; 4 -> "RETOUCH"; else -> "BLUR" }; blurValueText.text = "$label ${blurStrength}%"; if (fromUser) applyExtensionStrength() }
                override fun onStartTrackingTouch(bar: SeekBar) = Unit
                override fun onStopTrackingTouch(bar: SeekBar) = Unit
            })
        }
        blurRow.addView(blurBar, LinearLayout.LayoutParams(0, dp(32), 1f))
        portraitControls.addView(blurRow)
        root.addView(portraitControls)

        galleryButton = makeButton("OPEN GALLERY")
        galleryButton.setOnClickListener { openGallery() }
        root.addView(galleryButton, LinearLayout.LayoutParams(-1, dp(38)).apply { topMargin = dp(3); bottomMargin = dp(5) })

        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        flashButton = makeButton("FLASH OFF").apply { setOnClickListener { flashEnabled = !flashEnabled; applyFlash(); updateUI() } }
        shutterButton = makeButton("●").apply { textSize = 27f; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(190, 25, 35)); setOnClickListener { if (videoMode) toggleRecording() else takePhoto() } }
        val flipButton = makeButton("FLIP").apply { setOnClickListener { if (!recordingNow) { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK; startCamera() } } }
        actions.addView(flashButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(6) }); actions.addView(shutterButton, LinearLayout.LayoutParams(0, dp(62), 1.25f).apply { leftMargin = dp(4); rightMargin = dp(4) }); actions.addView(flipButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(6) }); root.addView(actions)
        root.addView(TextView(this).apply {
            text = if (isLowPowerActive()) "AUTO PERFORMANCE  •  TAP TO FOCUS  •  ${qualityLabel()}" else "AUTO QUALITY READY  •  TAP TO FOCUS  •  ${qualityLabel()}"
            textSize = 9f; letterSpacing = .08f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0, dp(8), 0, dp(2))
        })
        setContentView(root); updateUI()
    }

    private fun makeButton(label: String) = Button(this).apply { text = label; textSize = 11f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(31, 31, 31)); stateListAnimator = null }

    private fun showModePicker() {
        val modes = arrayOf("Photo Auto", "Photo Smart Scene", "Photo HDR Boost", "Photo Night Boost", "Photo Portrait", "Photo Face Retouch", "Photo Pro", "Video HD + EIS", "Video 4K + EIS", "Video Low Power")
        val selected = when {
            !videoMode && proPhotoMode -> 6
            !videoMode && photoEnhancementMode == 5 -> 1
            !videoMode && photoEnhancementMode == 1 -> 2
            !videoMode && photoEnhancementMode == 2 -> 3
            !videoMode && photoEnhancementMode == 3 -> 4
            !videoMode && photoEnhancementMode == 4 -> 5
            !videoMode -> 0
            forceLowPower -> 9
            request4K -> 8
            else -> 7
        }
        AlertDialog.Builder(this)
            .setTitle("NEXA CAM MODE")
            .setSingleChoiceItems(modes, selected) { dialog, which ->
                videoMode = which >= 7
                proPhotoMode = which == 6
                photoEnhancementMode = when (which) { 1 -> 5; 2 -> 1; 3 -> 2; 4 -> 3; 5 -> 4; else -> 0 }
                forceLowPower = which == 9
                request4K = which == 8
                dialog.dismiss()
                updateUI()
                startCamera()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun isLowPowerActive() = lowPowerProfile || forceLowPower

    private fun applyExtensionStrength() {
        if (photoEnhancementMode !in 1..4) return
        try {
            val activeCamera = camera ?: return
            val info = activeExtensionsManager?.getCameraExtensionsInfo(activeCamera.cameraInfo) ?: return
            if (info.isExtensionStrengthAvailable) {
                extensionControl = activeExtensionsManager?.getCameraExtensionsControl(activeCamera.cameraControl)
                (extensionControl as? androidx.camera.extensions.CameraExtensionsControl)?.setExtensionStrength(blurStrength)
            }
        } catch (_: Exception) {
            // Unsupported OEM extension: the standard Portrait fallback remains active.
        }
    }

    private fun showWhiteBalancePicker() {
        AlertDialog.Builder(this)
            .setTitle("WHITE BALANCE")
            .setSingleChoiceItems(whiteBalanceLabels, whiteBalanceIndex) { dialog, which ->
                whiteBalanceIndex = which
                whiteBalanceButton.text = whiteBalanceLabels[which]
                dialog.dismiss()
                if (manualExposureEnabled() && !recordingNow) startCamera()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun showFilterPicker() {
        val filters = arrayOf("Original", "Warm", "Cool", "Cinema", "Nexa Red", "Fade", "Noir Tint", "Vivid", "Golden Hour", "Teal & Orange", "Matte", "Vintage", "Cyberpunk", "Cinematic", "Noir", "Solar Horror", "Film Stock", "Cyberpunk Night", "Neon Night")
        AlertDialog.Builder(this)
            .setTitle("FILTERS & EFFECTS")
            .setSingleChoiceItems(filters, currentFilter) { dialog, which ->
                currentFilter = which
                filterOverlay.setFilter(which)
                filterButton.text = if (which == 0) "FILTER" else filters[which].uppercase(Locale.US)
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val baseSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            minimumFocusDistance = if (manualExposureEnabled()) {
                try { Camera2CameraInfo.from(provider.getCameraInfo(baseSelector)).getCameraCharacteristic(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f } catch (_: Exception) { 0f }
            } else 0f
            val selector = selectPhotoEnhancement(provider, baseSelector)
            if (!provider.hasCamera(selector)) { showError("This camera is not available."); return@addListener }
            val previewBuilder = Preview.Builder()
            if (manualExposureEnabled()) applyManualExposure(Camera2Interop.Extender(previewBuilder))
            val preview = previewBuilder.build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val imageBuilder = ImageCapture.Builder()
                .setCaptureMode(if (isLowPowerActive()) ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY else ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(if (isLowPowerActive()) 82 else 95)
                .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
            if (manualExposureEnabled()) applyManualExposure(Camera2Interop.Extender(imageBuilder))
            imageCapture = imageBuilder.build()
            val fallback = if (request4K && !isLowPowerActive()) Quality.FHD else Quality.HD
            val recorder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(preferredVideoQuality(), FallbackStrategy.lowerQualityOrHigherThan(fallback))
            ).build()
            videoCapture = VideoCapture.Builder(recorder)
                .setVideoStabilizationEnabled(true)
                .build()
            try {
                provider.unbindAll()
                val effectTargets = CameraEffect.PREVIEW or if (videoMode) CameraEffect.VIDEO_CAPTURE else CameraEffect.IMAGE_CAPTURE
                savedFilterEffect = OverlayEffect(
                    effectTargets,
                    0,
                    Handler(Looper.getMainLooper()),
                    Consumer { error -> showError("Saved filter disabled: ${error.localizedMessage}") }
                ).also { effect ->
                    effect.setOnDrawListener(Function<Frame, Boolean> { frame -> drawSavedFilter(frame) })
                }
                val groupBuilder = UseCaseGroup.Builder().addUseCase(preview).addEffect(savedFilterEffect!!)
                if (videoMode) groupBuilder.addUseCase(videoCapture!!) else groupBuilder.addUseCase(imageCapture!!)
                camera = provider.bindToLifecycle(this, selector, groupBuilder.build())
                filterOverlay.visibility = View.GONE
                eisEnabled = videoMode
                extensionControl = null
                applyExtensionStrength()
                if (camera?.cameraInfo?.hasFlashUnit() != true) flashEnabled = false
                flashButton.isEnabled = camera?.cameraInfo?.hasFlashUnit() == true
                applyFlash(); configureExposure(); updateUI()
            } catch (e: Exception) {
                filterOverlay.visibility = View.VISIBLE
                try {
                    provider.unbindAll()
                    if (videoMode) {
                        videoCapture = VideoCapture.withOutput(recorder)
                        camera = provider.bindToLifecycle(this, selector, preview, videoCapture)
                    } else {
                        camera = provider.bindToLifecycle(this, selector, preview, imageCapture)
                    }
                    eisEnabled = videoMode
                    applyFlash(); configureExposure(); updateUI()
                    showError("Saved filter unavailable on this camera; original media is being saved.")
                } catch (fallbackError: Exception) {
                    showError("Camera error: ${fallbackError.localizedMessage}")
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun selectPhotoEnhancement(provider: ProcessCameraProvider, baseSelector: CameraSelector): CameraSelector {
        if (videoMode || photoEnhancementMode == 0) return baseSelector
        return try {
            val manager = ExtensionsManager.getInstanceAsync(this, provider).get()
            activeExtensionsManager = manager
            val extensionMode = when (photoEnhancementMode) {
                2 -> ExtensionMode.NIGHT
                3 -> ExtensionMode.BOKEH
                4 -> ExtensionMode.FACE_RETOUCH
                5 -> ExtensionMode.AUTO
                else -> ExtensionMode.HDR
            }
            if (manager.isExtensionAvailable(baseSelector, extensionMode)) {
                manager.getExtensionEnabledCameraSelector(baseSelector, extensionMode)
            } else {
                val name = when (photoEnhancementMode) { 1 -> "HDR"; 2 -> "Night"; 3 -> "Portrait"; 4 -> "Face Retouch"; 5 -> "Smart Scene"; else -> "HDR" }
                showError("$name mode is not supported; using Auto.")
                baseSelector
            }
        } catch (_: Exception) {
            showError("Smart photo mode unavailable; using Auto.")
            baseSelector
        }
    }

    private fun applyFlash() {
        val available = camera?.cameraInfo?.hasFlashUnit() == true
        if (!available) flashEnabled = false
        imageCapture?.flashMode = if (flashEnabled && available) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(flashEnabled && available && videoMode)
    }
    private fun drawSavedFilter(frame: Frame): Boolean {
        renderColorEffect(frame.overlayCanvas, currentFilter, frame.size.width.toFloat(), frame.size.height.toFloat())
        return true
    }

    private fun manualExposureEnabled() = proPhotoMode && !videoMode
    private fun applyManualExposure(extender: Camera2Interop.Extender<*>) {
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, shutterSpeedsNs[shutterIndex])
        extender.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, isoValues[isoIndex])
        extender.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, whiteBalanceModes[whiteBalanceIndex])
        if (minimumFocusDistance > 0f) {
            extender.setCaptureRequestOption(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            extender.setCaptureRequestOption(CaptureRequest.LENS_FOCUS_DISTANCE, minimumFocusDistance * focusIndex / 100f)
        }
    }

    private fun configureExposure() {
        if (manualExposureEnabled()) {
            exposureBar.isEnabled = false
            exposureText.text = "MANUAL"
            return
        }
        val state = camera?.cameraInfo?.exposureState ?: return
        exposureBar.isEnabled = state.isExposureCompensationSupported
        exposureText.text = if (state.isExposureCompensationSupported) "EV 0.0" else "EV N/A"
        if (state.isExposureCompensationSupported) exposureBar.progress = 50
    }

    private fun focusAt(x: Float, y: Float, lock: Boolean) {
        val point = previewView.meteringPointFactory.createPoint(x, y)
        val actionBuilder = FocusMeteringAction.Builder(point)
        if (lock) actionBuilder.disableAutoCancel() else actionBuilder.setAutoCancelDuration(3, TimeUnit.SECONDS)
        camera?.cameraControl?.startFocusAndMetering(actionBuilder.build())
        focusLocked = lock
        if (!recordingNow) statusText.text = if (lock) "● AF LOCK" else "● READY"
    }

    private fun openGallery() {
        val mediaUri = MediaStore.Files.getContentUri("external")
        val galleryIntent = Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(mediaUri, "*/*")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(galleryIntent)
        } catch (_: Exception) {
            val fallback = Intent(android.content.Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                addCategory(android.content.Intent.CATEGORY_OPENABLE)
            }
            startActivity(Intent.createChooser(fallback, "Choose media"))
        }
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return; val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, "NEXA_${timestamp()}.jpg"); put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg"); if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/NEXA CAM") }
        val output = ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
        capture.takePicture(output, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) = runOnUiThread { Toast.makeText(this@MainActivity, "Photo saved to Pictures/NEXA CAM", Toast.LENGTH_SHORT).show() }
            override fun onError(exception: ImageCaptureException) = runOnUiThread { showError("Photo failed: ${exception.message}") }
        })
    }

    private fun toggleRecording() {
        if (recordingNow) { activeRecording?.stop(); return }
        val capture = videoCapture ?: return
        val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, "NEXA_${timestamp()}.mp4"); put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4"); if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/NEXA CAM") }
        val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
        var pending = capture.output.prepareRecording(this, output)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) pending = pending.withAudioEnabled()
        activeRecording = pending.start(ContextCompat.getMainExecutor(this)) { event -> when (event) {
            is VideoRecordEvent.Start -> { recordingNow = true; recordingStartedAt = System.currentTimeMillis(); timerHandler.post(timerRunnable); updateUI() }
            is VideoRecordEvent.Finalize -> { recordingNow = false; activeRecording = null; timerHandler.removeCallbacks(timerRunnable); updateUI(); Toast.makeText(this, if (event.hasError()) "Video save failed." else "Video saved to Movies/NEXA CAM", Toast.LENGTH_LONG).show() }
        } }
    }

    private fun hasPermissions() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    private fun detectLowPowerDevice(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.isLowRamDevice || manager.memoryClass < 256
    }
    private fun preferredVideoQuality(): Quality = when {
        isLowPowerActive() -> Quality.SD
        request4K -> Quality.UHD
        else -> Quality.HD
    }
    private fun qualityLabel(): String = when {
        isLowPowerActive() -> "LOW POWER 480p"
        request4K -> "4K AUTO"
        else -> "HD 720p"
    }
    private fun modeLabel(): String = when {
        !videoMode && proPhotoMode -> "MODE: PHOTO PRO"
        !videoMode && photoEnhancementMode == 5 -> "MODE: SMART SCENE"
        !videoMode && photoEnhancementMode == 1 -> "MODE: PHOTO HDR BOOST"
        !videoMode && photoEnhancementMode == 2 -> "MODE: PHOTO NIGHT BOOST"
        !videoMode && photoEnhancementMode == 3 -> "MODE: PHOTO PORTRAIT"
        !videoMode && photoEnhancementMode == 4 -> "MODE: FACE RETOUCH"
        !videoMode -> "MODE: PHOTO AUTO"
        forceLowPower -> "MODE: VIDEO LOW POWER"
        request4K -> "MODE: VIDEO 4K"
        else -> "MODE: VIDEO HD"
    }
    private fun updateUI() { if (!::modeButton.isInitialized) return; modeButton.text = modeLabel(); qualityButton.text = if (request4K) "4K AUTO" else "HD AUTO"; flashButton.text = if (flashEnabled) "FLASH / TORCH ON" else "FLASH OFF"; shutterButton.text = if (videoMode) if (recordingNow) "■ STOP" else "● REC" else "●"; if (::proControls.isInitialized) proControls.visibility = if (manualExposureEnabled()) View.VISIBLE else View.GONE; if (::portraitControls.isInitialized) portraitControls.visibility = if (!videoMode && photoEnhancementMode in 1..4) View.VISIBLE else View.GONE; if (!recordingNow && !focusLocked) statusText.text = if (videoMode && eisEnabled) "● READY  •  EIS ON" else "● READY" }
    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    private fun showError(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onStop() { if (recordingNow) activeRecording?.stop(); super.onStop() }
    override fun onDestroy() { timerHandler.removeCallbacksAndMessages(null); focusHandler.removeCallbacksAndMessages(null); cameraExecutor.shutdown(); super.onDestroy() }

    private class GridOverlay(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(115, 255, 255, 255); strokeWidth = 1f }
        override fun onDraw(canvas: Canvas) { val w = width.toFloat(); val h = height.toFloat(); canvas.drawLine(w / 3, 0f, w / 3, h, paint); canvas.drawLine(w * 2 / 3, 0f, w * 2 / 3, h, paint); canvas.drawLine(0f, h / 3, w, h / 3, paint); canvas.drawLine(0f, h * 2 / 3, w, h * 2 / 3, paint) }
    }

    private class FilterOverlay(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var filter = 0
        fun setFilter(value: Int) { filter = value; invalidate() }
        override fun onDraw(canvas: Canvas) {
            renderColorEffect(canvas, filter, width.toFloat(), height.toFloat())
        }
    }
}
