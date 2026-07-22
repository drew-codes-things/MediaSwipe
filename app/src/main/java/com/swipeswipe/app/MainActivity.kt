package com.swipeswipe.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.swipeswipe.app.data.AndroidPhotoDeleter
import com.swipeswipe.app.data.AndroidPhotoFavoriter
import com.swipeswipe.app.data.MediaStorePhotoRepository
import com.swipeswipe.app.data.SharedPreferencesPendingDeletionStore
import com.swipeswipe.app.data.SharedPreferencesSortTracker
import com.swipeswipe.app.ui.PhotoCleanerViewModel
import com.swipeswipe.app.ui.theme.SwipeSwipeTheme

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (hasPhotoPermission()) {
            viewModel.onPermissionGranted()
        }

        setContent {
            SwipeSwipeTheme {
                SwipeSwipeApp(
                    viewModel = viewModel,
                    onRequestPermission = { permissionLauncher.launch(requiredPhotoPermissions()) },
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

    private fun hasPhotoPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
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
