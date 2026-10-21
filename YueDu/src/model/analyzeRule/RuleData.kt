package io.YueDu.model.analyzeRule

import io.YueDu.utils.GSON

class RuleData : RuleDataInterface {

    override val variableMap by lazy {
        hashMapOf<String, String>()
    }

    override fun putBigVariable(key: String, value: String?) {
        if (value == null) {
            variableMap.remove(key)
        } else {
            variableMap[key] = value
        }
    }

    override fun getBigVariable(key: String): String? {
        return null
    }

    fun getVariable(): String? {
        if (variableMap.isEmpty()) {
            return null
        }
        return GSON.toJson(variableMap)
    }

}