package com.waveapp.tourcat.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.waveapp.tourcat.common.ComConstant
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 *  이미지 경로, 이미지, 생성 삭제 구하는 모듈
 */
object FileUtil {

    // 사진갤러리(공용저장소) 경로 구하기 - 앱내 촬영한 사진 원본저장용
    fun getGalleryFolderPath(folderName: String): String {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    // 앱 내부저장소 경로 구하기 - 앱내 히스토리 관리가 필요한 이미지 저장용(저해상도 사진 -> DB 저장Link)
    fun getAppInternalFolderPath(context: Context, folderName: String): String {
        val dir = File(context.filesDir, folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    // 파일명 생성 함수 (prefix, 확장자 등 구분값 넣기)
    fun generateFileName(type: String, extension: String = "jpg"): String {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss_SSS", Locale.getDefault()).format(Date())
        val uuid = UUID.randomUUID().toString().take(8)
        return "${type}_${timeStamp}_$uuid.$extension"
    }

    // 회전된 비트맵에서 썸네일 생성 함수 예시
    fun createThumbnailFromBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // 리사이즈 비율 계산
        val ratio = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }


    // 사진 촬영용 갤러리 Uri 생성 (카메라 Intent용)
    fun createGalleryImageUri(context: Context, folderName: String, fileName: String): Uri? {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
//                    Environment.DIRECTORY_PICTURES + File.separator + folderName
                    folderName
                )
                put(MediaStore.Images.Media.IS_PENDING, 0)
            } else {
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath
                put(
                    MediaStore.Images.Media.DATA,
                    "$pictures/$folderName/$fileName"
                )
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        return uri
    }

    // (선택) 저장 완료 후 IS_PENDING=0 처리 (Android Q 이상)
//    fun completePendingImage(context: Context, uri: Uri) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            val contentValues = ContentValues().apply {
//                put(MediaStore.Images.Media.IS_PENDING, 0)
//            }
//            context.contentResolver.update(uri, contentValues, null, null)
//        }
//    }

    /**
     * 내부저장소의 특정 폴더 내 파일 삭제
     * @param context Context
     * @param folderName 폴더명
     * @param fileName 삭제할 파일명
     * @return 성공여부(Boolean)
     */
    fun deleteInternalFile(context: Context, folderName: String, fileName: String): Boolean {
        val targetFile  = File(folderName + File.separator + fileName)
        return targetFile.exists() && targetFile.delete()
    }

//    fun createThumbnail(
//        srcBitmap: Bitmap,
//        thumbWidth: Int = 200,
//        thumbHeight: Int = 200
//    ): Bitmap {
//        return Bitmap.createScaledBitmap(
//            srcBitmap,
//            thumbWidth,
//            thumbHeight,
//            true
//        )
//    }

    /**
     * 이미지 Uri로 썸네일 Bitmap 생성
     * @param context Context
     * @param imageUri 이미지 Uri
     * @param thumbWidth 썸네일 너비
     * @param thumbHeight 썸네일 높이
     * @return Bitmap (썸네일) or null
     */
    fun createThumbnailFromUriWithRotation(
        context: Context,
        imageUri: Uri,
        thumbWidth: Int = 200,
        thumbHeight: Int = 200
    ): Bitmap? {
        return try {
            // Step 1: Open InputStream and get image dimensions
            val inputStream = context.contentResolver.openInputStream(imageUri)
                ?: return null // Return null if input stream cannot be opened

            // Step 2: Get the image dimensions to calculate the sample size
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            // Step 3: Calculate the sample size based on desired thumbnail size
            var sampleSize = 1
            val (srcWidth, srcHeight) = options.outWidth to options.outHeight
            if (srcHeight > thumbHeight || srcWidth > thumbWidth) {
                val heightRatio = srcHeight.toFloat() / thumbHeight
                val widthRatio = srcWidth.toFloat() / thumbWidth
                sampleSize = Math.round(if (heightRatio > widthRatio) heightRatio else widthRatio)
            }

            // Step 4: Decode the image with the sample size to create the thumbnail
            val inputStream2 = context.contentResolver.openInputStream(imageUri)
                ?: return null // Return null if input stream cannot be opened again
            val thumbOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeStream(inputStream2, null, thumbOptions)
            inputStream2.close()

            // Step 5: Read EXIF data to get orientation (rotation) information
            val exifInputStream = context.contentResolver.openInputStream(imageUri)
                ?: return bitmap // Return the unrotated bitmap if EXIF cannot be read
            val exif = ExifInterface(exifInputStream)
            exifInputStream.close()

            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            // Step 6: Get the rotation degree based on EXIF orientation
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }

            // Step 7: Rotate the image if necessary
            if (rotationDegrees != 0 && bitmap != null) {
                val matrix = Matrix()
                matrix.postRotate(rotationDegrees.toFloat())
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            // Handle any unexpected error
            null
        }
    }

    /**
     * 카메라+갤러리 통합 이미지 선택 Intent 생성
     * @param outputUri 카메라 촬영시 저장할 Uri (필수)
     * @return Intent
     */
    fun createImageChooserIntent(outputUri: Uri): Intent {
        // 카메라 인텐트(촬영 → outputUri로 저장)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
        }
        // 갤러리 인텐트(사진 선택)
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryIntent.type = "image/*"

        // 선택 다이얼로그
        val chooser = Intent.createChooser(galleryIntent, "이미지 선택")
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(cameraIntent))
        return chooser
    }
    /**
     * Bitmap 썸네일을 내부저장소 파일로 저장
     * @param context Context
     * @param thumbBitmap 썸네일 Bitmap
     * @param folderName 폴더명
     * @param fileName 저장 파일명
     * @return 저장된 썸네일 파일의 File 객체 또는 null
     */
    fun saveThumbnailToInternal(
        context: Context,
        thumbBitmap: Bitmap,
        foldetPath: String,
        fileName: String
    ): File? {
        val dir = File(foldetPath)
        if (!dir.exists()) dir.mkdirs()
        val thumbFile = File(dir, fileName)
        return try {
            FileOutputStream(thumbFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            thumbFile
        } catch (e: Exception) {
            null
        }
    }
}

/* ex
val galleryPath = FilePathUtil.getGalleryFolderPath("MyGallery")
val internalPath = FilePathUtil.getAppInternalFolderPath(context, "MyAppFiles")
val fileName = FilePathUtil.generateFileName(type = "PHOTO", extension = "jpg")

-----------
val fileName = FilePathUtil.generateFileName("PHOTO", "jpg")
val imageUri = FilePathUtil.createGalleryImageUri(context, "MyCameraFolder", fileName)

// 카메라 인텐트에 전달
val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
startActivityForResult(intent, REQUEST_CODE_CAMERA)
-------------

val deleted = FilePathUtil.deleteInternalFile(context, "MyAppFiles", "PHOTO_20250704_221016.jpg")
if (deleted) {
    // 삭제 성공
} else {
    // 삭제 실패 (파일 없음, 권한 등)
}
 */