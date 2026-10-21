package io.YueDu.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import io.YueDu.R
import io.YueDu.constant.AppConst.appInfo
import io.YueDu.constant.AppLog
import io.YueDu.help.CrashHandler
import io.YueDu.help.config.AppConfig
import io.YueDu.help.coroutine.Coroutine
import io.YueDu.help.update.AppUpdate
import io.YueDu.ui.widget.dialog.TextDialog
import io.YueDu.ui.widget.dialog.WaitDialog
import io.YueDu.utils.FileDoc
import io.YueDu.utils.compress.ZipUtils
import io.YueDu.utils.createFileIfNotExist
import io.YueDu.utils.createFolderIfNotExist
import io.YueDu.utils.delete
import io.YueDu.utils.externalCache
import io.YueDu.utils.find
import io.YueDu.utils.list
import io.YueDu.utils.openInputStream
import io.YueDu.utils.openOutputStream
import io.YueDu.utils.openUrl
import io.YueDu.utils.sendMail
import io.YueDu.utils.sendToClip
import io.YueDu.utils.showDialogFragment
import io.YueDu.utils.toastOnUi
import kotlinx.coroutines.delay
import splitties.init.appCtx
import java.io.File

class AboutFragment : PreferenceFragmentCompat() {

    private val waitDialog by lazy {
        WaitDialog(requireContext())
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.about)
        findPreference<Preference>("update_log")?.summary =
            "${getString(R.string.version)} ${appInfo.versionName}"
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "contributors" -> openUrl(R.string.contributors_url)
            "update_log" -> showMdFile(getString(R.string.update_log), "updateLog.md")
            "check_update" -> checkUpdate()
            "mail" -> requireContext().sendMail(getString(R.string.email))
            "license" -> showMdFile(getString(R.string.license), "LICENSE.md")
            "disclaimer" -> showMdFile(getString(R.string.disclaimer), "disclaimer.md")
            "privacyPolicy" -> showMdFile(getString(R.string.privacy_policy), "privacyPolicy.md")
            "gzGzh" -> requireContext().sendToClip(getString(R.string.legado_gzh))
            "crashLog" -> showDialogFragment<CrashLogsDialog>()
            "saveLog" -> saveLog()
            "createHeapDump" -> createHeapDump()
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("SameParameterValue")
    private fun openUrl(@StringRes addressID: Int) {
        requireContext().openUrl(getString(addressID))
    }

    /**
     * 显示md文件
     */
    private fun showMdFile(title: String, fileName: String) {
        val mdText = String(requireContext().assets.open(fileName).readBytes())
        showDialogFragment(TextDialog(title, mdText, TextDialog.Mode.MD))
    }

    /**
     * 检测更新
     */
    private fun checkUpdate() {
        waitDialog.show()
        AppUpdate.gitHubUpdate?.run {
            check(lifecycleScope)
                .onSuccess {
                    showDialogFragment(
                        UpdateDialog(it)
                    )
                }.onError {
                    appCtx.toastOnUi("${getString(R.string.check_update)}\n${it.localizedMessage}")
                }.onFinally {
                    waitDialog.dismiss()
                }
        }
    }


    /**
     * 加入qq群
     */
    private fun joinQQGroup(key: String): Boolean {
        val intent = Intent()
        intent.data =
            Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D$key")
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        kotlin.runCatching {
            startActivity(intent)
            return true
        }.onFailure {
            toastOnUi("添加失败,请手动添加")
        }
        return false
    }

    private fun saveLog() {
        Coroutine.async {
            val backupPath = AppConfig.backupPath ?: let {
                appCtx.toastOnUi("未设置备份目录")
                return@async
            }
            if (!AppConfig.recordLog) {
                appCtx.toastOnUi("未开启日志记录，请去其他设置里打开记录日志")
                delay(3000)
            }
            val doc = FileDoc.fromUri(Uri.parse(backupPath), true)
            copyLogs(doc)
            copyHeapDump(doc)
            appCtx.toastOnUi("已保存至备份目录")
        }.onError {
            AppLog.put("保存日志出错\n${it.localizedMessage}", it, true)
        }
    }

    private fun createHeapDump() {
        Coroutine.async {
            val backupPath = AppConfig.backupPath ?: let {
                appCtx.toastOnUi("未设置备份目录")
                return@async
            }
            if (!AppConfig.recordHeapDump) {
                appCtx.toastOnUi("未开启堆转储记录，请去其他设置里打开记录堆转储")
                delay(3000)
            }
            appCtx.toastOnUi("开始创建堆转储")
            System.gc()
            CrashHandler.doHeapDump(true)
            val doc = FileDoc.fromUri(Uri.parse(backupPath), true)
            if (!copyHeapDump(doc)) {
                appCtx.toastOnUi("未找到堆转储文件")
            } else {
                appCtx.toastOnUi("已保存至备份目录")
            }
        }.onError {
            AppLog.put("保存堆转储失败\n${it.localizedMessage}", it)
        }
    }

    private fun copyLogs(doc: FileDoc) {
        val cacheDir = appCtx.externalCache
        val logFiles = File(cacheDir, "logs")
        val crashFiles = File(cacheDir, "crash")
        val logcatFile = File(cacheDir, "logcat.txt")

        dumpLogcat(logcatFile)

        val zipFile = File(cacheDir, "logs.zip")
        ZipUtils.zipFiles(arrayListOf(logFiles, crashFiles, logcatFile), zipFile)

        doc.find("logs.zip")?.delete()

        zipFile.inputStream().use { input ->
            doc.createFileIfNotExist("logs.zip").openOutputStream().getOrNull()
                ?.use {
                    input.copyTo(it)
                }
        }
        zipFile.delete()
    }

    private fun copyHeapDump(doc: FileDoc): Boolean {
        val heapFile = FileDoc.fromFile(File(appCtx.externalCache, "heapDump")).list()
            ?.firstOrNull() ?: return false
        doc.find("heapDump")?.delete()
        val heapDumpDoc = doc.createFolderIfNotExist("heapDump")
        heapFile.openInputStream().getOrNull()?.use { input ->
            heapDumpDoc.createFileIfNotExist(heapFile.name).openOutputStream().getOrNull()
                ?.use {
                    input.copyTo(it)
                }
        }
        return true
    }

    private fun dumpLogcat(file: File) {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            file.outputStream().use {
                process.inputStream.copyTo(it)
            }
        } catch (e: Exception) {
            AppLog.put("保存Logcat失败\n$e", e)
        }
    }

}