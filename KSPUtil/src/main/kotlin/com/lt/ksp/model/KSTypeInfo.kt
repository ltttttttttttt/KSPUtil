package com.lt.ksp.model

/**
 * creator: lt  2022/10/22  lt.dygzs@qq.com
 * effect : 用于记录Type的信息
 * warning:
 */
class KSTypeInfo(
    //自身的类型
    val thisTypeName: TypeName,
    //自身的可空性
    val nullable: Boolean,
    //子的类型
    val childType: List<KSTypeInfo>,
) {
    override fun toString(): String {
        return "$thisTypeName${
            childType.joinToString(
                prefix = "<",
                postfix = ">"
            )
        }${if (nullable) "?" else ""}"
    }
}