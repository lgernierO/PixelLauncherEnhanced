package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.TextUtils
import com.drdisagree.pixellauncherenhanced.data.common.Constants.APP_DRAWER_THEMED_ICONS
import com.drdisagree.pixellauncherenhanced.data.common.Constants.FORCE_THEMED_ICONS
import com.drdisagree.pixellauncherenhanced.data.common.Constants.LAUNCHER3_PACKAGE
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.LauncherUtils.Companion.reloadIcons
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.MonochromeIconFactory
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.callMethod
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.callMethodSilently
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.callStaticMethod
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getAnyField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getExtraFieldSilently
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getFieldSilently
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getStaticFieldSilently
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getStaticField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hasMethod
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookConstructor
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookMethod
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.setAnyField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.setExtraField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.setField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.setFieldSilently
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs

class ThemedIcons(context: Context) : ModPack(context) {

    private var forceThemedIcons: Boolean = false
    private var appDrawerThemedIcons: Boolean = false

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            forceThemedIcons = getBoolean(FORCE_THEMED_ICONS, false)
            appDrawerThemedIcons = getBoolean(APP_DRAWER_THEMED_ICONS, false)
        }

        when (key.firstOrNull()) {
            in setOf(
                FORCE_THEMED_ICONS,
                APP_DRAWER_THEMED_ICONS
            ) -> reloadIcons()
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun handleLoadPackage(packageName: String, classLoader: ClassLoader) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        try {
            // Only for modified Launcher3
            if (mContext.packageName != LAUNCHER3_PACKAGE) throw Throwable()

            val launcherIconsClass = findClass("com.android.launcher3.icons.LauncherIcons")

            launcherIconsClass
                .hookMethod("getMonochromeDrawable")
                .throwError() // Available only in modified launcher3
                .runAfter { param ->
                    if (param.result == null && forceThemedIcons) {
                        val mIconBitmapSize = param.thisObject.getField("mIconBitmapSize") as Int

                        param.result = MonochromeIconFactory(mIconBitmapSize, true)
                            .wrap(mContext, param.args[0] as Drawable)
                    }
                }
        } catch (_: Throwable) {
            val baseIconFactoryClass = findClass("com.android.launcher3.icons.BaseIconFactory")

            baseIconFactoryClass
                .hookConstructor()
                .runAfter { param ->
                    val mIconBitmapSize = param.thisObject.getAnyField(
                        "mIconBitmapSize",
                        "iconBitmapSize"
                    ) as Int
                    val monochromeIconFactory = MonochromeIconFactory(mIconBitmapSize, false)

                    AdaptiveIconDrawable::class.java
                        .hookMethod("getMonochrome")
                        .runAfter runAfter2@{ param2 ->
                            if (param2.result == null && forceThemedIcons) {
                                // If it's from com.android.launcher3.icons.IconProvider class and
                                // mentioned methods, monochrome is already included
                                Thread.currentThread().stackTrace.firstOrNull {
                                    it.className.contains("IconProvider") && it.methodName in listOf(
                                        "getIconWithOverrides",
                                        "getIcon"
                                    )
                                }?.let { return@runAfter2 }

                                var monochromeIcon = param2.thisObject
                                    .getExtraFieldSilently("mMonochromeIcon") as? Drawable

                                if (monochromeIcon == null) {
                                    monochromeIcon = monochromeIconFactory.wrap(
                                        mContext,
                                        param2.thisObject as Drawable
                                    )
                                    param2.thisObject.setExtraField(
                                        "mMonochromeIcon",
                                        monochromeIcon
                                    )
                                }

                                param2.result = monochromeIcon
                            }
                        }
                }
        }

        val bubbleTextViewClass = findClass("com.android.launcher3.BubbleTextView")
        val themesClass = findClass("com.android.launcher3.util.Themes")
        val themeManagerClass = findClass(
            "com.android.launcher3.graphics.ThemeManager",
            suppressError = true
        )
        val themePreferenceClass = findClass(
            "com.android.launcher3.graphics.theme.ThemePreference",
            suppressError = true
        )
        val intArrayClass = findClass(
            "com.android.launcher3.util.IntArray",
            suppressError = true
        )

        // Hook shouldUseTheme if it exists (old launcher versions)
        bubbleTextViewClass
            .hookMethod("shouldUseTheme")
            .also { if (Build.VERSION.SDK_INT >= 37) it.suppressError() }
            .runAfter { param ->
                if (param.result == false && appDrawerThemedIcons) {
                val context = (param.thisObject as? android.view.View)?.context ?: return@runAfter
                    val mDisplay = param.thisObject.getField("mDisplay") as Int

                    param.result = mDisplay.shouldUseTheme(
                        context,
                        themesClass,
                        themeManagerClass,
                        themePreferenceClass
                    )
                }
            }

        // Android 17+ hooks for enhanced themed icon handling
        if (Build.VERSION.SDK_INT >= 37) {
            bubbleTextViewClass
                .hookMethod("getIconCreationFlagsForInfo")
                .runBefore { param ->
                    if (!appDrawerThemedIcons) return@runBefore

                    val context = param.thisObject.callMethod("getContext") as Context
                    val mDisplay = param.thisObject.getField("mDisplay") as Int
                    val mHideBadge = param.thisObject.getField("mHideBadge") as Boolean
                    val mSkipUserBadge = param.thisObject.getField("mSkipUserBadge") as Boolean

                    val shouldUseTheme = mDisplay.shouldUseTheme(
                        context,
                        themesClass,
                        themeManagerClass,
                        themePreferenceClass
                    )

                    var flags = if (shouldUseTheme) FLAG_THEMED else 0

                    if (mHideBadge || mDisplay == DISPLAY_SEARCH_RESULT_SMALL) {
                        flags = flags or FLAG_NO_BADGE
                    }
                    if (mSkipUserBadge) {
                        flags = flags or FLAG_SKIP_USER_BADGE
                    }

                    param.result = flags
                }

            val cacheLookupFlagClass =
                findClass("com.android.launcher3.icons.cache.CacheLookupFlag")

            bubbleTextViewClass
                .hookMethod("verifyHighRes")
                .runBefore { param ->
                    if (!appDrawerThemedIcons) return@runBefore

                    val context = param.thisObject.callMethod("getContext") as Context
                    val mDisplay = param.thisObject.getField("mDisplay") as Int

                    verifyHighResThemeOverride.set(
                        mDisplay.shouldUseTheme(
                            context,
                            themesClass,
                            themeManagerClass,
                            themePreferenceClass
                        )
                    )
                }
                .runAfter {
                    verifyHighResThemeOverride.remove()
                }

            cacheLookupFlagClass
                .hookMethod("updateMask")
                .runBefore { param ->
                    if (!appDrawerThemedIcons) return@runBefore

                    val addMask = verifyHighResThemeOverride.get() ?: return@runBefore
                    param.args[1] = addMask
                }

            bubbleTextViewClass
                .hookMethod("applyFromWorkspaceItem")
                .parameters("com.android.launcher3.model.data.WorkspaceItemInfo")
                .runAfter { param ->
                    if (!appDrawerThemedIcons) return@runAfter

                    val context = param.thisObject.callMethod("getContext") as Context
                    val mDisplay = param.thisObject.getField("mDisplay") as Int

                    if (mDisplay.shouldUseTheme(
                            context,
                            themesClass,
                            themeManagerClass,
                            themePreferenceClass
                        )
                    ) {
                        param.thisObject.callMethod("verifyHighRes")
                    }
                }
        }

        // Hook ThemeManager.isIconThemeEnabled() for new launcher versions
        themeManagerClass?.let { clazz ->
            clazz.hookMethod("isIconThemeEnabled")
                .suppressError()
                .runAfter { param ->
                    if (appDrawerThemedIcons && param.result == false) {
                        param.result = true
                    }
                }
        }

        val monoIconThemeControllerClass = findClass(
            "com.android.launcher3.icons.mono.MonoIconThemeController",
            suppressError = true
        )

        // Hook ThemeManager.verifyIconState() to inject MonoIconThemeController
        // into iconState.themeController when it's null
        if (themeManagerClass != null && monoIconThemeControllerClass != null) {
            var monoControllerCache: Any? = null

            fun getOrCreateController(): Any {
                return monoControllerCache ?: monoIconThemeControllerClass.getConstructor().newInstance().also {
                    monoControllerCache = it
                }
            }

            themeManagerClass.hookMethod("verifyIconState")
                .suppressError()
                .runAfter { param ->
                    if (!appDrawerThemedIcons) return@runAfter

                    val thisObj = param.thisObject
                    val iconState = thisObj.getFieldSilently("iconState") ?: return@runAfter
                    val themeController = iconState.getFieldSilently("themeController")
                    if (themeController == null) {
                        iconState.setField("themeController", getOrCreateController())
                    }
                }
        }

        // Hook BaseIconFactory constructor as fallback
        val baseIconFactoryClass2 = findClass(
            "com.android.launcher3.icons.BaseIconFactory",
            suppressError = true
        )

        if (baseIconFactoryClass2 != null && monoIconThemeControllerClass != null) {
            var monoControllerCache2: Any? = null

            fun getOrCreateController2(): Any {
                return monoControllerCache2 ?: monoIconThemeControllerClass.getConstructor().newInstance().also {
                    monoControllerCache2 = it
                }
            }

            baseIconFactoryClass2.hookConstructor()
                .suppressError()
                .runAfter { param ->
                    if (!appDrawerThemedIcons) return@runAfter

                    val themeController = param.thisObject.getFieldSilently("themeController")
                    if (themeController == null) {
                        param.thisObject.setField("themeController", getOrCreateController2())
                    }
                }
        }

        // Hook BitmapInfo.newIcon() to force themed rendering for icons without themedBitmap
        val bitmapInfoClass = findClass(
            "com.android.launcher3.icons.BitmapInfo",
            suppressError = true
        )

        if (bitmapInfoClass != null) {
            bitmapInfoClass.hookMethod("newIcon")
                .suppressError()
                .runBefore { param ->
                    if (!appDrawerThemedIcons) return@runBefore

                    val flags = param.args[1] as? Int ?: return@runBefore
                    if ((flags and FLAG_THEMED) == 0) return@runBefore

                    val themedBitmap = param.thisObject.getFieldSilently("themedBitmap")
                    if (themedBitmap == null || themedBitmap.javaClass.simpleName == "NOT_SUPPORTED") {
                        val icon = param.thisObject.getFieldSilently("icon") as? android.graphics.Bitmap
                        if (icon != null) {
                            try {
                                val softwareIcon = if (icon.config == android.graphics.Bitmap.Config.HARDWARE) {
                                    icon.copy(android.graphics.Bitmap.Config.ARGB_8888, true) ?: icon
                                } else {
                                    icon
                                }
                                val monoDrawable = MonochromeIconFactory(softwareIcon.width, false)
                                    .wrap(mContext, android.graphics.drawable.BitmapDrawable(mContext.resources, softwareIcon))

                                val monoBitmap = try {
                                    val bitmapField = monoDrawable.javaClass.getDeclaredField("mAlphaBitmap")
                                    bitmapField.isAccessible = true
                                    bitmapField.get(monoDrawable) as? android.graphics.Bitmap
                                } catch (_: Throwable) {
                                    val w = monoDrawable.intrinsicWidth.coerceAtLeast(1)
                                    val h = monoDrawable.intrinsicHeight.coerceAtLeast(1)
                                    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ALPHA_8)
                                    val canvas = android.graphics.Canvas(bmp)
                                    monoDrawable.setBounds(0, 0, w, h)
                                    monoDrawable.draw(canvas)
                                    bmp
                                }

                                if (monoBitmap != null) {
                                    val monoThemedBitmapClass = findClass(
                                        "com.android.launcher3.icons.mono.MonoThemedBitmap",
                                        suppressError = true
                                    )
                                    val cInterface = findClass("ta.c", suppressError = true)

                                    if (monoThemedBitmapClass != null && cInterface != null) {
                                        val monoControllerClass = findClass(
                                            "com.android.launcher3.icons.mono.MonoIconThemeController",
                                            suppressError = true
                                        )
                                        val colorProvider = monoControllerClass?.let { ctrlClass ->
                                            try {
                                                val instance = ctrlClass.getConstructor().newInstance()
                                                val field = ctrlClass.getDeclaredField("colorProvider")
                                                field.isAccessible = true
                                                field.get(instance)
                                            } catch (_: Throwable) {
                                                null
                                            }
                                        }

                                        if (colorProvider != null) {
                                            val newThemedBitmap = monoThemedBitmapClass
                                                .getConstructor(android.graphics.Bitmap::class.java, cInterface, Double::class.javaObjectType)
                                                .newInstance(monoBitmap, colorProvider, null)

                                            param.thisObject.setField("themedBitmap", newThemedBitmap)
                                        }
                                    }
                                }
                            } catch (_: Throwable) {
                            }
                        }
                    }
                }
        }

        bubbleTextViewClass
            .hookMethod("applyIconAndLabel")
            .parameters("com.android.launcher3.model.data.ItemInfoWithIcon")
            .runAfter { param ->
                if (!appDrawerThemedIcons) return@runAfter

                val info = param.args[0]
                val context = (param.thisObject as? android.view.View)?.context ?: return@runAfter
                val mDisplay = param.thisObject.getField("mDisplay") as? Int ?: return@runAfter
                val mHideBadge = param.thisObject.getField("mHideBadge") as? Boolean ?: false
                val mSkipUserBadge = param.thisObject.getField("mSkipUserBadge") as? Boolean ?: false
                val shouldUseTheme = mDisplay.shouldUseTheme(
                    context,
                    themesClass,
                    themeManagerClass,
                    themePreferenceClass
                )

                var flags = if (shouldUseTheme) FLAG_THEMED else 0

                if (mHideBadge || mDisplay == DISPLAY_SEARCH_RESULT_SMALL) {
                    flags = flags or FLAG_NO_BADGE
                }
                if (mSkipUserBadge) {
                    flags = flags or FLAG_SKIP_USER_BADGE
                }

                val hasNewIconWithContextFirst = info.hasMethod(
                    "newIcon",
                    Context::class.java,
                    Int::class.javaPrimitiveType
                )
                val iconDrawable = if (hasNewIconWithContextFirst) {
                    info.callMethod("newIcon", context, flags)
                } else {
                    info.callMethod("newIcon", flags, context)
                }
                val mDotParams = param.thisObject.getField("mDotParams")

                mDotParams.setFieldSilently(
                    "appColor",
                    iconDrawable.callMethodSilently("getIconColor")
                )
                val notificationDotColorAttr = context.resources.getIdentifier(
                    "notificationDotColor",
                    "attr",
                    context.packageName
                )
                mDotParams.setAnyField(
                    if (notificationDotColorAttr != 0) {
                        LauncherUtils.getAttrColor(context, notificationDotColorAttr)
                    } else {
                        context.resources.getColor(
                            context.resources.getIdentifier(
                                "system_accent3_200",
                                "color",
                                context.packageName
                            ),
                            context.theme
                        )
                    },
                    "dotColor",
                    "mDotColor"
                )

                param.thisObject.callMethod("setIcon", iconDrawable)
            }
    }

    private fun Int.shouldUseTheme(
        context: Context,
        themesClass: Class<*>?,
        themeManagerClass: Class<*>?,
        themePreferenceClass: Class<*>?
    ) = this in THEMED_DISPLAYS && runCatching {
        // New launcher version: ThemeManager.isIconThemeEnabled()
        themeManagerClass
            ?.getStaticFieldSilently("INSTANCE")
            ?.callMethodSilently("get", context)
            ?.callMethodSilently("isIconThemeEnabled") as? Boolean == true
    }.getOrElse {
        runCatching {
            // Old launcher version: Themes.isThemedIconEnabled(context)
            themesClass.callStaticMethod("isThemedIconEnabled", context)
        }.getOrElse {
            runCatching {
                // Older launcher version: ThemeManager.isMonoThemeEnabled()
                themeManagerClass
                    .getStaticField("INSTANCE")
                    .callMethod("get", context)
                    .callMethod("isMonoThemeEnabled")
            }.getOrElse {
                // Oldest launcher version: ThemePreference check
                themePreferenceClass?.getStaticFieldSilently("MONO_THEME_VALUE") == themeManagerClass
                    ?.getStaticFieldSilently("INSTANCE")
                    ?.callMethodSilently("get", context)
                    ?.getFieldSilently("themePreference")
                    ?.callMethodSilently("getValue")
            }
        }
    } as Boolean

    companion object {
        private const val FLAG_THEMED: Int = 1 shl 0
        private const val FLAG_NO_BADGE: Int = 1 shl 1
        private const val FLAG_SKIP_USER_BADGE: Int = 1 shl 2

        private const val DISPLAY_WORKSPACE: Int = 0
        private const val DISPLAY_ALL_APPS: Int = 1
        private const val DISPLAY_FOLDER: Int = 2
        private const val DISPLAY_TASKBAR: Int = 5
        private const val DISPLAY_SEARCH_RESULT: Int = 6
        private const val DISPLAY_SEARCH_RESULT_SMALL: Int = 7
        private const val DISPLAY_PREDICTION_ROW: Int = 8
        private const val DISPLAY_SEARCH_RESULT_APP_ROW: Int = 9

        private val THEMED_DISPLAYS = setOf(
            DISPLAY_WORKSPACE,
            DISPLAY_ALL_APPS,
            DISPLAY_FOLDER,
            DISPLAY_TASKBAR,
            DISPLAY_SEARCH_RESULT,
            DISPLAY_SEARCH_RESULT_SMALL,
            DISPLAY_PREDICTION_ROW,
            DISPLAY_SEARCH_RESULT_APP_ROW
        )

        private val verifyHighResThemeOverride = ThreadLocal<Boolean?>()
    }
}
