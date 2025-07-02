package com.lt.ksp.model

import com.google.devtools.ksp.symbol.KSName

/**
 * creator: lt  2025/7/2  lt.dygzs@qq.com
 * effect : 用字符串表示Type
 * warning:
 */
class TypeName(
    val packageName: KSName,
    val simpleName: KSName,
) {
    override fun toString(): String {
        return "${packageName.asString()}.${simpleName.asString()}"
    }
}