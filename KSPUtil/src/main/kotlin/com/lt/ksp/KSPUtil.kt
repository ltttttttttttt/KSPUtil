package com.lt.ksp

import com.google.devtools.ksp.findActualType
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.*
import com.lt.ksp.model.KSTypeInfo
import com.lt.ksp.model.TypeName
import java.io.OutputStream

/**
 * creator: lt  2022/10/21  lt.dygzs@qq.comS
 * effect : Common Tools for KSP
 * warning:
 */

/**
 * 向[OutputStream]中写入文字
 *
 * Write text to [OutputStream]
 */
fun OutputStream.appendText(str: String) {
    this.write(str.toByteArray())
}

/**
 * 获取ksType的完整泛型信息
 * [ksType] KSType信息
 *
 * Get complete generic information for ksType
 * [ksType] KSType Information
 */
fun getKSTypeInfo(
    ksType: KSType,
    childClass: KSClassDeclaration?,
    thisClass: KSClassDeclaration,
): KSTypeInfo {
    val arguments = ksType.arguments
    val childType: List<KSTypeInfo> = if (arguments.isEmpty()) listOf() else {
        //有泛型
        arguments.mapNotNull { it.type }.map {
            getKSTypeInfo(it.resolve(), childClass, thisClass)
        }
    }
    //是否可空
    var nullable = ksType.nullability == Nullability.NULLABLE
    //完整type字符串
    val thisType =
        ksType.declaration.let {
            val parentDeclaration = it.parentDeclaration
            //如果有父类,并且使用了父类的泛型
            if (parentDeclaration != null) {
                //处理使用父类的泛型(alpha),通过子类获取父类的子泛型(对比泛型名)
                childClass?.superTypes?.toList()?.findBy {
                    val thisType = it.resolve()
                    if (thisType.declaration.simpleName.asString() == thisClass.simpleName.asString())
                        thisType
                    else
                        null
                }?.let { thisType ->
                    thisType.arguments.getOrNull(
                        parentDeclaration.typeParameters.indexOfFirst { typeParameter ->
                            typeParameter.name.asString() == it.simpleName.asString()
                        }
                    )?.type?.let {
                        val type = it.resolve()
                        val declaration = type.declaration
                        nullable = type.nullability == Nullability.NULLABLE || ksType.isMarkedNullable
                        TypeName(declaration.packageName, declaration.simpleName)
                    }
                } ?: TypeName(it.packageName, it.simpleName)
            } else {
                TypeName(it.packageName, it.simpleName)
            }
        }
    return KSTypeInfo(
        //自身或泛型包含Buff注解
        thisType,
        nullable,
        childType
    )
}

/**
 * 获取ksType的完整子泛型信息列表
 * 可以自动判断是否是typealias类型并获取其中的真实类型
 * [ks] KSTypeReference信息
 * 参考: https://github.com/google/ksp/issues/1371 方案C
 *
 * Get the complete subgeneric information list of ksType
 * Can automatically determine whether it is a typealias type and obtain its true type
 * [ks] KSTypeReference Information
 */
fun getKSTypeArguments(
    ks: KSTypeReference,
    childClass: KSClassDeclaration?,
    thisClass: KSClassDeclaration,
    resolver: Resolver,
): List<KSTypeInfo> {
    //type对象
    val ksType = ks.resolve()
    //如果是typealias类型
    val arguments = if (ksType.declaration is KSTypeAlias) {
        resolver.expandType(ksType).arguments
    } else {
        ks.element?.typeArguments
    }
    return arguments?.map {
        getKSTypeInfo(it.type!!.resolve(), childClass, thisClass)
    } ?: listOf()
}

/**
 * 获取传入的type的最外层类的全类名
 * 比如Call<String>,获取到Call的全类名
 *
 * Get the full class name of the outermost class of the passed type
 * For example, Call<String>, obtain the full class name of Call
 */
fun getKSTypeOutermostName(ksType: KSType): TypeName {
    //如果是typealias类型
    return if (ksType.declaration is KSTypeAlias) {
        (ksType.declaration as KSTypeAlias).findActualType()
    } else {
        ksType.declaration
    }.let {
        TypeName(it.packageName, it.simpleName)
    }
}

/**
 * 通过[KSAnnotation]获取还原(构造)这个注解的String
 *
 * Obtain the String that restores (constructs) this annotation through [KSAnnotation]
 */
fun getNewAnnotationString(ksa: KSAnnotation): String {
    val ksType = ksa.annotationType.resolve()
    //完整type字符串
    val typeName = ksType.declaration.let {
        //todo bug? <ERROR TYPE: xxx>
        return@let if (ksType.isError)
            ksa.annotationType.toString()
        else
            "${it.packageName.asString()}.${it.simpleName.asString()}"
    }
    val args = StringBuilder()
    ksa.arguments.forEach {
        val value = it.value
        if (value != null) {
            val name = it.name
            if (name != null)
                args.append(name.asString())
                    .append(" = ")
            fun appendValue(value: Any?) {
                when (value) {
                    is String -> {
                        args.append("\"")
                            .append(value)
                            .append("\"")
                    }

                    is List<*> -> {
                        args.append("arrayOf(")
                        value.forEach(::appendValue)
                        args.append(")")
                    }

                    is KSType -> args.append(value).append("::class")
                    null -> args.append("null")
                    else -> args.append(value)
                }
                args.append(", ")
            }
            appendValue(value)
        }
    }
    return "$typeName($args)"
}

/**
 * 获取字符串type的子type,如果没有返回自身
 *
 * Get the subtype of the string type, if it does not return itself
 */
fun getTypeChild(type: String): String {
    if (!type.contains("<"))
        return type
    return type.substring(type.indexOf("<") + 1, type.lastIndexOf(">"))
}

/**
 * 判断KSType是否是List或其子类
 */
fun KSType.isList(): Boolean {
    val declaration = declaration
    val packageName = declaration.packageName.asString()
    val className = declaration.simpleName.asString()
    return if (packageName == "kotlin.collections" && className == "List")
        true
    else if (packageName == "kotlin" && className == "Any")
        false
    else {
        if (declaration is KSClassDeclaration) {
            declaration.superTypes.any {
                it.resolve().isList()
            }
        } else
            false
    }
}
