package com.google.android.car.kitchensink;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

public class SharingActivity2 extends Activity {
    private SurfaceView mSurfaceView1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sharing);
        WindowInsetsController controller = getWindow().getInsetsController();
        if (controller != null) {
            controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        ImageButton lock = findViewById(R.id.lock);
        TextView touchable = findViewById(R.id.touchable);
        Button padi = findViewById(R.id.padi);
        // Set the Z-order
        ViewCompat.setTranslationZ(lock, 100); // button2 will be in front of button1
        lock.setZ(100);
        ViewCompat.setTranslationZ(touchable, 100); // button2 will be in front of button1
        touchable.setZ(100);
        ViewCompat.setTranslationZ(padi, -100);
        padi.setZ(-100);

        mSurfaceView1 = findViewById(R.id.surface_view_);
        mSurfaceView1.setZOrderOnTop(true);  // SurfaceView should be placed over the App.

    }
}
