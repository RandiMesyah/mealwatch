package com.ratu.mealwatch

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import kotlin.math.sqrt

/**
 * Menganalisis setiap frame kamera menggunakan ML Kit Face Mesh (468 titik wajah).
 *
 * Titik penting yang dipakai (indeks mengikuti standar MediaPipe Face Mesh):
 *   - 13  = tengah bibir bagian dalam ATAS
 *   - 14  = tengah bibir bagian dalam BAWAH
 *   - 234 = pipi kiri (dipakai sebagai referensi lebar wajah)
 *   - 454 = pipi kanan (referensi lebar wajah)
 *
 * mouthOpenRatio = jarak(atas,bawah) / jarak(pipiKiri,pipiKanan)
 * Rasio dinormalisasi terhadap lebar wajah agar tidak terpengaruh jarak anak ke kamera.
 */
class FaceMeshAnalyzer(
    private val onResult: (faceDetected: Boolean, mouthOpenRatio: Float) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceMeshDetection.getClient(
        FaceMeshDetectorOptions.Builder()
            .setUseCase(FaceMeshDetectorOptions.FACE_MESH)
            .build()
    )

    companion object {
        private const val UPPER_LIP_INNER = 13
        private const val LOWER_LIP_INNER = 14
        private const val CHEEK_LEFT = 234
        private const val CHEEK_RIGHT = 454
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(inputImage)
            .addOnSuccessListener { faces: List<FaceMesh> ->
                if (faces.isEmpty()) {
                    onResult(false, 0f)
                } else {
                    val face = faces[0]
                    val points = face.allPoints

                    val upper = points.getOrNull(UPPER_LIP_INNER)
                    val lower = points.getOrNull(LOWER_LIP_INNER)
                    val cheekL = points.getOrNull(CHEEK_LEFT)
                    val cheekR = points.getOrNull(CHEEK_RIGHT)

                    if (upper != null && lower != null && cheekL != null && cheekR != null) {
                        val mouthGap = distance(
                            upper.position.x, upper.position.y,
                            lower.position.x, lower.position.y
                        )
                        val faceWidth = distance(
                            cheekL.position.x, cheekL.position.y,
                            cheekR.position.x, cheekR.position.y
                        )
                        val ratio = if (faceWidth > 0f) mouthGap / faceWidth else 0f
                        onResult(true, ratio)
                    } else {
                        onResult(true, 0f)
                    }
                }
            }
            .addOnFailureListener {
                onResult(false, 0f)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
