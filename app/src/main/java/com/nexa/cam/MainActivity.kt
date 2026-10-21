package com.nexa.cam

import android.Manifest
import android.app.ActivityManager
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var modeButton: Button
    private lateinit var shutterButton: Button
    private lateinit var flashButton: Button
    private lateinit var qualityButton: Button
    private lateinit var exposureText: TextView
    private lateinit var exposureBar: SeekBar
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var camera: Camera? = null
    private var videoMode = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashEnabled = false
    private var recordingNow = false
    private var recordingStartedAt = 0L
    private var lowPowerProfile = false
    private var request4K = true
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
            setOnTouchListener { _, event -> if (event.action == MotionEvent.ACTION_UP) focusAt(event.x, event.y); true }
        }
        viewfinder.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        val grid = GridOverlay(this)
        viewfinder.addView(grid, FrameLayout.LayoutParams(-1, -1))
        root.addView(viewfinder, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8); bottomMargin = dp(8) })

        modeButton = makeButton("MODE: PHOTO")
        modeButton.setOnClickListener { if (!recordingNow) { videoMode = !videoMode; updateUI(); startCamera() } else showError("Stop recording before switching mode.") }
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
        zoomRow.addView(gridButton, LinearLayout.LayoutParams(dp(92), dp(36))); root.addView(zoomRow)

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

        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        flashButton = makeButton("FLASH OFF").apply { setOnClickListener { flashEnabled = !flashEnabled; applyFlash(); updateUI() } }
        shutterButton = makeButton("●").apply { textSize = 27f; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(190, 25, 35)); setOnClickListener { if (videoMode) toggleRecording() else takePhoto() } }
        val flipButton = makeButton("FLIP").apply { setOnClickListener { if (!recordingNow) { lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK; startCamera() } } }
        actions.addView(flashButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply { rightMargin = dp(6) }); actions.addView(shutterButton, LinearLayout.LayoutParams(0, dp(62), 1.25f).apply { leftMargin = dp(4); rightMargin = dp(4) }); actions.addView(flipButton, LinearLayout.LayoutParams(0, dp(54), 1f).apply { leftMargin = dp(6) }); root.addView(actions)
        root.addView(TextView(this).apply {
            text = if (lowPowerProfile) "AUTO PERFORMANCE  •  TAP TO FOCUS  •  ${qualityLabel()}" else "AUTO QUALITY READY  •  TAP TO FOCUS  •  ${qualityLabel()}"
            textSize = 9f; letterSpacing = .08f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(0, dp(8), 0, dp(2))
        })
        setContentView(root); updateUI()
    }

    private fun makeButton(label: String) = Button(this).apply { text = label; textSize = 11f; isAllCaps = false; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(31, 31, 31)); stateListAnimator = null }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get(); val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            if (!provider.hasCamera(selector)) { showError("This camera is not available."); return@addListener }
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(if (lowPowerProfile) ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY else ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setJpegQuality(if (lowPowerProfile) 82 else 95)
                .setFlashMode(if (flashEnabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
                .build()
            val fallback = if (request4K && !lowPowerProfile) Quality.FHD else Quality.HD
            val recorder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(preferredVideoQuality(), FallbackStrategy.lowerQualityOrHigherThan(fallback))
            ).build()
            videoCapture = VideoCapture.withOutput(recorder)
            try {
                provider.unbindAll()
                camera = if (videoMode) provider.bindToLifecycle(this, selector, preview, videoCapture) else provider.bindToLifecycle(this, selector, preview, imageCapture)
                if (camera?.cameraInfo?.hasFlashUnit() != true) flashEnabled = false
                flashButton.isEnabled = camera?.cameraInfo?.hasFlashUnit() == true
                applyFlash(); configureExposure(); updateUI()
            } catch (e: Exception) { showError("Camera error: ${e.localizedMessage}") }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun applyFlash() {
        val available = camera?.cameraInfo?.hasFlashUnit() == true
        if (!available) flashEnabled = false
        imageCapture?.flashMode = if (flashEnabled && available) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        camera?.cameraControl?.enableTorch(flashEnabled && available && videoMode)
    }
    private fun configureExposure() {
        val state = camera?.cameraInfo?.exposureState ?: return
        exposureBar.isEnabled = state.isExposureCompensationSupported
        exposureText.text = if (state.isExposureCompensationSupported) "EV 0.0" else "EV N/A"
        if (state.isExposureCompensationSupported) exposureBar.progress = 50
    }

    private fun focusAt(x: Float, y: Float) { val point = previewView.meteringPointFactory.createPoint(x, y); camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).setAutoCancelDuration(3, TimeUnit.SECONDS).build()) }

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
        lowPowerProfile -> Quality.SD
        request4K -> Quality.UHD
        else -> Quality.HD
    }
    private fun qualityLabel(): String = when {
        lowPowerProfile -> "LOW POWER 480p"
        request4K -> "4K AUTO"
        else -> "HD 720p"
    }
    private fun updateUI() { if (!::modeButton.isInitialized) return; modeButton.text = if (videoMode) "MODE: VIDEO  •  TAP TO SWITCH" else "MODE: PHOTO  •  TAP TO SWITCH"; qualityButton.text = if (request4K) "4K AUTO" else "HD AUTO"; flashButton.text = if (flashEnabled) "FLASH / TORCH ON" else "FLASH OFF"; shutterButton.text = if (videoMode) if (recordingNow) "■ STOP" else "● REC" else "●"; if (!recordingNow) statusText.text = "● READY" }
    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    private fun showError(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onStop() { if (recordingNow) activeRecording?.stop(); super.onStop() }
    override fun onDestroy() { timerHandler.removeCallbacksAndMessages(null); cameraExecutor.shutdown(); super.onDestroy() }

    private class GridOverlay(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(115, 255, 255, 255); strokeWidth = 1f }
        override fun onDraw(canvas: Canvas) { val w = width.toFloat(); val h = height.toFloat(); canvas.drawLine(w / 3, 0f, w / 3, h, paint); canvas.drawLine(w * 2 / 3, 0f, w * 2 / 3, h, paint); canvas.drawLine(0f, h / 3, w, h / 3, paint); canvas.drawLine(0f, h * 2 / 3, w, h * 2 / 3, paint) }
    }
}
