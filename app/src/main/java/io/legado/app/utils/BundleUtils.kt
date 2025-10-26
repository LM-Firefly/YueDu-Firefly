package io.legado.app.utils

import android.os.Bundle
import android.os.Parcelable
import java.io.Serializable

fun bundleOfCompat(vararg pairs: Pair<String, Any?>): Bundle {
    val bundle = Bundle(pairs.size)
    for ((key, value) in pairs) {
        when (value) {
            null -> bundle.putString(key, null)
            is Boolean -> bundle.putBoolean(key, value)
            is Byte -> bundle.putByte(key, value)
            is Char -> bundle.putChar(key, value)
            is Double -> bundle.putDouble(key, value)
            is Float -> bundle.putFloat(key, value)
            is Int -> bundle.putInt(key, value)
            is Long -> bundle.putLong(key, value)
            is Short -> bundle.putShort(key, value)
            is Bundle -> bundle.putBundle(key, value)
            is CharSequence -> bundle.putCharSequence(key, value)
            is Parcelable -> bundle.putParcelable(key, value)
            is BooleanArray -> bundle.putBooleanArray(key, value)
            is ByteArray -> bundle.putByteArray(key, value)
            is CharArray -> bundle.putCharArray(key, value)
            is DoubleArray -> bundle.putDoubleArray(key, value)
            is FloatArray -> bundle.putFloatArray(key, value)
            is IntArray -> bundle.putIntArray(key, value)
            is LongArray -> bundle.putLongArray(key, value)
            is ShortArray -> bundle.putShortArray(key, value)
            is Array<*> -> when {
                value.isArrayOf<Parcelable>() -> {
                    @Suppress("UNCHECKED_CAST")
                    bundle.putParcelableArray(key, value as Array<Parcelable>)
                }

                value.isArrayOf<String>() -> bundle.putStringArray(
                    key,
                    Array(value.size) { index -> value[index] as String }
                )
                value.isArrayOf<CharSequence>() -> {
                    bundle.putCharSequenceArray(
                        key,
                        Array(value.size) { index -> value[index] as CharSequence }
                    )
                }

                value.isArrayOf<Serializable>() -> bundle.putSerializable(key, value)
                else -> throw IllegalArgumentException(
                    "Illegal value array type ${value::class.java.componentType} for key '$key'"
                )
            }

            is Serializable -> bundle.putSerializable(key, value)
            else -> throw IllegalArgumentException(
                "Illegal value type ${value::class.java.name} for key '$key'"
            )
        }
    }
    return bundle
}
