package com.sajjad.overlaycalculator;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE = 1222;

    @Override
    protected void onCreate(Bundle Bundle) {
        super.onCreate(Bundle);

        // التحقق من صلاحية العرض فوق التطبيقات للأندرويد
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE);
        } else {
            startFloatingService();
        }
    }

    private void startFloatingService() {
        startService(new Intent(MainActivity.this, FloatingService.class));
        finish(); // إغلاق الواجهة فوراً ليبقى الزر العائم فقط
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingService();
            } else {
                Toast.makeText(this, "الصلاحية ضرورية لتشغيل الآلة الحاسبة العائمة!", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
