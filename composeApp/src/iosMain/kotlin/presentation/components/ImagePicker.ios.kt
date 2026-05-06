package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
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
import platform.UniformTypeIdentifiers.UTTypeImage
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

@Composable
actual fun rememberMultipleImagePicker(onImagesPicked: (List<String>) -> Unit): MultipleImagePickerLauncher {
    val delegate = remember {
        IOSMultipleImagePickerDelegate(onImagesPicked)
    }

    return remember {
        object : MultipleImagePickerLauncher {
            override fun launch() {
                val config = PHPickerConfiguration().apply {
                    selectionLimit = 0 // 0 means unlimited
                }
                val picker = PHPickerViewController(config).apply {
                    setDelegate(delegate)
                }
                getTopViewController()?.presentViewController(picker, animated = true, completion = null)
            }
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun saveImageToFile(image: UIImage): String? {
    val fileName = "picked_${NSUUID().UUIDString()}.jpg"
    val filePath = NSHomeDirectory() + "/Documents/$fileName"
    
    val documentsDir = NSHomeDirectory() + "/Documents"
    NSFileManager.defaultManager().createDirectoryAtPath(
        documentsDir,
        withIntermediateDirectories = true,
        attributes = null,
        error = null
    )

    val imageData: NSData? = UIImageJPEGRepresentation(image, 0.9)
    return if (imageData != null && imageData.writeToFile(filePath, atomically = true)) {
        filePath
    } else {
        null
    }
}

// Single picker delegate using UIImagePickerController
private class IOSImagePickerDelegate(
    private val onImagePicked: (String?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
            ?: didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage

        if (image != null) {
            onImagePicked(saveImageToFile(image))
        } else {
            onImagePicked(null)
        }
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImagePicked(null)
    }
}

// Multiple picker delegate using PHPickerViewController
private class IOSMultipleImagePickerDelegate(
    private val onImagesPicked: (List<String>) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        
        if (didFinishPicking.isEmpty()) {
            onImagesPicked(emptyList())
            return
        }

        val paths = mutableListOf<String>()
        val group = platform.darwin.dispatch_group_create()
        
        for (result in didFinishPicking) {
            val pickerResult = result as? PHPickerResult ?: continue
            val itemProvider = pickerResult.itemProvider
            
            if (itemProvider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
                platform.darwin.dispatch_group_enter(group)
                
                itemProvider.loadObjectOfClass(
                    UIImage.`class`(),
                    completionHandler = { image, error ->
                        if (error == null && image is UIImage) {
                            saveImageToFile(image)?.let { path ->
                                paths.add(path)
                            }
                        }
                        platform.darwin.dispatch_group_leave(group)
                    }
                )
            }
        }
        
        platform.darwin.dispatch_group_notify(group, platform.darwin.dispatch_get_main_queue()) {
            onImagesPicked(paths.toList())
        }
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
