package com.drdisagree.pixellauncherenhanced.xposed.mods

import android.content.Context
import com.drdisagree.pixellauncherenhanced.data.common.Constants.LAUNCHER_ICON_SIZE
import com.drdisagree.pixellauncherenhanced.data.common.Constants.LAUNCHER_TEXT_SIZE
import com.drdisagree.pixellauncherenhanced.xposed.ModPack
import com.drdisagree.pixellauncherenhanced.xposed.mods.LauncherUtils.Companion.reloadLauncher
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook.Companion.findClass
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.getFieldSilently
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookConstructor
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.hookMethod
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.setFieldSilently
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs

class IconTextSize(context: Context) : ModPack(context) {

    private class ProfileBuildState(
        val parent: ProfileBuildState?
    ) {
        var workspaceProfileCreated = false
        var allAppsProfileCreated = false
    }

    private var iconSizeModifier = 1f
    private var textSizeModifier = 1f
    private val profileBuildState = ThreadLocal<ProfileBuildState>()

    override fun updatePrefs(vararg key: String) {
        Xprefs.apply {
            iconSizeModifier = getSliderInt(LAUNCHER_ICON_SIZE, 100) / 100f
            textSizeModifier = getSliderInt(LAUNCHER_TEXT_SIZE, 100) / 100f
        }

        when (key.firstOrNull()) {
            in setOf(
                LAUNCHER_ICON_SIZE,
                LAUNCHER_TEXT_SIZE
            ) -> reloadLauncher(mContext)
        }
    }

    override fun handleLoadPackage(packageName: String, classLoader: ClassLoader) {
        val deviceProfileClass = findClass("com.android.launcher3.DeviceProfile")
        val legacyDeviceProfileBuilderClass = findClass(
            $$"com.android.launcher3.DeviceProfile$Builder",
            suppressError = true
        )
        val deviceProfileBuilderClass = findClass(
            "com.android.launcher3.deviceprofile.DeviceProfileBuilder",
            suppressError = true
        )
        val workspaceProfileClass = findClass(
            "com.android.launcher3.deviceprofile.WorkspaceProfile",
            suppressError = true
        )
        val allAppsProfileClass = findClass(
            "com.android.launcher3.deviceprofile.AllAppsProfile",
            suppressError = true
        )

        // Older Launcher3 versions keep all dimensions directly in DeviceProfile.
        deviceProfileClass
            .hookConstructor()
            .runAfter { param ->
                param.thisObject?.scaleLegacyDeviceProfile()
            }

        val builderClasses = listOfNotNull(
            legacyDeviceProfileBuilderClass,
            deviceProfileBuilderClass
        ).distinct()

        // Newer launchers build immutable WorkspaceProfile and AllAppsProfile objects. Their
        // final fields can no longer be reliably changed after DeviceProfile is constructed, so
        // keep track of the build call and alter the profile constructor arguments instead.
        builderClasses.forEach { builderClass ->
            builderClass
                .hookMethod("build")
                .suppressError()
                .runBeforeAndAfter(
                    beforeCallback = {
                        profileBuildState.set(ProfileBuildState(profileBuildState.get()))
                    },
                    afterCallback = {
                        val parent = profileBuildState.get()?.parent
                        if (parent == null) {
                            profileBuildState.remove()
                        } else {
                            profileBuildState.set(parent)
                        }
                    }
                )

            builderClass
                .hookMethod("createFolderProfile")
                .suppressError()
                .runAfter { param ->
                    val state = profileBuildState.get() ?: return@runAfter
                    param.result?.scaleFolderProfile(
                        folderIconAlreadyScaled = state.workspaceProfileCreated
                    )
                }
        }

        workspaceProfileClass
            .hookConstructor()
            .suppressError()
            .runBefore { param ->
                val state = profileBuildState.get() ?: return@runBefore
                if (state.workspaceProfileCreated) return@runBefore

                // Pixel Launcher Canary: WorkspaceProfile(float iconScale, int iconSizePx,
                // int iconTextSizePx, ...).
                val iconSizePx = param.args.getOrNull(1) as? Int ?: return@runBefore
                val iconTextSizePx = param.args.getOrNull(2) as? Int ?: return@runBefore

                param.args[1] = (iconSizePx * iconSizeModifier).toInt()
                param.args[2] = (iconTextSizePx * textSizeModifier).toInt()
                state.workspaceProfileCreated = true
            }

        allAppsProfileClass
            .hookConstructor()
            .suppressError()
            .runBefore { param ->
                val state = profileBuildState.get() ?: return@runBefore
                if (state.allAppsProfileCreated) return@runBefore

                // Pixel Launcher Canary: AllAppsProfile(Point, int cellHeightPx,
                // int iconSizePx, float iconTextSizePx, ...).
                val iconSizePx = param.args.getOrNull(2) as? Int ?: return@runBefore
                val iconTextSizePx = param.args.getOrNull(3) as? Float ?: return@runBefore

                param.args[2] = (iconSizePx * iconSizeModifier).toInt()
                param.args[3] = iconTextSizePx * textSizeModifier
                state.allAppsProfileCreated = true
            }
    }

    private fun Any.scaleLegacyDeviceProfile() {
        if (getFieldSilently("iconSizePx") !is Int) return

        listOf(
            "iconSizePx",
            "folderIconSizePx",
            "folderChildIconSizePx",
            "allAppsIconSizePx",
            "folderCellWidthPx",
            "folderCellHeightPx"
        ).forEach { fieldName ->
            scaleNumericField(fieldName, iconSizeModifier)
        }

        listOf(
            "iconTextSizePx",
            "folderLabelTextSizePx",
            "folderChildTextSizePx",
            "allAppsIconTextSizePx"
        ).forEach { fieldName ->
            scaleNumericField(fieldName, textSizeModifier)
        }
    }

    private fun Any.scaleFolderProfile(folderIconAlreadyScaled: Boolean) {
        listOf(
            "childIconSizePx",
            "cellWidthPx",
            "cellHeightPx"
        ).forEach { fieldName ->
            scaleNumericField(fieldName, iconSizeModifier)
        }

        if (!folderIconAlreadyScaled) {
            scaleNumericField("folderIconSizePx", iconSizeModifier)
        }

        listOf(
            "labelTextSizePx",
            "childTextSizePx"
        ).forEach { fieldName ->
            scaleNumericField(fieldName, textSizeModifier)
        }
    }

    private fun Any.scaleNumericField(fieldName: String, modifier: Float) {
        when (val value = getFieldSilently(fieldName)) {
            is Int -> setFieldSilently(fieldName, (value * modifier).toInt())
            is Float -> setFieldSilently(fieldName, value * modifier)
            is Double -> setFieldSilently(fieldName, value * modifier)
        }
    }
}
