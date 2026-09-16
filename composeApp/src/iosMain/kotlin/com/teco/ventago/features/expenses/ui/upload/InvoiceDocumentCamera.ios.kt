package com.teco.ventago.features.expenses.ui.upload

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.teco.ventago.utils.getRootViewController
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerCameraCaptureMode
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun InvoiceDocumentCamera(
    onCaptured: (ByteArray) -> Unit,
    onClose: () -> Unit
) {
    val imagePicker = remember { UIImagePickerController() }
    val cameraDelegate = remember {
        object : NSObject(), UIImagePickerControllerDelegateProtocol,
            UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                val imageData = image?.let { UIImageJPEGRepresentation(it, 1.0) }
                val bytes = imageData?.bytes
                val length = imageData?.length?.toInt() ?: 0
                val captured = if (bytes != null && length > 0) {
                    val data: CPointer<ByteVar> = bytes.reinterpret()
                    ByteArray(length) { index -> data[index] }
                } else {
                    null
                }
                picker.dismissViewControllerAnimated(true, null)
                if (captured != null) {
                    onCaptured(captured)
                } else {
                    onClose()
                }
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                onClose()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!UIImagePickerController.isSourceTypeAvailable(
                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
            )
        ) {
            onClose()
            return@LaunchedEffect
        }
        imagePicker.setSourceType(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
        imagePicker.setAllowsEditing(false)
        imagePicker.setCameraCaptureMode(
            UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto
        )
        imagePicker.setDelegate(cameraDelegate)
        getRootViewController()?.presentViewController(imagePicker, true, null) ?: onClose()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    )
}
