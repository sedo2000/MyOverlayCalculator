package com.sajjad.overlaycalculator;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private View closeZoneView;
    private WindowManager.LayoutParams floatParams;
    private WindowManager.LayoutParams closeParams;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        // 1. إعداد واجهة الزر العائم (التي تحتوي على الـ WebView لكود الـ HTML الخاص بك)
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null);

        // تحديد إعدادات النافذة لتظهر فوق كل شيء
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        floatParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        floatParams.gravity = Gravity.TOP | Gravity.START;
        floatParams.x = 100;
        floatParams.y = 100;

        // 2. إعداد واجهة منطقة الإغلاق (Close Zone) أسفل الشاشة
        closeZoneView = LayoutInflater.from(this).inflate(R.layout.layout_close_zone, null);
        closeParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        
        closeParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        closeZoneView.setVisibility(View.GONE); // مخفية في البداية

        windowManager.addView(closeZoneView, closeParams);
        windowManager.addView(floatingView, floatParams);

        // تشغيل كود الـ HTML الحالي داخل الـ WebView
        WebView webView = floatingView.findViewById(R.id.calculator_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///index.html");
        // يقرأ ملفك مباشرة

        // 3. إضافة ميزة السحب السلس والرمي في زر Close
        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        final View bubble = floatingView.findViewById(R.id.bubble_icon);
        
        bubble.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = floatParams.x;
                        initialY = floatParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        
                        // إظهار زر الحذف أسفل الشاشة عند بدء التحريك
                        closeZoneView.setVisibility(View.VISIBLE);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        floatParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        floatParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, floatParams);

                        // التحقق إذا اقترب الزر العائم من أسفل الشاشة (منطقة الإغلاق)
                        if (event.getRawY() > (getResources().getDisplayMetrics().heightPixels - 250)) {
                            closeZoneView.setScaleX(1.2f);
                            closeZoneView.setScaleY(1.2f);
                        } else {
                            closeZoneView.setScaleX(1.0f);
                            closeZoneView.setScaleY(1.0f);
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        // إخفاء زر الحذف
                        closeZoneView.setVisibility(View.GONE);

                        // إذا تم إفلات الزر العائم أسفل الشاشة (فوق زر الحذف تماماً)
                        if (event.getRawY() > (getResources().getDisplayMetrics().heightPixels - 250)) {
                            // إنهاء الخدمة فوراً وحذف النوافذ من الشاشة
                            stopSelf();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // تنظيف الشاشة عند الإغلاق التام
        if (floatingView != null) windowManager.removeView(floatingView);
        if (closeZoneView != null) windowManager.removeView(closeZoneView);

        // السطر السحري: قتل عملية التطبيق وحذفه نهائياً من قائمة التطبيقات المعلقة الخلفية (Recents)
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }
}
