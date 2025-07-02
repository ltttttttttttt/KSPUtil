package com.lt.ksp

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter

/**
 * creator: lt  2025/7/2  lt.dygzs@qq.com
 * effect :
 * warning:
 */

/**
 * 获取typealias的真实类型
 * 参考: https://github.com/google/ksp/issues/1371
 * https://github.com/google/ksp/blob/646d6d32f6d1bf7d5c08684d85c2135d1952a417/test-utils/src/main/kotlin/com/google/devtools/ksp/processor/TypeAliasProcessor.kt#L129
 */
internal fun Resolver.expandType(
    type: KSType,
    substitutions: MutableMap<KSTypeParameter, KSType> = mutableMapOf(),
): KSType {
    val decl = type.declaration
    return when (decl) {
        is KSClassDeclaration -> {
            val arguments = type.arguments.map {
                val argType = it.type?.resolve() ?: return@map it
                getTypeArgument(createKSTypeReferenceFromKSType(expandType(argType, substitutions)), it.variance)
            }
            decl.asType(arguments)
        }

        is KSTypeParameter -> {
            val substituted = substitutions.get(decl) ?: return type
            val fullySubstituted = expandType(substituted, substitutions)
            // update/cache with refined substitution
            if (substituted != fullySubstituted)
                substitutions[decl] = fullySubstituted
            fullySubstituted
        }

        is KSTypeAlias -> {
            val aliasedType = decl.type.resolve()

            decl.typeParameters.zip(type.arguments).forEach { (param, arg) ->
                arg.type?.resolve()?.let {
                    substitutions[param] = it
                }
            }

            expandType(aliasedType, substitutions)
        }

        else -> type
    }
}

/**
 * 查找获取lambda中的数据,如果不为空则停止遍历
 */
internal inline fun <T, R : Any> Iterable<T>.findBy(find: (T) -> R?): R? {
    var r: R? = null
    for (t in this) {
        r = find(t)
        if (r != null)
            break
    }
    return r
}