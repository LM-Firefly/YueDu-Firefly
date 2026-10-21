package io.YueDu.api.controller

import io.YueDu.api.ReturnData
import io.YueDu.data.appDb
import io.YueDu.data.entities.ReplaceRule
import io.YueDu.utils.GSON
import io.YueDu.utils.fromJsonObject
import io.YueDu.utils.replace
import io.YueDu.utils.stackTraceStr

object ReplaceRuleController {

    val allRules: ReturnData
        get() {
            val rules = appDb.replaceRuleDao.all
            val returnData = ReturnData()
            returnData.setData(GSON.toJson(rules))
            return returnData
        }


    fun saveRule(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        val rule = GSON.fromJsonObject<ReplaceRule>(postData).getOrNull()
        if (rule == null) {
            returnData.setErrorMsg("格式不对")
        } else {
            if (rule.order == Int.MIN_VALUE) {
                rule.order = appDb.replaceRuleDao.maxOrder + 1
            }
            appDb.replaceRuleDao.insert(rule)
        }
        return returnData
    }


    fun delete(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        val rule = GSON.fromJsonObject<ReplaceRule>(postData).getOrNull()
        if (rule == null) {
            returnData.setErrorMsg("格式不对")
        } else {
            appDb.replaceRuleDao.delete(rule)
        }
        return returnData
    }

    /**
     * 传入测试数据格式
     * {
     *  rule: Replace,
     *  text: "xxx"
     * }
     */
    fun testRule(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData ?: return returnData.setErrorMsg("数据不能为空")
        val map = GSON.fromJsonObject<Map<String, *>>(postData).getOrNull()
        if (map == null) {
            returnData.setErrorMsg("格式不对")
        } else {
            val rule = map["rule"]?.let {
                if (it is String) {
                    GSON.fromJsonObject<ReplaceRule>(it).getOrNull()
                } else {
                    GSON.fromJsonObject<ReplaceRule>(GSON.toJson(it)).getOrNull()
                }
            }
            if (rule == null) {
                returnData.setErrorMsg("格式不对")
                return returnData
            }
            if (rule.pattern.isEmpty()) {
                returnData.setErrorMsg("替换规则不能为空")
            }
            val text = map["text"] as String
            val content = try {
                if (rule.isRegex) {
                    text.replace(
                        rule.pattern.toRegex(),
                        rule.replacement,
                        rule.getValidTimeoutMillisecond()
                    )
                } else {
                    text.replace(rule.pattern, rule.replacement)
                }
            } catch (e: Exception) {
                e.stackTraceStr
            }
            returnData.setData(content)
        }
        return returnData
    }

}