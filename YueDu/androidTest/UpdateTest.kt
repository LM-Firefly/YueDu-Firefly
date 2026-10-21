package io.YueDu

import com.google.gson.Gson
import io.YueDu.exception.NoStackTraceException
import io.YueDu.help.http.okHttpClient
import io.YueDu.help.update.GithubRelease
import io.YueDu.utils.fromJsonObject
import okhttp3.Request
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {

    private val lastReleaseUrl =
        "https://api.github.com/repos/LM-Firefly/YueDu-Firefly/releases/latest"

    private val lastBetaReleaseUrl =
        "https://api.github.com/repos/LM-Firefly/YueDu-Firefly/releases/tags/beta"

    @Test
    fun updateApp_beta() {
        val body = okHttpClient.newCall(Request.Builder().url(lastBetaReleaseUrl).build()).execute()
            .body!!.string()

        val releaseList = Gson().fromJsonObject<GithubRelease>(body)
            .getOrElse {
                throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
            }
            .gitReleaseToAppReleaseInfo()
            .sortedByDescending { it.createdAt }

        assertTrue(releaseList.size == 2)
        assertTrue(releaseList.all { it.downloadUrl.isNotBlank() })
        assertTrue(releaseList.all { it.versionName.isNotBlank() })
    }

    @Test
    fun updateApp() {
        val body = okHttpClient.newCall(Request.Builder().url(lastReleaseUrl).build()).execute()
            .body!!.string()

        val releaseList = Gson().fromJsonObject<GithubRelease>(body)
            .getOrElse {
                throw NoStackTraceException("获取新版本出错 " + it.localizedMessage)
            }
            .gitReleaseToAppReleaseInfo()
            .sortedByDescending { it.createdAt }

        assertTrue(releaseList.size == 1)
        assertTrue(releaseList.all { it.downloadUrl.isNotBlank() })
        assertTrue(releaseList.all { it.versionName.isNotBlank() })
    }

}
