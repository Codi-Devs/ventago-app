package com.teco.ventago.core.camera

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberGalleryManager(onResult: (SharedImage?) -> Unit): GalleryManager {
    val contentResolver: ContentResolver = LocalContext.current.contentResolver
    val galleryLauncher =
        rememberLauncherForActivityResult(PickImageFromGallery()) { uri ->
            uri?.let {
                onResult.invoke(SharedImage(BitmapUtils.getBitmapFromUri(uri, contentResolver)))
            }
        }
    return remember(galleryLauncher) {
        GalleryManager(onLaunch = {
            galleryLauncher.launch(Unit)
        })
    }
}

private class PickImageFromGallery : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) return null
        return intent?.data
    }
}

actual class GalleryManager actual constructor(private val onLaunch: () -> Unit) {
    actual fun launch() {
        onLaunch()
    }
}
