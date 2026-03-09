package com.example.accessbuttons

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.ContextThemeWrapper
import android.view.animation.DecelerateInterpolator
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

class VolumeService : Service() {

    private enum class SliderMode {
        SYSTEM_UI,
        CUSTOM
    }

    companion object {
        const val ACTION_START = "com.example.accessbuttons.action.START"
        const val ACTION_STOP = "com.example.accessbuttons.action.STOP"
        const val ACTION_RESET_STATE = "com.example.accessbuttons.action.RESET_STATE"
        const val ACTION_SET_MODE_SYSTEM = "com.example.accessbuttons.action.SET_MODE_SYSTEM"
        const val ACTION_SET_MODE_CUSTOM = "com.example.accessbuttons.action.SET_MODE_CUSTOM"

        private const val NOTIFICATION_CHANNEL_ID = "volume_overlay_channel"
        private const val NOTIFICATION_ID = 2001
        private const val DEFAULT_X_DP = 24
        private const val DEFAULT_Y_DP = 180

        @Volatile
        private var isServiceActive = false

        fun isActive(): Boolean = isServiceActive
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val sliderMode = MutableStateFlow(SliderMode.SYSTEM_UI)

    private lateinit var windowManager: WindowManager
    private lateinit var audioManager: AudioManager
    private var floatingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action ?: ACTION_START) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RESET_STATE -> {
                resetOverlayState()
                return START_STICKY
            }
            ACTION_SET_MODE_SYSTEM -> sliderMode.value = SliderMode.SYSTEM_UI
            ACTION_SET_MODE_CUSTOM -> sliderMode.value = SliderMode.CUSTOM
        }

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()
        attachOverlayIfNeeded()
        return START_STICKY
    }

    override fun onDestroy() {
        isServiceActive = false
        removeOverlay()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VolumeService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.stop_service), stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun attachOverlayIfNeeded() {
        if (floatingView != null) return

        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dpToPx(DEFAULT_X_DP)
            y = dpToPx(DEFAULT_Y_DP)
        }

        val themedContext = ContextThemeWrapper(this, R.style.Theme_AccessButtons)
        val root = LayoutInflater.from(themedContext).inflate(R.layout.floating_layout, null)
        val container = root.findViewById<View>(R.id.floatingContainer)
        val upButton = root.findViewById<View>(R.id.btnVolumeUp)
        val downButton = root.findViewById<View>(R.id.btnVolumeDown)

        val rootDragTouch = createDragOrTapTouchListener(params, onTap = null)
        val upTouch = createDragOrTapTouchListener(params) {
            upButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            adjustMusicVolume(+1)
        }
        val downTouch = createDragOrTapTouchListener(params) {
            downButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            adjustMusicVolume(-1)
        }

        container.setOnTouchListener(rootDragTouch)
        upButton.setOnTouchListener(upTouch)
        downButton.setOnTouchListener(downTouch)

        floatingView = root
        layoutParams = params
        windowManager.addView(root, params)
    }

    private fun createDragOrTapTouchListener(
        params: WindowManager.LayoutParams,
        onTap: (() -> Unit)?
    ): View.OnTouchListener {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        return View.OnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY

                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        updateLayoutSafely(params)
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDragging) {
                        snapToNearestEdge(params)
                    } else {
                        onTap?.invoke()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun snapToNearestEdge(params: WindowManager.LayoutParams) {
        val width = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            resources.displayMetrics.widthPixels
        }
        val floatingWidth = floatingView?.width ?: dpToPx(164)
        val edgeMargin = dpToPx(12)
        val leftEdgeX = edgeMargin
        val rightEdgeX = width - floatingWidth - edgeMargin
        val targetX = if (params.x + (floatingWidth / 2) < width / 2) leftEdgeX else rightEdgeX

        ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 220L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                params.x = animator.animatedValue as Int
                updateLayoutSafely(params)
            }
            start()
        }
    }

    private fun adjustMusicVolume(delta: Int) {
        serviceScope.launch(Dispatchers.Default) {
            val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (current + delta).coerceIn(0, max)
            val flags = when (sliderMode.value) {
                SliderMode.SYSTEM_UI -> AudioManager.FLAG_SHOW_UI
                SliderMode.CUSTOM -> 0
            }
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, flags)
        }
    }

    private fun updateLayoutSafely(params: WindowManager.LayoutParams) {
        floatingView?.let {
            runCatching { windowManager.updateViewLayout(it, params) }
        }
    }

    private fun resetOverlayState() {
        sliderMode.value = SliderMode.SYSTEM_UI
        val params = layoutParams
        if (params != null) {
            params.x = dpToPx(DEFAULT_X_DP)
            params.y = dpToPx(DEFAULT_Y_DP)
            updateLayoutSafely(params)
            removeOverlay()
            attachOverlayIfNeeded()
        }
    }

    private fun removeOverlay() {
        floatingView?.let {
            runCatching { windowManager.removeView(it) }
        }
        floatingView = null
        layoutParams = null
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()
}
