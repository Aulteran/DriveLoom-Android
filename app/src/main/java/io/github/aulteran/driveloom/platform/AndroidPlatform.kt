package io.github.aulteran.driveloom.platform

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PersistableBundle
import android.provider.OpenableColumns
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.aulteran.driveloom.R
import io.github.aulteran.driveloom.ui.PickedFile
import io.github.aulteran.driveloom.ui.Platform
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * The Android side of [Platform]. Files and photos are reached only through
 * the system pickers, so the app holds no storage or media permission and
 * sees nothing the person did not choose.
 *
 * Must be created in `onCreate`, before the activity starts, because it
 * registers for activity results.
 */
class AndroidPlatform(private val activity: ComponentActivity) : Platform {
    private var pendingImage: CompletableDeferred<Uri?>? = null
    private var pendingOpen: CompletableDeferred<Uri?>? = null
    private var pendingSave: CompletableDeferred<Uri?>? = null

    private val imagePicker = activity.registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pendingImage?.complete(uri)
        pendingImage = null
    }
    private val documentOpener = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pendingOpen?.complete(if (result.resultCode == Activity.RESULT_OK) result.data?.data else null)
        pendingOpen = null
    }
    private val documentSaver = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        pendingSave?.complete(if (result.resultCode == Activity.RESULT_OK) result.data?.data else null)
        pendingSave = null
    }

    override val appName: String = activity.getString(R.string.app_name)

    override val appVersion: String = try {
        activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: ""
    } catch (_: Exception) {
        ""
    }

    override suspend fun pickImage(): PickedFile? {
        val answer = CompletableDeferred<Uri?>()
        pendingImage?.complete(null)
        pendingImage = answer
        try {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: ActivityNotFoundException) {
            pendingImage = null
            throw IOException("No photo picker is available on this device.")
        }
        val uri = answer.await() ?: return null
        val bytes = withContext(Dispatchers.IO) { Images.prepareUpload(activity.contentResolver, uri) }
            ?: throw IOException("That image could not be read, or is too large even after resizing.")
        return PickedFile("photo.jpg", "image/jpeg", bytes)
    }

    override suspend fun pickDocument(mimeTypes: List<String>, maxBytes: Int): PickedFile? {
        val answer = CompletableDeferred<Uri?>()
        pendingOpen?.complete(null)
        pendingOpen = answer
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        }
        try {
            documentOpener.launch(intent)
        } catch (_: ActivityNotFoundException) {
            pendingOpen = null
            throw IOException("No file picker is available on this device.")
        }
        val uri = answer.await() ?: return null
        return withContext(Dispatchers.IO) {
            val resolver = activity.contentResolver
            val queried: String? = try {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst() && !c.isNull(0)) c.getString(0) else null
                }
            } catch (_: Exception) {
                null
            }
            val name = queried ?: "file"
            val read: ByteArray? = try {
                resolver.openInputStream(uri)?.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        if (out.size() > maxBytes) throw IOException("That file is larger than ${maxBytes / (1024 * 1024)} MB.")
                    }
                    out.toByteArray()
                }
            } catch (e: SecurityException) {
                throw IOException("That file could not be opened.", e)
            }
            val bytes = read ?: throw IOException("That file could not be opened.")
            PickedFile(name, resolver.getType(uri).orEmpty(), bytes)
        }
    }

    override suspend fun saveDocument(suggestedName: String, mime: String, bytes: ByteArray): Boolean {
        val answer = CompletableDeferred<Uri?>()
        pendingSave?.complete(null)
        pendingSave = answer
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime
            putExtra(Intent.EXTRA_TITLE, suggestedName)
        }
        try {
            documentSaver.launch(intent)
        } catch (_: ActivityNotFoundException) {
            pendingSave = null
            throw IOException("No file picker is available on this device.")
        }
        val uri = answer.await() ?: return false
        withContext(Dispatchers.IO) {
            try {
                activity.contentResolver.openOutputStream(uri, "wt")?.use { it.write(bytes) } ?: throw IOException("That location could not be written.")
            } catch (e: SecurityException) {
                throw IOException("That location could not be written.", e)
            }
        }
        return true
    }

    override fun decodeImage(bytes: ByteArray): ImageBitmap? = Images.decodeForDisplay(bytes)?.asImageBitmap()

    override fun copyToClipboard(label: String, text: String, sensitive: Boolean) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText(label, text)
        if (sensitive) {
            // Keeps the value out of the clipboard preview on Android 13 and later.
            clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        }
        clipboard.setPrimaryClip(clip)
    }

    private var secureHolds = 0

    override fun setSecureScreen(secure: Boolean) {
        secureHolds = (secureHolds + if (secure) 1 else -1).coerceAtLeast(0)
        if (secureHolds > 0) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun openAuthenticator(otpauthUri: String): Boolean {
        val uri = Uri.parse(otpauthUri)
        if (uri.scheme != "otpauth") return false
        return try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
