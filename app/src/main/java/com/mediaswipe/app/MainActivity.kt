package com.mediaswipe.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaswipe.app.data.AndroidPhotoDeleter
import com.mediaswipe.app.data.AndroidPhotoFavoriter
import com.mediaswipe.app.data.MediaStorePhotoRepository
import com.mediaswipe.app.data.PickFromPhotoSource
import com.mediaswipe.app.data.SharedPreferencesPendingDeletionStore
import com.mediaswipe.app.data.SharedPreferencesSortTracker
import com.mediaswipe.app.data.installedPhotoSource
import com.mediaswipe.app.ui.PhotoCleanerViewModel
import com.mediaswipe.app.ui.theme.MediaSwipeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PhotoCleanerViewModel by viewModels {
        viewModelFactory {
            initializer {
                PhotoCleanerViewModel(
                    photoRepository = MediaStorePhotoRepository(applicationContext),
                    photoDeleter = AndroidPhotoDeleter(applicationContext),
                    sortTracker = SharedPreferencesSortTracker(applicationContext),
                    photoFavoriter = AndroidPhotoFavoriter(applicationContext),
                    pendingDeletionStore = SharedPreferencesPendingDeletionStore(applicationContext),
                )
            }
        }
    }

    private val deleteConfirmationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDeleteConfirmationResult(result.resultCode == RESULT_OK)
    }

    private val favoriteConfirmationLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onFavoriteConfirmationResult(result.resultCode == RESULT_OK)
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) {
            viewModel.onPermissionGranted()
        } else {
            viewModel.onPermissionDenied()
        }
    }

    private val photoSourceLauncher = registerForActivityResult(PickFromPhotoSource()) { uris ->
        viewModel.onPhotosPicked(uris)
    }

    private val systemPickerLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        viewModel.onPhotosPicked(uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPhotoPermission()) {
            viewModel.onPermissionGranted()
        }

        setContent {
            MediaSwipeTheme {
                MediaSwipeApp(
                    viewModel = viewModel,
                    onRequestPermission = { permissionLauncher.launch(requiredPhotoPermissions()) },
                    onOpenAppSettings = {
                        startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                        )
                    },
                    onPickPhotos = ::pickPhotos,
                    onLaunchDeleteConfirmation = { intentSender ->
                        deleteConfirmationLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    },
                    onLaunchFavoriteConfirmation = { intentSender ->
                        favoriteConfirmationLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    },
                    onShare = { photo ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (photo.isVideo) "video/*" else "image/*"
                            putExtra(Intent.EXTRA_STREAM, photo.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, null))
                    },
                )
            }
        }
    }

    private fun pickPhotos() {
        val source = installedPhotoSource(packageManager).takeIf { hasPhotoPermission() }
        if (source != null) {
            photoSourceLauncher.launch(source)
        } else {
            systemPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
        }
    }

    private fun granted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun hasPhotoPermission(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> true
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            granted(Manifest.permission.READ_MEDIA_IMAGES) && granted(Manifest.permission.READ_MEDIA_VIDEO)
        else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun requiredPhotoPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}
