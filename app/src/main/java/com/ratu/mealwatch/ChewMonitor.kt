package com.ratu.mealwatch

/**
 * Status pemantauan yang bisa dibaca oleh UI / pemicu suara.
 */
enum class MealState {
    CHEWING,           // Anak terdeteksi & sedang mengunyah dengan normal
    FACE_PRESENT_IDLE, // Wajah terdeteksi tapi tidak ada gerakan mengunyah dalam waktu lama
    FACE_NOT_FOUND     // Anak tidak terdeteksi di kamera (keluar jangkauan)
}

/**
 * ChewMonitor menerima rasio bukaan mulut dari setiap frame, lalu menyimpulkan
 * apakah anak sedang mengunyah, diam saja, atau tidak ada di depan kamera.
 *
 * Logika deteksi "mengunyah":
 * Mengunyah = mulut membuka & menutup berulang kali (osilasi), bukan cuma buka sekali (misal ngobrol/menguap).
 * Kita hitung jumlah transisi "tertutup -> terbuka" dalam sliding window waktu.
 * Jika jumlah transisi >= CHEW_TRANSITIONS_THRESHOLD dalam WINDOW_MS -> dianggap mengunyah.
 */
class ChewMonitor(
    private val mouthOpenThreshold: Float = 0.18f,   // rasio di atas ini = mulut "terbuka"
    private val windowMs: Long = 6000L,               // jendela waktu untuk menghitung osilasi
    private val chewTransitionsThreshold: Int = 3,    // min. jumlah buka-tutup dalam window agar dianggap mengunyah
    private val idleWarningMs: Long = 20_000L,        // wajah ada tapi tak mengunyah selama ini -> peringatan
    private val faceLostWarningMs: Long = 6_000L      // wajah tak terdeteksi selama ini -> peringatan
) {
    private var lastMouthOpen = false
    private val transitionTimestamps = mutableListOf<Long>()

    private var lastChewDetectedAt = System.currentTimeMillis()
    private var faceLostSince: Long? = null

    var totalChewCycles = 0
        private set

    /**
     * Panggil setiap kali ada hasil frame baru dari FaceMeshAnalyzer.
     * Mengembalikan MealState terkini.
     */
    fun update(faceDetected: Boolean, mouthOpenRatio: Float): MealState {
        val now = System.currentTimeMillis()

        if (!faceDetected) {
            if (faceLostSince == null) faceLostSince = now
            return if (now - (faceLostSince ?: now) >= faceLostWarningMs) {
                MealState.FACE_NOT_FOUND
            } else {
                // Beri toleransi sedikit sebelum benar-benar dianggap "hilang"
                MealState.FACE_PRESENT_IDLE
            }
        } else {
            faceLostSince = null
        }

        val mouthOpenNow = mouthOpenRatio >= mouthOpenThreshold

        // Deteksi transisi tertutup -> terbuka (satu "gigitan/kunyahan")
        if (mouthOpenNow && !lastMouthOpen) {
            transitionTimestamps.add(now)
            totalChewCycles++
        }
        lastMouthOpen = mouthOpenNow

        // Buang data lama di luar window
        transitionTimestamps.removeAll { now - it > windowMs }

        return if (transitionTimestamps.size >= chewTransitionsThreshold) {
            lastChewDetectedAt = now
            MealState.CHEWING
        } else if (now - lastChewDetectedAt >= idleWarningMs) {
            MealState.FACE_PRESENT_IDLE
        } else {
            // Masih dalam toleransi (misal baru menyendok makanan, belum sempat mengunyah)
            MealState.CHEWING
        }
    }

    fun reset() {
        lastMouthOpen = false
        transitionTimestamps.clear()
        lastChewDetectedAt = System.currentTimeMillis()
        faceLostSince = null
        totalChewCycles = 0
    }
}
