package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.content.Context
import com.drdisagree.pixellauncherenhanced.data.common.Constants.ALLOW_WALLPAPER_ZOOMING
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getField
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookConstructor
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookMethod
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs

class WallpaperZoom(context: Context) : ModPack(context) {

    private var mWallpaperZooming = true

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            mWallpaperZooming = getBoolean(ALLOW_WALLPAPER_ZOOMING, true)
        }
    }

    override fun handleLoadPackage(packageName: String, classLoader: ClassLoader) {
        val baseDepthControllerClass = findClass(
            "com.android.quickstep.util.BaseDepthController",
            "com.android.quickstep.util.BaseDepthControllerImpl",
        )

        baseDepthControllerClass
            .hookConstructor()
            .runAfter { param ->
                val mWallpaperManager = param.thisObject.getField("mWallpaperManager")

                mWallpaperManager.javaClass
                    .hookMethod("setWallpaperZoomOut")
                    .runBefore { param2 ->
                        if (!mWallpaperZooming && param2.thisObject == mWallpaperManager) {
                            param2.args[1] = 1
                        }
                    }
            }
    }
}