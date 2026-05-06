package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject

@Composable
actual fun rememberImagePicker(onImagePicked: (String?) -> Unit): ImagePickerLauncher {
    val delegate = remember {
        IOSImagePickerDelegate(onImagePicked)
    }

    return remember {
        object : ImagePickerLauncher {
            override fun launch() {
                val picker = UIImagePickerController().apply {
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                    setDelegate(delegate)
                    allowsEditing = false
                }
                getTopViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

private class IOSImagePickerDelegate(
    private val onImagePicked: (String?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage

        if (image != null) {
            val fileName = "picked_${NSUUID().UUIDString()}.jpg"
            val filePath = NSHomeDirectory() + "/Documents/$fileName"
            
            // Ensure Documents directory exists
            val documentsDir = NSHomeDirectory() + "/Documents"
            NSFileManager.defaultManager().createDirectoryAtPath(
                documentsDir,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )

            val imageData: NSData? = UIImageJPEGRepresentation(image, 0.9)
            if (imageData != null && imageData.writeToFile(filePath, atomically = true)) {
                onImagePicked(filePath)
            } else {
                onImagePicked(null)
            }
        } else {
            onImagePicked(null)
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImagePicked(null)
    }
}

private fun getTopViewController(): UIViewController? {
    val keyWindow = UIApplication.sharedApplication.keyWindow
    var topController = keyWindow?.rootViewController
    while (topController?.presentedViewController != null) {
        topController = topController.presentedViewController
    }
    return topController
}
