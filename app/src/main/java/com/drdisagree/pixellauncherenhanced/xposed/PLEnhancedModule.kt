package com.drdisagree.pixellauncherenhanced.xposed

import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.UserManager
import android.util.Log
import com.drdisagree.pixellauncherenhanced.BuildConfig
import com.drdisagree.pixellauncherenhanced.IRootProviderProxy
import com.drdisagree.pixellauncherenhanced.R
import com.drdisagree.pixellauncherenhanced.data.common.Constants.FRAMEWORK_PACKAGE
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.ResourceHookManager
import com.drdisagree.pixellauncherenhanced.xposed.mods.toolkit.XposedHook
import com.drdisagree.pixellauncherenhanced.xposed.utils.BootLoopProtector
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.Xprefs
import com.drdisagree.pixellauncherenhanced.xposed.utils.XPrefs.XprefsIsInitialized
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture

class PLEnhancedModule : XposedModule(), ServiceConnection {

    private lateinit var mContext: Context

    init {
        instance = this
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val packageName = param.packageName
        XposedHook.classLoader = param.defaultClassLoader
        val isChildProcess = try { packageName.contains(":") } catch (_: Throwable) { false }

        if (packageName == FRAMEWORK_PACKAGE) {
            try {
                val phoneWindowManagerClass = Class.forName(
                    "com.android.server.policy.PhoneWindowManager",
                    false,
                    param.defaultClassLoader
                )

                val initMethod = phoneWindowManagerClass.declaredMethods
                    .firstOrNull { it.name == "init" }
                    ?: phoneWindowManagerClass.methods
                        .firstOrNull { it.name == "init" }

                if (initMethod != null) {
                    hook(initMethod).intercept { chain ->
                        try {
                            if (!::mContext.isInitialized) {
                                mContext = chain.args[0] as Context

                                HookRes.modRes = mContext.createPackageContext(
                                    BuildConfig.APPLICATION_ID,
                                    Context.CONTEXT_IGNORE_SECURITY
                                ).resources

                                XPrefs.init(mContext)
                                ResourceHookManager.init(mContext)

                                CompletableFuture.runAsync {
                                    waitForXprefsLoad(packageName, param.defaultClassLoader)
                                }
                            }
                        } catch (throwable: Throwable) {
                            log(TAG, "Error in PhoneWindowManager.init hook: $throwable")
                        }
                        chain.proceed()
                    }
                }
            } catch (throwable: Throwable) {
                log(TAG, "Error hooking framework: $throwable")
            }
        } else {
            if (!isChildProcess) {
                try {
                    val instrumentationClass = Instrumentation::class.java

                    val newApplicationMethod = instrumentationClass.declaredMethods
                        .firstOrNull {
                            it.name == "newApplication" &&
                                    it.parameterTypes.size == 3 &&
                                    it.parameterTypes[0] == ClassLoader::class.java &&
                                    it.parameterTypes[1] == String::class.java
                        }

                    if (newApplicationMethod != null) {
                        hook(newApplicationMethod).intercept { chain ->
                            try {
                                if (!::mContext.isInitialized) {
                                    mContext = chain.args[2] as Context

                                    HookRes.modRes = mContext.createPackageContext(
                                        BuildConfig.APPLICATION_ID,
                                        Context.CONTEXT_IGNORE_SECURITY
                                    ).resources

                                    XPrefs.init(mContext)
                                    ResourceHookManager.init(mContext)

                                    waitForXprefsLoad(packageName, param.defaultClassLoader)
                                }
                            } catch (throwable: Throwable) {
                                log(TAG, "Error in newApplication hook: $throwable")
                            }
                            chain.proceed()
                        }
                    }
                } catch (throwable: Throwable) {
                    log(TAG, "Error setting up app hooks: $throwable")
                }
            }
        }
    }

    private fun onXPrefsReady(packageName: String, classLoader: ClassLoader) {
        if (!isChildProcess && BootLoopProtector.isBootLooped(packageName)) {
            log(TAG, "Possible crash in $packageName ; Module will not load for now...")
            return
        }

        loadModPacks(packageName, classLoader)
    }

