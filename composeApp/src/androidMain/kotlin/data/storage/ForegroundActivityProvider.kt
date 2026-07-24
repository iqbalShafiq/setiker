package data.storage

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Tracks the currently resumed [Activity] and the last non-destroyed Activity so
 * off-screen Compose renders (and other window-attached work) can still attach when
 * the app is briefly backgrounded.
 */
internal object ForegroundActivityProvider {

    @Volatile
    private var resumedActivity: WeakReference<Activity>? = null

    @Volatile
    private var lastAliveActivity: WeakReference<Activity>? = null

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) {
                    resumedActivity = WeakReference(activity)
                    lastAliveActivity = WeakReference(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    lastAliveActivity = WeakReference(activity)
                }

                override fun onActivityPaused(activity: Activity) {
                    if (resumedActivity?.get() === activity) {
                        resumedActivity = null
                    }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    if (resumedActivity?.get() === activity) {
                        resumedActivity = null
                    }
                    if (lastAliveActivity?.get() === activity) {
                        lastAliveActivity = null
                    }
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            }
        )
    }

    fun current(): Activity? = resumedActivity?.get()?.takeUnless { it.isFinishing || it.isDestroyed }

    /** Last Activity that is still alive (may be stopped / not resumed). */
    fun lastAlive(): Activity? = lastAliveActivity?.get()?.takeUnless { it.isFinishing || it.isDestroyed }
}
