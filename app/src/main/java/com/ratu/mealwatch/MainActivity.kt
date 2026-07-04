package com.ratu.mealwatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.ratu.mealwatch.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var chewMonitor: ChewMonitor
    private lateinit var reminderManager: ReminderManager

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                binding.statusText.text = "Izin kamera diperlukan agar MealWatch bisa bekerja."
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()
        chewMonitor = ChewMonitor()
        reminderManager = ReminderManager(this)

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceMeshAnalyzer { faceDetected, ratio ->
                        handleFrameResult(faceDetected, ratio)
                    })
                }

            // Kamera depan, karena anak biasanya menghadap layar/tablet saat makan
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                binding.statusText.text = "Memantau waktu makan..."
            } catch (exc: Exception) {
                binding.statusText.text = "Gagal membuka kamera: ${exc.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleFrameResult(faceDetected: Boolean, mouthOpenRatio: Float) {
        val state = chewMonitor.update(faceDetected, mouthOpenRatio)

        runOnUiThread {
            binding.chewCountText.text = "Kunyahan terdeteksi: ${chewMonitor.totalChewCycles}"

            when (state) {
                MealState.CHEWING -> {
                    binding.statusText.text = "✅ Sedang makan & mengunyah"
                }
                MealState.FACE_PRESENT_IDLE -> {
                    binding.statusText.text = "⚠️ Belum terlihat mengunyah, ayo lanjut makan"
                    reminderManager.sayIdleReminder()
                }
                MealState.FACE_NOT_FOUND -> {
                    binding.statusText.text = "🔴 Anak tidak terdeteksi di kamera"
                    reminderManager.sayFaceLostReminder()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        reminderManager.shutdown()
    }
}
