package io.YueDu.model

import android.content.Context
import io.YueDu.constant.IntentAction
import io.YueDu.service.DownloadService
import io.YueDu.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}