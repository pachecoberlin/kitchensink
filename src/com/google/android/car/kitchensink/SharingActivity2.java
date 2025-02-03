package com.google.android.car.kitchensink;

import android.app.Activity;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.SurfaceControl;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManagerGlobal;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

public class SharingActivity2 extends Activity {
    private DisplayManager mDisplayManager;
    private DisplayLockHelper mDisplayLockHelper;
    private boolean mMirroring = false;
    private SurfaceControl mMirrorSurfaceControl1;
    private int mOtherDisplayId;
    private int mSelectedUserId;
    private View mStopMirrorBtn;
    private SurfaceView mSurfaceView1;
    private UserManager mUserManager;
    private String TAG = SharingActivity2.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = this.getSystemService(UserManager.class);
        mDisplayManager = this.getSystemService(DisplayManager.class);
        setOtherUser();

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
        ViewCompat.setTranslationZ(lock, 100); // lock will be in front of button
        lock.setZ(100);
        ViewCompat.setTranslationZ(touchable, 100); // text will be in front of button
        touchable.setZ(100);
        ViewCompat.setTranslationZ(padi, -100);
        padi.setZ(-100);

        mSurfaceView1 = findViewById(R.id.surface_view_);
        mSurfaceView1.setZOrderOnTop(true);  // SurfaceView should be placed over the App.
        mStopMirrorBtn = findViewById(R.id.stopmirror);
        mStopMirrorBtn.setEnabled(false);
        mStopMirrorBtn.setVisibility(View.INVISIBLE);
        mStopMirrorBtn.setOnClickListener(this::stopMirroring);
        findViewById(R.id.startmirror).setOnClickListener(this::startMirroring);
        findViewById(R.id.serve).setOnClickListener(this::serve);
        findViewById(R.id.sharelink).setOnClickListener(this::share);
        findViewById(R.id.lock).setOnClickListener(this::switchLock);

