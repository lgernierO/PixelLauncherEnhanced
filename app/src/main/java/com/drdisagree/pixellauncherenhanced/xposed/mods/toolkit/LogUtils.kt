package com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit

import android.util.Log
import android.view.View
import android.view.ViewGroup

private const val TAG = "PLEnhanced"

fun log(message: String?) {
    Log.i(TAG, message ?: "null")
}

fun log(message: Any?) {
    Log.i(TAG, message.toString())
}

fun log(tag: String, message: Any?) {
    Log.i("[$TAG] $tag", message.toString())
}

fun <T : Any> log(clazz: T, message: Any?) {
    Log.i(
        "[$TAG] ${clazz.javaClass.simpleName.replace("\$Companion", "")}",
        message.toString()
    )
}

fun <T : Any> log(clazz: T, throwable: Throwable?) {
    Log.i(
        "[$TAG] ${clazz.javaClass.simpleName.replace("\$Companion", "")}",
        throwable.toString()
    )
}

fun <T : Any> log(clazz: T, exception: Exception?) {
    Log.i(
        "[$TAG] ${clazz.javaClass.simpleName.replace("\$Companion", "")}",
        exception.toString()
    )
}

fun findAndDumpClass(className: String, classLoader: ClassLoader?): Class<*> {
    dumpClass(className, classLoader)
    return Class.forName(className, false, classLoader)
}

fun findAndDumpClassIfExists(className: String, classLoader: ClassLoader?): Class<*> {
    dumpClass(className, classLoader)
    return try {
        Class.forName(className, false, classLoader)
    } catch (_: ClassNotFoundException) {
        throw ClassNotFoundException("Class not found: $className")
    }
}

private fun dumpClass(className: String, classLoader: ClassLoader?) {
    val ourClass = try {
        Class.forName(className, false, classLoader)
    } catch (_: ClassNotFoundException) {
        null
    }
    if (ourClass == null) {
        Log.i(TAG, "DumpClass: Class is null")
        return
    }
    ourClass.dumpClass()
}

fun Class<*>?.dumpClass() {
    if (this == null) {
        Log.i(TAG, "DumpClass: Class is null")
        return
    }

    Log.i(TAG, "\n\nClass: $name")
    Log.i(TAG, "extends: ${superclass?.name}")

    Log.i(TAG, "Subclasses:")
    val scs = classes
    for (c in scs) {
        Log.i(TAG, "\t" + c.name)
    }
    if (scs.isEmpty()) {
        Log.i(TAG, "\tNone")
    }

    Log.i(TAG, "Constructors:")
    val cons = declaredConstructors
    for (m in cons) {
        Log.i(TAG, "\t" + m.name + " - " + this::class.java.simpleName + " - " + m.parameterCount)
        val cs = m.parameterTypes
        for (c in cs) {
            Log.i(TAG, "\t\t" + c.typeName)
        }
    }
    if (cons.isEmpty()) {
        Log.i(TAG, "\tNone")
    }

    Log.i(TAG, "Methods:")
    val ms = declaredMethods.toList().union(methods.toList())
    for (m in ms) {
        Log.i(TAG, "\t" + m.name + " - " + m.returnType + " - " + m.parameterCount)
        val cs = m.parameterTypes
        for (c in cs) {
            Log.i(TAG, "\t\t" + c.typeName)
        }
    }
    if (ms.isEmpty()) {
        Log.i(TAG, "\tNone")
    }

    Log.i(TAG, "Fields:")
    val fs = declaredFields
    for (f in fs) {
        Log.i(TAG, "\t" + f.name + " - " + f.type.name)
    }
    if (fs.isEmpty()) {
        Log.i(TAG, "\tNone")
    }
    Log.i(TAG, "End dump\n\n")
}

fun View.dumpChildViews() {
    if (this is ViewGroup) {
        logViewInfo(this, 0)
        dumpChildViewsRecursive(this, 0)
    } else {
        logViewInfo(this, 0)
    }
}

private fun dumpChildViewsRecursive(
    viewGroup: ViewGroup,
    indentationLevel: Int
) {
    for (i in 0 until viewGroup.childCount) {
        val childView = viewGroup.getChildAt(i)
        logViewInfo(childView, indentationLevel + 1)
        if (childView is ViewGroup) {
            dumpChildViewsRecursive(childView, indentationLevel + 1)
        }
    }
}

private fun logViewInfo(view: View, indentationLevel: Int) {
    val indentation = repeatString("\t", indentationLevel)
    val viewName = view.javaClass.simpleName
    val superclassName = view.javaClass.superclass?.simpleName ?: "None"
    val backgroundDrawable = view.background
    val childCount = if (view is ViewGroup) view.childCount else 0
    var resourceIdName = "none"
    try {
        val viewId = view.id
        resourceIdName = view.context.resources.getResourceName(viewId)
    } catch (_: Throwable) {
    }
    var logMessage = "$indentation$viewName (Extends: $superclassName) - ID: $resourceIdName"
    if (childCount > 0) {
        logMessage += " - ChildCount: $childCount"
    }
    if (backgroundDrawable != null) {
        logMessage += " - Background: ${backgroundDrawable.javaClass.simpleName}"
    }
    Log.i(TAG, logMessage)
}

@Suppress("SameParameterValue")
private fun repeatString(str: String, times: Int): String {
    val result = StringBuilder()
    for (i in 0 until times) {
        result.append(str)
    }
    return result.toString()
}

fun Any.dumpPreferenceKeys() {
    for (i in 0 until callMethod("getPreferenceCount") as Int) {
        val preference = callMethod("getPreference", i)!!

        log("${preference::class.java.simpleName} -> Key: ${preference.callMethod("getKey")}")

        if (preference::class.java.simpleName == "PreferenceCategory") {
            preference.dumpPreferenceKeys()
        }
    }
}
