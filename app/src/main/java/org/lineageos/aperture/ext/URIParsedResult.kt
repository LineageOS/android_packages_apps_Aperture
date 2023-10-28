package org.lineageos.aperture.ext

import android.content.Intent
import android.net.Uri
import com.google.zxing.client.result.URIParsedResult

fun URIParsedResult.createIntent() = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