        ArrayList<Integer> displays = getDisplays();
        displays.remove((Object) this.getDisplayId());
        mOtherDisplayId = displays.getFirst();
        mDisplayLockHelper = new DisplayLockHelper();
        updateLockBtn();
    }

    private void updateLockBtn() {
        if (mDisplayLockHelper.init(this, mOtherDisplayId)) {
            findViewById(R.id.lock).setBackgroundResource(R.drawable.locked);
            ((TextView) findViewById(R.id.touchable)).setText(R.string.untouchable);
        } else {
            findViewById(R.id.lock).setBackgroundResource(R.drawable.unlocked);
            ((TextView) findViewById(R.id.touchable)).setText(R.string.touchable);
        }
    }

    private void stopMirroring(View view) {
        Log.d(TAG, "stopMirroring");
        releaseMirroredSurfaces();
        mMirroring = false;
        updateUi();
    }

    private void startMirroring(View view) {
        Log.d(TAG, "startMirroring");
        mMirrorSurfaceControl1 = mirrorDisplayOnSurfaceView(mOtherDisplayId, mSurfaceView1);
        if (mMirrorSurfaceControl1 == null) {
            Log.d(TAG, "Error while mirroring.");
            Toast.makeText(this, "Error while mirroring.", Toast.LENGTH_SHORT);
            return;
        }
        mMirroring = true;
        updateUi();
    }

    private void serve(View view) {
        Log.d(TAG, "Serving, trying to start intent for userId: " + mSelectedUserId);
        String rrPackageName = "com.example.android.cars.roadreels";
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setClassName(rrPackageName, rrPackageName + ".MainActivity")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("detail", true);
        try {
            this.startActivityAsUser(intent, UserHandle.of(mSelectedUserId));
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Road Reels for user " + mSelectedUserId, e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void share(View view) {
        Log.d(TAG, "Recommandation, trying to send intent to userId: " + mSelectedUserId);
        String sharingPackageName = "de.pacheco.sharing";
        Intent intent = new Intent(Intent.ACTION_VIEW)
                .setClassName(sharingPackageName, sharingPackageName + ".MainActivity")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra("linksharing", true);
        try {
            this.startActivityAsUser(intent, UserHandle.of(mSelectedUserId));
        } catch (Exception e) {
            Log.e(TAG, "Failed to start Sharing Use Cases App for user " + mSelectedUserId, e);
            Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void switchLock(View view) {
        Log.d(TAG, "lock, mOtherDisplayId: " + mOtherDisplayId);
        mDisplayLockHelper.switchLock(mOtherDisplayId, this);
        updateLockBtn();
    }

    private void setOtherUser() {

        int currentUserId = UserHandle.myUserId();
        Log.d(TAG, "currentUserId: " + currentUserId);
        ArrayList<Integer> userIds = new ArrayList<>();
        Set<UserHandle> visibleUsers = mUserManager.getVisibleUsers();
        for (Iterator<UserHandle> iterator = visibleUsers.iterator(); iterator.hasNext(); ) {
            UserHandle userHandle = iterator.next();
            int userId = userHandle.getIdentifier();
            if (userId == currentUserId) {
                // Skip the current user (driver).
                continue;
            }
            userIds.add(userId);
        }
        mSelectedUserId = userIds.getFirst();
    }

    private ArrayList<Integer> getDisplays() {
        ArrayList<Integer> displayIds = new ArrayList<>();
        Display[] displays = mDisplayManager.getDisplays();
        int uidSelf = Process.myUid();
        for (Display disp : displays) {
            if (!disp.hasAccess(uidSelf)) {
                continue;
            }
            displayIds.add(disp.getDisplayId());
        }
        return displayIds;
    }

    private SurfaceControl mirrorDisplayOnSurfaceView(int displayId, SurfaceView surfaceView) {
        SurfaceControl mirroredSurfaceControl = mirrorDisplay(displayId);
        if (mirroredSurfaceControl == null || !mirroredSurfaceControl.isValid()) {
            Log.e(TAG, "Failed to mirror display = " + displayId);
            return null;
        }
        Display display = mDisplayManager.getDisplay(displayId);
        DisplayInfo displayInfo = new DisplayInfo();
        display.getDisplayInfo(displayInfo);
        float scaleX = (float) surfaceView.getWidth() / displayInfo.appWidth;
        float scaleY = (float) surfaceView.getHeight() / displayInfo.appHeight;
        float scale = Math.min(scaleX, scaleY);

        SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();
        mTransaction
                .show(mirroredSurfaceControl)
                .setScale(mirroredSurfaceControl, scale, scale)
                .reparent(mirroredSurfaceControl, surfaceView.getSurfaceControl())
                .setLayer(mirroredSurfaceControl, 1)  // Place the mirrorSurface over SurfaceView.
                .apply();
        return mirroredSurfaceControl;
    }

    private SurfaceControl mirrorDisplay(final int displayId) {
        try {
            SurfaceControl outSurfaceControl = new SurfaceControl();
            WindowManagerGlobal.getWindowManagerService().mirrorDisplay(displayId,
                    outSurfaceControl);
            return outSurfaceControl;
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to reach window manager", e);
        }
        return null;
    }

    private void releaseMirroredSurfaces() {
        SurfaceControl.Transaction mTransaction = new SurfaceControl.Transaction();
        if (mMirrorSurfaceControl1 != null) {
            mTransaction.remove(mMirrorSurfaceControl1);
        }
        mTransaction.apply();
        mMirrorSurfaceControl1 = null;
    }

    private void updateUi() {
        mStopMirrorBtn.setEnabled(mMirroring);
        int visibility;
        if (mMirroring) {
            visibility = View.VISIBLE;
        } else {
            visibility = View.INVISIBLE;
        }
        mStopMirrorBtn.setVisibility(visibility);
    }
}
