package com.waveapp.tourcat.helper

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.helper.MLKitTranslatorModule.fixCommonOcrNumberErrors
import com.waveapp.tourcat.util.LogUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageHelper {

    interface OCRListener {
        fun onTextRecognized(blocks: List<Text.TextBlock>)  // ← 수정됨
        fun onError(error: String)
    }
    /**
     * 언어코드("ko", "ja", "zh", "lat" 등) 입력 → 해당 TextRecognizer 인스턴스 반환
     */
    fun getTextRecognizerForLanguage(langCode: String?): TextRecognizer {
        return when (langCode?.lowercase()) {
            "ko" -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
            "ja" -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
            "zh" -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) // 영어/기본 라틴
        }
    }
    fun processImage(
        context: Context,
        bitmap: Bitmap,
        langCode: String,
        listener: OCRListener
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer: TextRecognizer = getTextRecognizerForLanguage(langCode)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val blocks = visionText.textBlocks
                    listener.onTextRecognized(blocks)
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    listener.onError(e.message ?: "OCR Failed")
                }
        } catch (e: Exception) {
            e.printStackTrace()
            listener.onError(context.getString(R.string.msg_error_image_notcomplet    ))
        }
    }
    // 이미지 관련 권한 배열 반환 함수
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(Manifest.permission.CAMERA)
            // Android 12 이하에서는 READ_EXTERNAL_STORAGE 권한을 사용하지 않으므로 비워둡니다.
            // 필요한 경우 이곳에 추가적인 권한을 넣을 수 있습니다.
        }
    }
    fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
        )
    }
    // 권한이 모두 허용되었는지 확인하는 함수
    fun hasImagePermissions(context: Context): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

//     이미지 관련 권한 요청
    fun requestImagePermissions(context: Context, permissionLauncher: ActivityResultLauncher<Array<String>>) {
        val requiredPermissions = getRequiredPermissions()
        permissionLauncher.launch(requiredPermissions)
    }

    //스마트폰 사진  또는 갤러리 열어 이미지 가져오기
    fun openImageChooser(
        context: Context,
        authority: String,
        createFile: () -> File,
        onIntentReady: (Intent, Uri?) -> Unit
    ) {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = try {
            createFile()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }

        var photoUri: Uri? = null
        photoFile?.let {
            photoUri = FileProvider.getUriForFile(context, "$authority.fileprovider", it)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        }

        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"         // OK! (속성 할당, 권장)
            // 또는 setType("image/*") // OK! (함수 호출, Java 스타일)
        }

        val chooser = Intent.createChooser(galleryIntent, context.getString(R.string.camera_gallary ))
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))

        onIntentReady(chooser, photoUri)
    }
    /**
     * 주어진 사각형(들)을 maskColor로 칠해 OCR 대상에서 제외하고,
     * 선택적으로 roi 영역만 잘라낸 비트맵을 반환합니다.
     * - 기존 비트맵은 그대로 두고, 복사본에서 작업합니다.
     */
    fun Bitmap.applyMaskAndCrop(
        maskRects: List<Rect> = emptyList(),
        roi: Rect? = null,
        maskColor: Int = Color.WHITE
    ): Bitmap {
        // 1) 복사본 생성
        val out = copy(config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        // 2) 마스킹
        if (maskRects.isNotEmpty()) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = maskColor
            }
            maskRects.forEach { r ->
                // 입력 좌표가 비트맵 범위를 벗어나더라도 안전하게 clamp
                val clamped = Rect(
                    r.left.coerceIn(0, out.width),
                    r.top.coerceIn(0, out.height),
                    r.right.coerceIn(0, out.width),
                    r.bottom.coerceIn(0, out.height)
                )
                if (!clamped.isEmpty) canvas.drawRect(clamped, paint)
            }
        }

        // 3) ROI 크롭 (선택)
        roi ?: return out
        val roiClamped = Rect(
            roi.left.coerceIn(0, out.width),
            roi.top.coerceIn(0, out.height),
            roi.right.coerceIn(0, out.width),
            roi.bottom.coerceIn(0, out.height)
        )
        if (roiClamped.isEmpty) return out
        return Bitmap.createBitmap(out, roiClamped.left, roiClamped.top, roiClamped.width(), roiClamped.height())
    }
