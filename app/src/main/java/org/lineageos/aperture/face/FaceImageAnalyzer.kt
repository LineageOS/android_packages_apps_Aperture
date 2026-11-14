/*
 * SPDX-FileCopyrightText: 2026 Серж
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.aperture.face

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Одно лицо в кадре: где оно и какого размера.
 *
 * [centerX] и [centerY] — середина лица в долях кадра от 0 до 1, начало
 * координат в левом верхнем углу. [widthFraction] — ширина лица в долях
 * ширины кадра: по ней видно, далеко человек или вплотную.
 */
data class FaceInfo(
    val centerX: Float,
    val centerY: Float,
    val widthFraction: Float,
)

/**
 * Ищет лица в кадре офлайн, без интернета: модель определения лиц лежит
 * внутри приложения (ML Kit, packed model).
 *
 * Результат отдаётся потоком [faces]. Кадры разбираются не чаще, чем раз в
 * [MIN_FRAME_INTERVAL_MS], чтобы не греть телефон и не мешать съёмке.
 */
class FaceImageAnalyzer : ImageAnalysis.Analyzer {
    private val detector: FaceDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            // FAST хватает: нужно положение и размер лица, а не черты
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(MIN_FACE_SIZE)
            .build()

        FaceDetection.getClient(options)
    }

    private val _faces = MutableStateFlow<List<FaceInfo>>(emptyList())
    val faces: StateFlow<List<FaceInfo>> = _faces

    /**
     * Фронтальная камера показывает зеркало: слева на экране то, что справа
     * в кадре. Чтобы слова про «слева» и «справа» не путали, зёркалим ось X.
     */
    @Volatile
    var mirrorHorizontally = false

    private var busy = false
    private var lastAnalyzedAt = 0L

    override fun analyze(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()

        // Кадр слишком частый или предыдущий ещё не разобран
        if (busy || now - lastAnalyzedAt < MIN_FRAME_INTERVAL_MS) {
            image.close()
            return
        }

        val mediaImage = image.image
        if (mediaImage == null) {
            image.close()
            return
        }

        busy = true
        lastAnalyzedAt = now

        // ML Kit отдаёт рамки уже с учётом поворота, значит и размеры кадра
        // берём повёрнутые: при 90 и 270 градусах ширина и высота меняются местами
        val rotation = image.imageInfo.rotationDegrees
        val frameWidth: Int
        val frameHeight: Int
        if (rotation == 90 || rotation == 270) {
            frameWidth = image.height
            frameHeight = image.width
        } else {
            frameWidth = image.width
            frameHeight = image.height
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val mirror = mirrorHorizontally

        detector.process(inputImage)
            .addOnSuccessListener { detected ->
                val result = detected.map { face ->
                    val box = face.boundingBox
                    val x = box.centerX().toFloat() / frameWidth
                    val y = box.centerY().toFloat() / frameHeight

                    FaceInfo(
                        centerX = if (mirror) 1f - x else x,
                        centerY = y,
                        widthFraction = box.width().toFloat() / frameWidth,
                    )
                }

                // Порядок лиц ML Kit не гарантирует, для стабильной озвучки
                // сортируем сами: сверху вниз, потом слева направо
                val sorted = result.sortedWith(
                    compareBy({ it.centerY }, { it.centerX })
                )

                if (sorted.size != _faces.value.size || descriptionsDiffer(sorted)) {
                    _faces.value = sorted
                }
            }
            .addOnFailureListener { e ->
                Log.w(LOG_TAG, "Не удалось определить лица: ${e.message}")
            }
            .addOnCompleteListener {
                busy = false
                image.close()
            }
    }

    /**
     * Сравниваем со сдвигом в десятую долю кадра: мелкое дрожание камеры не
     * должно каждый раз считаться новым положением лица.
     */
    private fun descriptionsDiffer(new: List<FaceInfo>): Boolean =
        new.map { it.cell() } != _faces.value.map { it.cell() }

    fun release() {
        detector.close()
    }

    companion object {
        private const val LOG_TAG = "ApertureFace"
        private const val MIN_FRAME_INTERVAL_MS = 250L
        private const val MIN_FACE_SIZE = 0.08f
    }
}

/**
 * Делит кадр на девять частей: три по горизонтали и три по вертикали.
 * Возвращает номер клетки — по нему видно, изменилось ли положение лица.
 */
fun FaceInfo.cell(): Int {
    val column = when {
        centerX < ONE_THIRD -> 0
        centerX > TWO_THIRDS -> 2
        else -> 1
    }
    val row = when {
        centerY < ONE_THIRD -> 0
        centerY > TWO_THIRDS -> 2
        else -> 1
    }

    return row * 3 + column
}

private const val ONE_THIRD = 1f / 3f
private const val TWO_THIRDS = 2f / 3f
