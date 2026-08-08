package iad1tya.echo.music.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

fun buildUpiUri(vpa: String, name: String? = null, note: String? = null, amount: String? = null): String {
    val params = mutableListOf("pa=${Uri.encode(vpa)}")
    name?.let { params += "pn=${Uri.encode(it)}" }
    note?.let { params += "tn=${Uri.encode(it)}" }
    amount?.takeIf { it.isNotBlank() }?.let { params += "am=${Uri.encode(it)}" }
    return "upi://pay?${params.joinToString("&") }"
}

fun maskVpa(vpa: String, visibleLast: Int = 4): String {
    val at = vpa.indexOf('@')
    return if (at > 0) {
        val local = vpa.substring(0, at)
        val domain = vpa.substring(at)
        val visible = local.takeLast(visibleLast.coerceAtMost(local.length))
        val masked = if (local.length <= visibleLast) "••••" else "••••$visible"
        masked + domain
    } else {
        val visible = vpa.takeLast(visibleLast.coerceAtMost(vpa.length))
        if (vpa.length <= visibleLast) "••••" else "••••$visible"
    }
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val hints = mapOf(
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 1,
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q
    )
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bmp.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bmp
}

@Composable
fun UpiQrDialog(
    vpa: String,
    payeeName: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val uri = remember(vpa, payeeName) { buildUpiUri(vpa, payeeName) }

    val qrBitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = try {
            generateQrBitmap(uri, 600)
        } catch (e: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Scan to Pay", style = MaterialTheme.typography.titleMedium)
                qrBitmap?.let { bmp ->
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = "UPI QR", modifier = Modifier
                        .size(300.dp)
                        .align(alignment = androidx.compose.ui.Alignment.CenterHorizontally))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("UPI", vpa))
                        }) { Text("Copy UPI ID") }

                        Button(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }) { Text("Open") }

                        Button(onClick = onDismiss) { Text("Close") }
                    }
                } ?: Text("Unable to generate QR")
            }
        }
    }
}