//
//    //촬영한 이미지 저장
//    fun saveBitmapToGallery2(context: Context, bitmap: Bitmap, path: String): Uri? {
//        val filename = "tourcat_${System.currentTimeMillis()}.jpg"
//        val picturesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), path)
//
//        if (!picturesDir.exists()) {
//            picturesDir.mkdirs()
//        }
//
//        val file = File(picturesDir, filename)
//        return try {
//            val outputStream = FileOutputStream(file)
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//            outputStream.flush()
//            outputStream.close()
//
//            MediaScannerConnection.scanFile(
//                context,
//                arrayOf(file.absolutePath),
//                arrayOf("image/jpeg"),
//                null
//            )
//
//            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
//        } catch (e: IOException) {
//            e.printStackTrace()
//            null
//        }
//    }
    /**
     * 비트맵 이미지를 갤러리에 저장합니다. (폴더 지정 가능)
     * Android 10(Q) 이상은 폴더와 IS_PENDING 처리, 하위 버전은 기본 Pictures 하위에 저장됩니다.
     *
     * @param context Context
     * @param bitmap 저장할 Bitmap 객체
     * @param folder 저장할 폴더명 (기본값 "TourCat")
     * @return 저장된 이미지의 Uri, 실패시 null
     */
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, folder: String = ComConstant.FOLDER_TOURCAT): Uri? {
        val filename = "tourcat_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/$folder")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        if (imageUri != null) {
            var out: OutputStream? = null
            try {
                out = resolver.openOutputStream(imageUri)
                if (out == null) {
                    // 스트림 오픈 실패
                    resolver.delete(imageUri, null, null)
                    return null
                }
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                if (!success) {
                    // 비트맵 압축 실패시 이미지 삭제
                    resolver.delete(imageUri, null, null)
                    return null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                resolver.delete(imageUri, null, null) // 실패시 파일 정리
                return null
            } finally {
                try { out?.close() } catch (_: Exception) {}
            }
            // Android 10(Q) 이상: IS_PENDING 해제
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues()
                updateValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(imageUri, updateValues, null, null)
            }
            return imageUri
        } else {
            // Uri 할당 실패
            return null
        }
    }


    fun createTempImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmsss", Locale.getDefault()).format(Date())
        val storageDir = File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            ComConstant.FOLDER_CACHE // 예: "temp" 등
        )
        if (!storageDir.exists()) storageDir.mkdirs()

        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    fun saveBitmapToTempFile(context: Context, bitmap: Bitmap, prefix: String = "temp_"): Uri? {
        return try {
            // 파일 이름 구성
            val timeStamp = SimpleDateFormat("yyyyMMddHHmmsss", Locale.getDefault()).format(Date())
            val fileName = "${prefix}${timeStamp}.jpg"

            // 저장 경로 설정 (앱 전용 Pictures/temp)
            val storageDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                ComConstant.FOLDER_CACHE
            )
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            // 파일 생성 및 저장
            val file = File(storageDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // FileProvider URI 반환
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    fun getRotatedBitmap(context: Context, uri: Uri ): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // EXIF 정보 읽기 (파일 경로 기반이 아니라 InputStream 기반)
        val exifInputStream = context.contentResolver.openInputStream(uri)
        val exif = exifInputStream?.let { ExifInterface(it) }
        exifInputStream?.close()
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        ) ?: ExifInterface.ORIENTATION_NORMAL

        // 회전 각도 구하기
        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        return if (rotationDegrees != 0 && bitmap != null) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }


    fun processImageForOcrPipeline(
        originalBitmap: Bitmap,
        roi: Rect,
        ): Bitmap {
        val cropped = cropImage(originalBitmap, roi)

//        val adjusted = adjustBrightnessContrastSaturation(
//            cropped,
//            brightness = 1.1f,
//            contrast = 1.3f,
//            saturation = 0.0f
//        )
        val sharpened = sharpenSimple(cropped)

        val grayscale = convertToGrayscale(sharpened)

        return grayscale
    }

    fun processImageForOcrPipelineTrans(
        originalBitmap: Bitmap,
        roi: Rect,
        maskRects: List<Rect> = emptyList(),     // [추가]
        maskColor: Int = Color.WHITE  ,           // [추가]
    ): Bitmap {

        // 1) [신규] 마스킹 + ROI 크롭 (applyMaskAndCrop)
        val maskedCropped = originalBitmap.applyMaskAndCrop(maskRects, roi, maskColor)


//        val cropped = cropImage(maskedCropped, roi)

//        val adjusted = adjustBrightnessContrastSaturation(
//            cropped,
//            brightness = 1.1f,
//            contrast = 1.3f,
//            saturation = 0.0f
//        )


        val sharpened = sharpenSimple(maskedCropped)

        val grayscale = convertToGrayscale(sharpened)



//        //개선 소스
//        val preForOcr = preprocessForOcr(maskedCropped, clipHistPercent = 1.0)

        return grayscale
//        return preForOcr
    }



    // 텍스트 영역 찾아서 roi return
    fun detectTextRegion(
        bitmap: Bitmap,
        lang: String,
        onResult: (Rect?) -> Unit
    ) {
//        // [1] 시작 시점 타임스탬프 기록
//        val timeStart = System.currentTimeMillis()

        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = getTextRecognizerForLanguage(lang)

        // 최소 허용 너비/높이(px) 설정 (실제 이미지 크기·테스트 기준으로 조정)
        val minWidth = 30
        val minHeight = 10

        recognizer.process(image)
            .addOnSuccessListener { result ->
//                val timeAfterRecognize = System.currentTimeMillis()
                // (1) 너무 작은 블록은 ROI 계산에서 제외
                val blocks = result.textBlocks.filter { block ->
                    block.boundingBox?.let { it.width() >= minWidth && it.height() >= minHeight } ?: false
                }
//                LogUtil.i("detectTextRegion", "[$lang] 텍스트 블록 필터링 완료: 전체=${result.textBlocks.size}, 유효=${blocks.size} (소요: ${timeAfterRecognize - timeStart}ms)")

                if (blocks.isEmpty()) {
//                    LogUtil.i("detectTextRegion", "[$lang] ROI 블록 없음 (최소 조건 미달, 전체=${result.textBlocks.size}) (총 소요: ${System.currentTimeMillis() - timeStart}ms)")
                    onResult(null)
                    return@addOnSuccessListener
                }

                // (2) 남은 블록의 boundingBox만 union하여 ROI 계산
//                val timeBeforeUnion = System.currentTimeMillis()
                val unionRect = blocks.mapNotNull { it.boundingBox }
                    .reduce { acc, rect -> acc.union(rect); acc }
//                val timeAfterUnion = System.currentTimeMillis()

//                LogUtil.i(
//                    "detectTextRegion",
//                    "[$lang] unionRect 계산 완료: (${unionRect.left},${unionRect.top})~(${unionRect.right},${unionRect.bottom}), " +
//                            "유효 블록=${blocks.size}, " +
//                            "소요: recognize=${timeAfterRecognize - timeStart}ms, union=${timeAfterUnion - timeBeforeUnion}ms, 총=${timeAfterUnion - timeStart}ms"
//                )

                onResult(unionRect)
            }
            .addOnFailureListener { e ->
//                LogUtil.e(
//                    "detectTextRegion",
//                    "[$lang] 텍스트 영역 감지 실패: ${e.message} (총 소요: ${System.currentTimeMillis() - timeStart}ms)"
//                )
                onResult(null)
            }
    }

    // 비트맵 리사이즈 함수 예시
    fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val scale = maxDimension.toFloat() / maxOf(width, height)
        return if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
        else bitmap
    }
    //Crop
    fun cropImage(bitmap: Bitmap, cropRect: Rect): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            cropRect.left,
            cropRect.top,
            cropRect.width(),
            cropRect.height()
        )
    }

    //Sharpen