    private fun loadModPacks(packageName: String, classLoader: ClassLoader) {
        if (HookRes.modRes
                .getStringArray(R.array.root_requirement)
                .toList()
                .contains(packageName)
        ) {
            forceConnectRootService()
        }

        for (mod in EntryList.getEntries(packageName)) {
            try {
                val modInstance = mod.getConstructor(Context::class.java).newInstance(mContext)

                if (XprefsIsInitialized) {
                    try {
                        modInstance.updatePrefs()
                    } catch (throwable: Throwable) {
                        log(TAG, "Failed to update prefs in ${mod.name}")
                        log(TAG, "$throwable")
                    }
                }

                modInstance.handleLoadPackage(packageName, classLoader)
                runningMods.add(modInstance)
            } catch (invocationTargetException: InvocationTargetException) {
                log(TAG, "Start Error Dump - Occurred in ${mod.name}")
                log(TAG, "${invocationTargetException.cause}")
            } catch (throwable: Throwable) {
                log(TAG, "Start Error Dump - Occurred in ${mod.name}")
                log(TAG, "$throwable")
            }
        }
    }

    private fun waitForXprefsLoad(packageName: String, classLoader: ClassLoader) {
        while (true) {
            try {
                Xprefs.getBoolean("LoadTestBooleanValue", false)
                break
            } catch (_: Throwable) {
                try {
                    Thread.sleep(1000.toLong())
                } catch (_: Throwable) {
                }
            }
        }

        log(TAG, "Version: ${BuildConfig.VERSION_NAME}")
        log(TAG, "Hooked $packageName")

        onXPrefsReady(packageName, classLoader)
    }

    private fun forceConnectRootService() {
        Thread {
            try {
                val mUserManager = mContext.getSystemService(Context.USER_SERVICE) as? UserManager

                while (mUserManager == null || !mUserManager.isUserUnlocked) {
                    Thread.sleep(2000)
                }

                Thread.sleep(5000)

                while (rootProxyIPC == null) {
                    connectRootService()
                    Thread.sleep(5000)
                }
            } catch (throwable: Throwable) {
                log(TAG, "Error in forceConnectRootService: $throwable")
            }
        }.start()
    }

    private fun connectRootService() {
        try {
            val intent = Intent().apply {
                component = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    "${BuildConfig.APPLICATION_ID}.services.RootProviderProxy"
                )
            }

            mContext.bindService(
                intent,
                instance!!,
                Context.BIND_AUTO_CREATE or Context.BIND_ADJUST_WITH_ACTIVITY
            )
        } catch (throwable: Throwable) {
            log(TAG, "Error connecting root service: $throwable")
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        rootProxyIPC = IRootProviderProxy.Stub.asInterface(service)

        synchronized(proxyQueue) {
            while (!proxyQueue.isEmpty()) {
                try {
                    proxyQueue.poll()!!.run(rootProxyIPC!!)
                } catch (_: Throwable) {
                }
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        rootProxyIPC = null
        forceConnectRootService()
    }

    fun interface ProxyRunnable {
        @Throws(RemoteException::class)
        fun run(proxy: IRootProviderProxy)
    }

    companion object {
        private const val TAG = "PLEnhanced"

        var module: PLEnhancedModule? = null
            private set

        private var _instance: WeakReference<PLEnhancedModule>? = null
        internal var instance: PLEnhancedModule?
            get() = _instance?.get()
            set(value) {
                _instance = value?.let { WeakReference(it) }
                module = value
            }

        val runningMods = ArrayList<ModPack>()
        var isChildProcess = false

        private var rootProxyIPC: IRootProviderProxy? = null
        private val proxyQueue: Queue<ProxyRunnable> = LinkedList()

        fun log(tag: String, message: Any?) {
            Log.i("[$TAG] $tag", message.toString())
        }

        fun enqueueProxyCommand(runnable: ProxyRunnable) {
            rootProxyIPC?.let {
                try {
                    runnable.run(it)
                } catch (_: RemoteException) {
                }
            } ?: run {
                synchronized(proxyQueue) {
                    proxyQueue.add(runnable)
                }

                instance!!.forceConnectRootService()
            }
        }
    }
}
