package com.nexa.cam

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
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

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var modeButton: Button
    private lateinit var shutterButton: Button
    private lateinit var flipButton: Button
    private lateinit var flashButton: Button

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var camera: Camera? = null

    private var videoMode = false
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var flashEnabled = false
    private var recordingNow = false

    private val cameraExecutor: ExecutorService =
        Executors.newSingleThreadExecutor()

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            if (hasPermissions()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera and microphone permissions are required.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(8, 8, 8)
        window.navigationBarColor = Color.rgb(8, 8, 8)

        buildUI()

        if (hasPermissions()) {
            startCamera()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    private fun buildUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(8), dp(14), dp(10))
            setBackgroundColor(Color.rgb(8, 8, 8))
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }

        val logo = TextView(this).apply {
            text = "NEXA CAM"
            textSize = 23f
            letterSpacing = 0.15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        }

        statusText = TextView(this).apply {
            text = "● READY"
            textSize = 11f
            setTextColor(Color.rgb(229, 57, 53))
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(0, dp(48), 1f)
        )

        header.addView(
            statusText,
            LinearLayout.LayoutParams(-2, dp(48))
        )

        root.addView(header)

        previewView = PreviewView(this).apply {
            implementationMode =
                PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            setBackgroundColor(Color.BLACK)
        }

        root.addView(
            previewView,
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(8)
                bottomMargin = dp(12)
            }
        )

        modeButton = makeButton("MODE: PHOTO")

        modeButton.setOnClickListener {
            if (recordingNow) {
                Toast.makeText(
                    this,
                    "Stop recording before switching mode.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                videoMode = !videoMode
                updateUI()
                startCamera()
            }
        }

        root.addView(
            modeButton,
            LinearLayout.LayoutParams(-1, dp(46)).apply {
                bottomMargin = dp(8)
            }
        )

        val actions = LinearLayout(this).apply {
            gravity = Gravity.CENTER
        }

        flashButton = makeButton("FLASH OFF")

        flashButton.setOnClickListener {
            flashEnabled = !flashEnabled

            camera?.cameraControl?.enableTorch(flashEnabled)

            imageCapture?.flashMode =
                if (flashEnabled) {
                    ImageCapture.FLASH_MODE_ON
                } else {
                    ImageCapture.FLASH_MODE_OFF
                }

            updateUI()
        }

        shutterButton = makeButton("●").apply {
            textSize = 27f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(190, 25, 35))
        }

        shutterButton.setOnClickListener {
            if (videoMode) {
                toggleRecording()
            } else {
                takePhoto()
            }
        }

        flipButton = makeButton("FLIP")

        flipButton.setOnClickListener {
            if (!recordingNow) {
                lensFacing =
                    if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                        CameraSelector.LENS_FACING_FRONT
                    } else {
                        CameraSelector.LENS_FACING_BACK
                    }

                startCamera()
            }
        }

        actions.addView(
            flashButton,
            LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                rightMargin = dp(6)
            }
        )

        actions.addView(
            shutterButton,
            LinearLayout.LayoutParams(0, dp(62), 1.25f).apply {
                leftMargin = dp(4)
                rightMargin = dp(4)
            }
        )

        actions.addView(
            flipButton,
            LinearLayout.LayoutParams(0, dp(54), 1f).apply {
                leftMargin = dp(6)
            }
        )

        root.addView(actions)

        val footer = TextView(this).apply {
            text = "PRO PHOTO  •  CINEMA VIDEO"
            textSize = 10f
            letterSpacing = 0.12f
            gravity = Gravity.CENTER
            setTextColor(Color.LTGRAY)
            setPadding(0, dp(12), 0, dp(3))
        }

        root.addView(footer)

        setContentView(root)
        updateUI()
    }

    private fun makeButton(label: String): Button {
        return Button(this).apply {
            text = label
            textSize = 12f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(31, 31, 31))
            stateListAnimator = null
        }
    }

    private fun updateUI() {
        if (!::modeButton.isInitialized) return

        modeButton.text =
            if (videoMode) {
                "MODE: VIDEO  •  TAP TO SWITCH"
            } else {
                "MODE: PHOTO  •  TAP TO SWITCH"
            }

        flashButton.text =
            if (flashEnabled) "FLASH / TORCH ON" else "FLASH OFF"

        shutterButton.text =
            if (videoMode) {
                if (recordingNow) "■ STOP" else "● REC"
            } else {
                "●"
            }

        statusText.text =
            if (recordingNow) "● REC" else "● READY"
    }

    private fun hasPermissions(): Boolean {
        val cameraGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        val audioGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

        return cameraGranted && audioGranted
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)

        future.addListener({

            val provider = future.get()

            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            if (!provider.hasCamera(selector)) {
                Toast.makeText(
                    this,
                    "This camera is not available.",
                    Toast.LENGTH_SHORT
                ).show()
                return@addListener
            }

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(
                    ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
                )
                .setFlashMode(
                    if (flashEnabled) {
                        ImageCapture.FLASH_MODE_ON
                    } else {
                        ImageCapture.FLASH_MODE_OFF
                    }
                )
                .build()

            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.HD)
                )
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            try {
                provider.unbindAll()

                camera =
                    if (videoMode) {
                        provider.bindToLifecycle(
                            this,
                            selector,
                            preview,
                            videoCapture
                        )
                    } else {
                        provider.bindToLifecycle(
                            this,
                            selector,
                            preview,
                            imageCapture
                        )
                    }

                camera?.cameraControl?.enableTorch(
                    flashEnabled && videoMode
                )

            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Camera error: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return

        val stamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(System.currentTimeMillis())

        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "NEXA_$stamp.jpg"
            )

            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "image/jpeg"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/NEXA CAM"
                )
            }
        }

        val output = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        capture.takePicture(
            output,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(
                    results: ImageCapture.OutputFileResults
                ) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Photo saved to Pictures/NEXA CAM",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(
                    exception: ImageCaptureException
                ) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Photo failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }

    private fun toggleRecording() {
        if (recordingNow) {
            activeRecording?.stop()
            recordingNow = false
            updateUI()
            return
        }

        val capture = videoCapture ?: return

        val stamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.US
        ).format(System.currentTimeMillis())

        val values = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                "NEXA_$stamp.mp4"
            )

            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "video/mp4"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "Movies/NEXA CAM"
                )
            }
        }

        val output = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(values)
            .build()

        var pending = capture.output.prepareRecording(
            this,
            output
        )

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pending = pending.withAudioEnabled()
        }

        activeRecording = pending.start(
            ContextCompat.getMainExecutor(this)
        ) { event ->

            when (event) {

                is VideoRecordEvent.Start -> {
                    recordingNow = true
                    updateUI()
                }

                is VideoRecordEvent.Finalize -> {
                    recordingNow = false
                    activeRecording = null
                    updateUI()

                    val message =
                        if (event.hasError()) {
                            "Video save failed."
                        } else {
                            "Video saved to Movies/NEXA CAM"
                        }

                    Toast.makeText(
                        this,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).toInt()
    }

    override fun onDestroy() {
        activeRecording?.stop()
        cameraExecutor.shutdown()
        super.onDestroy()
    }
}