//    fun applySharpenFilter(bitmap: Bitmap): Bitmap {
//        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//        val canvas = Canvas(result)
//        val paint = Paint()
//        val sharpenMatrix = ColorMatrix(
//            floatArrayOf(
//                1.5f, -0.5f, 0f, 0f, 0f,
//                -0.5f, 1.5f, -0.5f, 0f, 0f,
//                0f, -0.5f, 1.5f, 0f, 0f,
//                0f, 0f, 0f, 1f, 0f
//            )
//        )
//        paint.colorFilter = ColorMatrixColorFilter(sharpenMatrix)
//        canvas.drawBitmap(bitmap, 0f, 0f, paint)
//        return result
//    }
    fun sharpenSimple(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val kernel = arrayOf(
            intArrayOf(0, -1, 0),
            intArrayOf(-1, 5, -1),
            intArrayOf(0, -1, 0)
        )

        val offset = arrayOf(-1, 0, 1)
        val newPixels = IntArray(pixels.size)

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0
                var g = 0
                var b = 0

                for (ky in 0..2) {
                    for (kx in 0..2) {
                        val px = x + offset[kx]
                        val py = y + offset[ky]
                        val color = pixels[py * width + px]
                        val weight = kernel[ky][kx]

                        r += ((color shr 16) and 0xFF) * weight
                        g += ((color shr 8) and 0xFF) * weight
                        b += (color and 0xFF) * weight
                    }
                }

                r = r.coerceIn(0, 255)
                g = g.coerceIn(0, 255)
                b = b.coerceIn(0, 255)

                newPixels[y * width + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }

        result.setPixels(newPixels, 0, width, 0, 0, width, height)
        return result
    }


    //그레일 스케일 변환
    fun convertToGrayscale(bitmap: Bitmap): Bitmap {
        val gray = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(gray)
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return gray
    }

    fun resizeBitmapProportionally(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 1. 원본이 이미 충분히 작으면 리사이즈 불필요
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        // 2. 리사이즈 필요시만 비율 계산
        val aspectRatio = width.toFloat() / height.toFloat()
        var newWidth = maxWidth
        var newHeight = (maxWidth / aspectRatio).toInt()

        if (newHeight > maxHeight) {
            newHeight = maxHeight
            newWidth = (maxHeight * aspectRatio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Bitmap을 JPEG 포맷으로 압축하여 ByteArray로 변환
     * - JPEG 압축률(quality)은 0~100 (일반적으로 80~90 권장)
     * - 서버 업로드, 네트워크 전송 등에서 사용
     *
     * @param bitmap 변환할 Bitmap 객체
     * @param quality JPEG 압축률(기본값 85)
     * @return JPEG로 압축된 바이트 배열(ByteArray)
     */
    fun bitmapToJpegByteArray(bitmap: Bitmap, quality: Int = 90): ByteArray {
        val outputStream = ByteArrayOutputStream()
        // JPEG 포맷으로 압축 (압축률은 상황에 맞게 조절)
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Bitmap → data URI (Base64) 변환
     * 이미지 첨부 필요시 사용 (2025년 기준, 서버 규격 통일)
     */
    fun bitmapToDataUri(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val base64 = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        return "data:image/png;base64,$base64"
    }

    /**
     *  이미지 갤러리view로 보기
     */
    fun showImageInSystemGallery(context: Context, imagePath: String) {
        val imageFile = File(imagePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        if (context is Activity) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.gallary)))
        }
    }
    /**
     * ML Kit OCR 결과 블록들을 Y축(줄 단위)로 그룹핑해서 텍스트 라인 리스트로 변환
     */
    /**
     * OCR 블록을 Y좌표 기준 줄 단위로 병합(옵션: minLineLength별 추가 병합)
     * - 병합 시 각 블록별 fixCommonOcrNumberErrors() 후처리 적용
     * - 예외 상황 robust, 불필요 공백/개행 제거
     *
     * @param blocks OCR TextBlock 리스트
     * @param yThreshold Y좌표 기준 줄 병합 임계값 (null이면 한 줄)
     * @param minLineLength 병합 후 줄별 최소 문자수(옵션, null이면 스킵)
     * @param postProcess 블록별 후처리 함수 (default: fixCommonOcrNumberErrors)
     * @return 병합+후처리된 줄 리스트
     */
//    fun groupTextBlocksByLine(
//        blocks: List<Text.TextBlock>,
//        yThreshold: Int? = 30,
//        minLineLength: Int? = null,
//        postProcess: ((String) -> String)? = null
//    ): List<String> {
//        val fixFn: (String) -> String = postProcess ?: ::fixCommonOcrNumberErrors
//        try {
//            if (blocks.isEmpty()) return emptyList()
//
//            // [1] Y좌표 기준 줄 그룹핑
//            val groupedLines: List<List<Text.TextBlock>> = if (yThreshold != null) {
//                val sortedBlocks = try {
//                    blocks.sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
//                } catch (e: Exception) {
//                    return listOf(blocks.joinToString(" ") { fixFn(it.text) })
//                }
//                val lines = mutableListOf<MutableList<Text.TextBlock>>()
//                for (block in sortedBlocks) {
//                    try {
//                        val y = block.boundingBox?.top ?: continue
//                        val added = lines.find { group ->
//                            val baseY = group.first().boundingBox?.top ?: return@find false
//                            kotlin.math.abs(baseY - y) <= yThreshold
//                        }
//                        if (added != null) added.add(block)
//                        else lines.add(mutableListOf(block))
//                    } catch (_: Exception) {
//                        continue
//                    }
//                }
//                lines
//            } else {
//                listOf(blocks.toMutableList())
//            }
//
//            // [2] 줄 병합 + 후처리(숫자/I/O 등 보정)
//            val mergedLines = groupedLines.mapNotNull { lineBlocks ->
//                try {
//                    lineBlocks.sortedBy { it.boundingBox?.left ?: 0 }
//                        .joinToString(" ") { block ->
//                            val fixed = fixFn(block.text.trim())
//                            fixed
//                        }
//                        // ↓ cleanOcrText 로직을 직접 포함
//                        .replace("\\(.*?\\)|\\[.*?\\]|\\{.*?\\}".toRegex(), " ")  // 괄호 및 괄호 안 제거
//                        .replace("[/\\\\]".toRegex(), " ")                        // 슬래시 제거
//                        .replace("[→←↑↓⇨⇦⇧⇩]".toRegex(), " ")                   // 화살표 제거
//                        .replace("[|<>]".toRegex(), " ")                         // |, <, > 제거
//                        .replace("[\\t\\r\\n]+".toRegex(), " ")                  // 탭, 줄바꿈 등 제거
//                        .replace(" {2,}".toRegex(), " ")                         // 중복 공백 정리
//                        .trim()
//                        .takeIf { it.isNotBlank() }
//                } catch (_: Exception) {
//                    null
//                }
//            }
//
//            // [3] (옵션) 문자 수 기준 추가 병합
//            return if (minLineLength != null && minLineLength > 0) {
//                val result = mutableListOf<String>()
//                var buffer = StringBuilder()
//                for (line in mergedLines) {
//                    if (buffer.isNotEmpty()) buffer.append(" ")
//                    buffer.append(line)
//                    if (buffer.length >= minLineLength) {
//                        result.add(buffer.toString().trim())
//                        buffer = StringBuilder()
//                    }
//                }
//                if (buffer.isNotEmpty()) result.add(buffer.toString().trim())
//                result
//            } else {
//                mergedLines
//            }
//        } catch (e: Exception) {
//            // Log.e("groupTextBlocksByLine", "예외 발생: ${e.message}")
//            return emptyList()
//        }
//    }


    /**
     * 해당 텍스트의 주요 언어 스크립트(영,한,일,중 등) 판별
     */
    fun getMainScriptType(text: String): String {
        return when {
            text.any { it in '\uAC00'..'\uD7A3' } -> "ko" // 한글
            text.any { it in '\u3040'..'\u30FF' || it in '\u31F0'..'\u31FF' } -> "ja" // 일본어
            text.any { it in '\u4E00'..'\u9FFF' } -> "zh" // 한자(중국어/일부일본어)
            text.any { it in 'A'..'Z' || it in 'a'..'z' } -> "en" // 영문
            else -> "other"
        }
    }

/*
    fun groupTextBlocksByLineWithLanguageSplit(
        blocks: List<Text.TextBlock>,
        yThreshold: Int? = 30,
        minLineLength: Int? = null,
        postProcess: ((String) -> String)? = null
    ): List<String> {
        val fixFn: (String) -> String = postProcess ?: ::fixCommonOcrNumberErrors
        try {
            if (blocks.isEmpty()) return emptyList()

            // 1. Y좌표 기준 줄 그룹핑
            val groupedLines: List<List<Text.TextBlock>> = if (yThreshold != null) {
                val sortedBlocks = try {
                    blocks.sortedBy { it.boundingBox?.top ?: Int.MAX_VALUE }
                } catch (e: Exception) {
                    return listOf(blocks.joinToString(" ") { fixFn(it.text) })
                }
                val lines = mutableListOf<MutableList<Text.TextBlock>>()
                for (block in sortedBlocks) {
                    val y = block.boundingBox?.top ?: continue
                    val added = lines.find { group ->
                        val baseY = group.first().boundingBox?.top ?: return@find false
                        kotlin.math.abs(baseY - y) <= yThreshold
                    }
                    if (added != null) added.add(block)
                    else lines.add(mutableListOf(block))
                }
                lines
            } else {
                listOf(blocks.toMutableList())
            }

            // 2. 라인 내에서 언어 그룹별로 분리
            val finalMergedLines = mutableListOf<String>()
            for (lineBlocks in groupedLines) {
                if (lineBlocks.isEmpty()) continue
                var currentLang = getMainScriptType(lineBlocks[0].text)
                var buffer = StringBuilder()
                for (block in lineBlocks.sortedBy { it.boundingBox?.left ?: 0 }) {
                    val blockLang = getMainScriptType(block.text)
                    if (buffer.isNotEmpty() && blockLang != currentLang) {
                        finalMergedLines.add(buffer.toString().trim())
                        buffer = StringBuilder()
                        currentLang = blockLang
                    }
                    if (buffer.isNotEmpty()) buffer.append(" ")
                    buffer.append(fixFn(block.text.trim()))
                }
                if (buffer.isNotEmpty()) finalMergedLines.add(buffer.toString().trim())
            }

            // 3. 문자 수 기준 추가 병합 (언어타입이 다르면 절대 합치지 않음!)
            return if (minLineLength != null && minLineLength > 0) {
                val result = mutableListOf<String>()
                var buffer = StringBuilder()
                var bufferLang: String? = null
                for (line in finalMergedLines) {
                    val lineLang = getMainScriptType(line)
                    // 1) 버퍼가 비었으면 시작
                    if (buffer.isEmpty()) {
                        buffer.append(line)
                        bufferLang = lineLang
                    }
                    // 2) 같은 언어 && 길이 한도 내면 합침
                    else if (lineLang == bufferLang && buffer.length + 1 + line.length <= minLineLength) {
                        buffer.append(" ").append(line)
                    }
                    // 3) 다른 언어 or 길이 초과면 방출 후 새로 시작
                    else {
                        result.add(buffer.toString().trim())
                        buffer = StringBuilder(line)
                        bufferLang = lineLang
                    }
                    // 4) minLineLength 넘으면 즉시 방출
                    if (buffer.length >= minLineLength) {
                        result.add(buffer.toString().trim())
                        buffer = StringBuilder()
                        bufferLang = null
                    }
                }
                if (buffer.isNotEmpty()) result.add(buffer.toString().trim())
                result
            } else {
                finalMergedLines
            }
        } catch (e: Exception) {
            return emptyList()
        }
    }
*/
fun groupTextBlocksFirstByLanguageThenByLine(
    blocks: List<Text.TextBlock>,
    yThreshold: Int? = 30,
    minLineLength: Int? = null,
    postProcess: ((String) -> String)? = null
): List<String> {
    // [MOD-0] 안전한 후처리 함수 선택
    val fixFn: (String) -> String = postProcess ?: ::fixCommonOcrNumberErrors

    try {
        if (blocks.isEmpty()) return emptyList()

        // [MOD-1] 라인 높이 기반 동적 임계값 계산 (centerY 기준으로 묶을 때 사용)
        // - yThreshold가 null이면 블록 높이 중앙값의 0.6배로 사용
        val heights = blocks.mapNotNull { it.boundingBox?.height() }.sorted()
        val medianH = if (heights.isNotEmpty()) heights[heights.size / 2] else 30
        val dynTol = ((medianH * 0.6f).toInt()).coerceAtLeast(12)
        val tol = yThreshold ?: dynTol

        // [기존] 언어별로 먼저 분리
        val langBlockMap = blocks.groupBy { getMainScriptType(it.text) }

        val allRows = mutableListOf<List<Text.TextBlock>>()

        for ((_, blockList) in langBlockMap) {
            // [MOD-2] Y좌표 정렬 기준을 top가 아니라 centerY로 변경
            val sortedBlocks = blockList.sortedBy { it.boundingBox?.centerY() ?: Int.MAX_VALUE }

            // [기존+보정] centerY 가까운 것끼리 행 그룹핑
            val lines = mutableListOf<MutableList<Text.TextBlock>>()
            for (block in sortedBlocks) {
                val box = block.boundingBox ?: continue
                val cy = box.centerY()
                // 가장 최근 행과 비교(선형 탐색) → 퍼포먼스 문제 없으면 유지
                val hit = lines.indexOfLast { group ->
                    val base = group.first().boundingBox
                    base != null && kotlin.math.abs((base.centerY()) - cy) <= tol
                }
                if (hit >= 0) {
                    lines[hit].add(block)
                } else {
                    lines.add(mutableListOf(block))
                }
            }

            // [MOD-3] 각 행 내부를 X(left)로 정렬
            lines.forEach { row ->
                row.sortBy { it.boundingBox?.left ?: Int.MAX_VALUE }
            }

            // 언어 그룹 결과를 allRows에 합산
            allRows.addAll(lines)
        }

        // [MOD-4] 언어 그룹 합친 전체 행을 Y(min centerY)로 최종 정렬
        val allRowsSorted = allRows.sortedBy { row ->
            row.minOfOrNull { it.boundingBox?.centerY() ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
        }

        // ------------------ 표(테이블) 판별 ------------------
        // [MOD-5] “표처럼 보이는 행”의 개수뿐 아니라,
        //         간단한 열 간격 일관성(열 수, 열 left 위치의 분산)도 함께 확인
        val tableLikeRows = allRowsSorted.filter { it.size >= 2 }
        val isTable = if (tableLikeRows.size >= 2) {
            // 열 인덱스를 맞춰보고, 각 열의 X(left) 위치 분산이 너무 크지 않으면 표로 인정
            val maxCols = tableLikeRows.maxOf { it.size }
            // 열별 좌표 리스트
            val colXs = Array(maxCols) { mutableListOf<Int>() }
            for (row in tableLikeRows) {
                row.forEachIndexed { idx, tb ->
                    val x = tb.boundingBox?.left
                    if (x != null) colXs[idx].add(x)
                }
            }
            // 아주 간단한 분산 체크(표면 열 정렬이 대략 맞으면 분산/평균 비율이 낮게 나옴)
            val okCols = colXs.count { xs ->
                if (xs.size < 2) false
                else {
                    val mean = xs.average()
                    val variance = xs.fold(0.0) { acc, v -> acc + (v - mean) * (v - mean) } / xs.size
                    val cv = if (mean != 0.0) kotlin.math.sqrt(variance) / mean else 1.0
                    cv < 0.25 // 임계치: 필요시 0.2~0.35에서 조정
                }
            }
            okCols >= 1 // 한 열 이상 정렬이 맞으면 표로 인정(보수적으로 유지)
        } else false

        // ------------------ 결과 빌드 ------------------
        if (isTable) {
            // 표: 각 행을 탭(\t)으로 합침
            return allRowsSorted.filter { it.isNotEmpty() }.map { lineBlocks ->
                lineBlocks.joinToString("\t") { fixFn(it.text.trim()) }
                    // [MOD-6] 과격한 기호 제거 축소: 탭/개행/중복 공백만 정리
                    .replace("[\\t\\r\\n]+".toRegex(), " ")
                    .replace(" {2,}".toRegex(), " ")
                    .trim()
            }.filter { it.isNotBlank() }
        } else {
            // 라인별 병합
            val lineListWithY = mutableListOf<Pair<Int, String>>()
            for (lineBlocks in allRowsSorted) {
                if (lineBlocks.isEmpty()) continue
                val minY = lineBlocks.minOfOrNull { it.boundingBox?.centerY() ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
                val merged = lineBlocks.joinToString(" ") { fixFn(it.text.trim()) }
                    // [MOD-6] 동일(탭/개행/중복 공백 정리만)
                    .replace("[\\t\\r\\n]+".toRegex(), " ")
                    .replace(" {2,}".toRegex(), " ")
                    .trim()
                if (merged.isNotBlank()) lineListWithY.add(minY to merged)
            }

            val sortedLines = lineListWithY.sortedBy { it.first }.map { it.second }

            // (옵션) 문자 수 기준 추가 병합
            return if (minLineLength != null && minLineLength > 0) {
                val result = mutableListOf<String>()
                var buffer = StringBuilder()
                var bufferLang: String? = null

                fun flushBuffer() {
                    if (buffer.isNotEmpty()) {
                        result.add(buffer.toString().trim())
                        buffer = StringBuilder()
                        bufferLang = null
                    }
                }

                for (line in sortedLines) {
                    val lineLang = getMainScriptType(line)
                    if (buffer.isEmpty()) {
                        buffer.append(line)
                        bufferLang = lineLang
                    } else if (lineLang == bufferLang && (buffer.length + 1 + line.length) <= minLineLength) {
                        buffer.append(" ").append(line)
                    } else {
                        flushBuffer()
                        buffer.append(line)
                        bufferLang = lineLang
                    }

                    // 목표 길이에 도달했으면 즉시 플러시
                    if (buffer.length >= minLineLength) flushBuffer()
                }
                flushBuffer()
                result
            } else {
                sortedLines
            }
        }
    } catch (e: Exception) {
        return emptyList()
    }
}

//    fun groupTextBlocksFirstByLanguageThenByLine(
//        blocks: List<Text.TextBlock>,
//        yThreshold: Int? = 30,
//        minLineLength: Int? = null,
//        postProcess: ((String) -> String)? = null
//    ): List<String> {
//        // [MOD-0] 안전한 후처리 함수 선택
//        val fixFn: (String) -> String = postProcess ?: ::fixCommonOcrNumberErrors
//
//        try {
//            if (blocks.isEmpty()) return emptyList()
//
//            // [MOD-1] 라인 높이 기반 동적 임계값 계산 (centerY 기준으로 묶을 때 사용)
//            // - yThreshold가 null이면 블록 높이 중앙값의 0.6배로 사용
//            val heights = blocks.mapNotNull { it.boundingBox?.height() }.sorted()
//            val medianH = if (heights.isNotEmpty()) heights[heights.size / 2] else 30
//            val dynTol = ((medianH * 0.6f).toInt()).coerceAtLeast(12)
//            val tol = yThreshold ?: dynTol
//
//            // [기존] 언어별로 먼저 분리
//            val langBlockMap = blocks.groupBy { getMainScriptType(it.text) }
//
//            val allRows = mutableListOf<List<Text.TextBlock>>()
//
//            for ((_, blockList) in langBlockMap) {
//                // [MOD-2] Y좌표 정렬 기준을 top가 아니라 centerY로 변경
//                val sortedBlocks = blockList.sortedBy { it.boundingBox?.centerY() ?: Int.MAX_VALUE }
//
//                // [기존+보정] centerY 가까운 것끼리 행 그룹핑
//                val lines = mutableListOf<MutableList<Text.TextBlock>>()
//                for (block in sortedBlocks) {
//                    val box = block.boundingBox ?: continue
//                    val cy = box.centerY()
//                    // 가장 최근 행과 비교(선형 탐색) → 퍼포먼스 문제 없으면 유지
//                    val hit = lines.indexOfLast { group ->
//                        val base = group.first().boundingBox
//                        base != null && kotlin.math.abs((base.centerY()) - cy) <= tol
//                    }
//                    if (hit >= 0) {
//                        lines[hit].add(block)
//                    } else {
//                        lines.add(mutableListOf(block))
//                    }
//                }
//
//                // [MOD-3] 각 행 내부를 X(left)로 정렬
//                lines.forEach { row ->
//                    row.sortBy { it.boundingBox?.left ?: Int.MAX_VALUE }
//                }
//
//                // 언어 그룹 결과를 allRows에 합산
//                allRows.addAll(lines)
//            }
//
//            // [MOD-4] 언어 그룹 합친 전체 행을 Y(min centerY)로 최종 정렬
//            val allRowsSorted = allRows.sortedBy { row ->
//                row.minOfOrNull { it.boundingBox?.centerY() ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
//            }
//
//            // ------------------ 표(테이블) 판별 ------------------
//            // [MOD-5] “표처럼 보이는 행”의 개수뿐 아니라,
//            //         간단한 열 간격 일관성(열 수, 열 left 위치의 분산)도 함께 확인
//            val tableLikeRows = allRowsSorted.filter { it.size >= 2 }
//            val isTable = if (tableLikeRows.size >= 2) {
//                // 열 인덱스를 맞춰보고, 각 열의 X(left) 위치 분산이 너무 크지 않으면 표로 인정
//                val maxCols = tableLikeRows.maxOf { it.size }
//                // 열별 좌표 리스트
//                val colXs = Array(maxCols) { mutableListOf<Int>() }
//                for (row in tableLikeRows) {
//                    row.forEachIndexed { idx, tb ->
//                        val x = tb.boundingBox?.left
//                        if (x != null) colXs[idx].add(x)
//                    }
//                }
//                // 아주 간단한 분산 체크(표면 열 정렬이 대략 맞으면 분산/평균 비율이 낮게 나옴)
//                val okCols = colXs.count { xs ->
//                    if (xs.size < 2) false
//                    else {
//                        val mean = xs.average()
//                        val variance = xs.fold(0.0) { acc, v -> acc + (v - mean) * (v - mean) } / xs.size
//                        val cv = if (mean != 0.0) kotlin.math.sqrt(variance) / mean else 1.0
//                        cv < 0.25 // 임계치: 필요시 0.2~0.35에서 조정
//                    }
//                }
//                okCols >= 1 // 한 열 이상 정렬이 맞으면 표로 인정(보수적으로 유지)
//            } else false
//
//            // ------------------ 결과 빌드 ------------------
//            if (isTable) {
//                // 표: 각 행을 탭(\t)으로 합침
//                return allRowsSorted.filter { it.isNotEmpty() }.map { lineBlocks ->
//                    lineBlocks.joinToString("\t") { fixFn(it.text.trim()) }
//                        // [MOD-6] 과격한 기호 제거 축소: 탭/개행/중복 공백만 정리
//                        .replace("[\\t\\r\\n]+".toRegex(), " ")
//                        .replace(" {2,}".toRegex(), " ")
//                        .trim()
//                }.filter { it.isNotBlank() }
//            } else {
//                // 라인별 병합
//                val lineListWithY = mutableListOf<Pair<Int, String>>()
//                for (lineBlocks in allRowsSorted) {
//                    if (lineBlocks.isEmpty()) continue
//                    val minY = lineBlocks.minOfOrNull { it.boundingBox?.centerY() ?: Int.MAX_VALUE } ?: Int.MAX_VALUE
//                    val merged = lineBlocks.joinToString(" ") { fixFn(it.text.trim()) }
//                        // [MOD-6] 동일(탭/개행/중복 공백 정리만)
//                        .replace("[\\t\\r\\n]+".toRegex(), " ")
//                        .replace(" {2,}".toRegex(), " ")
//                        .trim()
//                    if (merged.isNotBlank()) lineListWithY.add(minY to merged)
//                }
//
//                val sortedLines = lineListWithY.sortedBy { it.first }.map { it.second }
//
//                // (옵션) 문자 수 기준 추가 병합
//                return if (minLineLength != null && minLineLength > 0) {
//                    val result = mutableListOf<String>()
//                    var buffer = StringBuilder()
//                    var bufferLang: String? = null
//
//                    fun flushBuffer() {
//                        if (buffer.isNotEmpty()) {
//                            result.add(buffer.toString().trim())
//                            buffer = StringBuilder()
//                            bufferLang = null
//                        }
//                    }
//
//                    for (line in sortedLines) {
//                        val lineLang = getMainScriptType(line)
//                        if (buffer.isEmpty()) {
//                            buffer.append(line)
//                            bufferLang = lineLang
//                        } else if (lineLang == bufferLang && (buffer.length + 1 + line.length) <= minLineLength) {
//                            buffer.append(" ").append(line)
//                        } else {
//                            flushBuffer()
//                            buffer.append(line)
//                            bufferLang = lineLang
//                        }
//
//                        // 목표 길이에 도달했으면 즉시 플러시
//                        if (buffer.length >= minLineLength) flushBuffer()
//                    }
//                    flushBuffer()
//                    result
//                } else {
//                    sortedLines
//                }
//            }
//        } catch (e: Exception) {
//            return emptyList()
//        }
//    }
    /** CameraX ImageProxy → Bitmap (회전 보정 포함) */
    fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        return when (image.format) {
            android.graphics.ImageFormat.YUV_420_888 -> {
                // YUV 포맷 (보통 3개)
                val planes = image.planes
                if (planes.size == 3) {
                    val yBuffer = planes[0].buffer
                    val uBuffer = planes[1].buffer
                    val vBuffer = planes[2].buffer

                    val ySize = yBuffer.remaining()
                    val uSize = uBuffer.remaining()
                    val vSize = vBuffer.remaining()
                    val nv21 = ByteArray(ySize + uSize + vSize)

                    yBuffer.get(nv21, 0, ySize)
                    vBuffer.get(nv21, ySize, vSize)
                    uBuffer.get(nv21, ySize + vSize, uSize)

                    val yuvImage = android.graphics.YuvImage(
                        nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null
                    )
                    val out = java.io.ByteArrayOutputStream()
                    yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
                    val jpegBytes = out.toByteArray()
                    var bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)

                    // rotationDegrees 적용
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    if (rotationDegrees != 0 && bitmap != null) {
                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    bitmap
                } else {
                    // 비정상(1개 등) → 아래 JPEG과 동일 처리
                    val buffer = planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    // rotationDegrees 적용
                    val rotationDegrees = image.imageInfo.rotationDegrees
                    if (rotationDegrees != 0 && bitmap != null) {
                        val matrix = Matrix()
                        matrix.postRotate(rotationDegrees.toFloat())
                        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    bitmap
                }
            }
            android.graphics.ImageFormat.JPEG -> {
                // JPEG 포맷은 planes 1개만 존재, 그냥 byte 추출!
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                // rotationDegrees 적용
                val rotationDegrees = image.imageInfo.rotationDegrees
                if (rotationDegrees != 0 && bitmap != null) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDegrees.toFloat())
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                bitmap
            }
            else -> {
                // 예외(알 수 없는 포맷): 그냥 planes[0]로 해본다
                val planes = image.planes
                val buffer = planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                // rotationDegrees 적용
                val rotationDegrees = image.imageInfo.rotationDegrees
                if (rotationDegrees != 0 && bitmap != null) {
                    val matrix = Matrix()
                    matrix.postRotate(rotationDegrees.toFloat())
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
                bitmap
            }
        }
    }
    /**
     *    OCR 글자인식 이슈로 테스트중인 소스들
     *
     */

    /**
     * OCR 친화 전처리
     * 1) 휘도 기반 그레이스케일(Rec.709)
     * 2) 자동대비(히스토그램 클리핑 스트레치, 기본 1%)
     * 3) Otsu 이진화(배경/글자 분리)
     *
     * Note: 결과는 흑/백 2값 이미지(ARGB_8888).
     */
    fun preprocessForOcr(
        src: Bitmap,
        clipHistPercent: Double = 1.0   // 0.5~2.0 추천
    ): Bitmap {
        val gray = toGrayLuma709(src)
//        val contrasted = autoContrast(gray, clipHistPercent)
//        return otsuBinarize(gray)
        return return otsuBinarize(gray)
    }

    /** Rec.709 휘도 기반 그레이스케일 (채도 0f보다 텍스트 대비 유지에 유리) */
    fun toGrayLuma709(src: Bitmap): Bitmap {
        val w = src.width; val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Rec.709 luma: Y = 0.2126R + 0.7152G + 0.0722B
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val y = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (y shl 16) or (y shl 8) or y
        }
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 히스토그램 클리핑 자동대비: 양끝(clip%)을 잘라 min/max 재매핑
     * - 저조도/강한 그림자에서 문자 대비를 확 올려줌
     */
    fun autoContrast(gray: Bitmap, clipHistPercent: Double = 1.0): Bitmap {
        require(gray.config == Bitmap.Config.ARGB_8888) { "expect ARGB_8888 gray" }
        val w = gray.width; val h = gray.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h)
        gray.getPixels(px, 0, w, 0, 0, w, h)

        // 1) 히스토그램
        val hist = IntArray(256)
        for (p in px) hist[p and 0xFF]++

        // 2) 클리핑 임계 계산
        val total = w * h
        val clipAmt = (total * (clipHistPercent / 100.0)).toInt()
        var low = 0; var high = 255
        var acc = 0
        while (low < 255) { acc += hist[low]; if (acc > clipAmt) break; low++ }
        acc = 0
        while (high > 0) { acc += hist[high]; if (acc > clipAmt) break; high-- }
        if (low >= high) {
            // 대비 스트레치 불가 → 원본 반환
            return gray
        }

        val scale = 255.0 / (high - low)
        // 3) 리매핑
        for (i in px.indices) {
            val y = px[i] and 0xFF
            val v = ((y - low) * scale).coerceIn(0.0, 255.0).toInt()
            px[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }

    /** Otsu 임계로 2값화(글자/배경 분리). 조명 들쭉날쭉 환경에서 효과적 */
    fun otsuBinarize(gray: Bitmap): Bitmap {
        require(gray.config == Bitmap.Config.ARGB_8888) { "expect ARGB_8888 gray" }
        val w = gray.width; val h = gray.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h)
        gray.getPixels(px, 0, w, 0, 0, w, h)

        val hist = IntArray(256)
        for (p in px) hist[p and 0xFF]++

        // Otsu threshold 계산
        val total = w * h
        var sum = 0L
        for (i in 0..255) sum += i * hist[i]
        var sumB = 0L
        var wB = 0L
        var wF: Long
        var maxVar = -1.0
        var thresh = 127
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0L) continue
            wF = total - wB
            if (wF == 0L) break
            sumB += t * hist[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val between = wB.toDouble() * wF.toDouble() * (mB - mF) * (mB - mF)
            if (between > maxVar) {
                maxVar = between
                thresh = t
            }
        }

        // 이진화(검정/흰색)
        for (i in px.indices) {
            val y = px[i] and 0xFF
            val v = if (y > thresh) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            px[i] = v
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }
}