package com.blur.module;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;

import com.facebook.react.bridge.GuardedAsyncTask;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.uimanager.UIManagerModule;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public class BlurManagerModule extends ReactContextBaseJavaModule implements LifecycleEventListener
{
    // ATRIBUTES ===================================================================================

    private final ReactApplicationContext reactContext;
    public static final String TAG = "BlurManager";
    private static final String HIDE_CONTENT_WHEN_APPLICATION_INACTIVE = "HIDE_CONTENT";
    private static final String BLURRED_IMAGE = "BLURRED_IMAGE";

    // CONSTRUCTOR =================================================================================

    public BlurManagerModule(ReactApplicationContext reactContext)
    {
        super(reactContext);
        this.reactContext = reactContext;
    }

    // METHODS =====================================================================================

    @Override
    public String getName()
    {
        return "BlurManagerModule";
    }

    @Override
    public Map<String, Object> getConstants()
    {
        return Collections.emptyMap();
    }

    @Override
    public void onCatalystInstanceDestroy()
    {
        super.onCatalystInstanceDestroy();
        new CleanTask(getReactApplicationContext()).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @ReactMethod
    public void releaseCapture(String uri)
    {
        final String path = Uri.parse(uri).getPath();
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (parent.equals(reactContext.getExternalCacheDir()) || parent.equals(reactContext.getCacheDir())) {
            file.delete();
        }
    }

    @ReactMethod
    public void hideContentWhenApplicationInactive(Boolean enable) {

        Log.d("TouchIDManagerModule", "BlurManagerModule hideContentWhenApplicationInactive: " + enable);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getReactApplicationContext().getBaseContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HIDE_CONTENT_WHEN_APPLICATION_INACTIVE, enable);
        editor.apply();
    }

    @ReactMethod
    public void getBlurredImage(Promise promise) {

        Log.d("TouchIDManagerModule", "BlurManagerModule getBlurredImage");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getReactApplicationContext().getBaseContext());
        promise.resolve(preferences.getString(BLURRED_IMAGE, ""));
    }

    @ReactMethod
    public void captureScreen(ReadableMap options, Promise promise)
    {
        ReactApplicationContext context = getReactApplicationContext();
        String format = options.getString("format");
        Bitmap.CompressFormat compressFormat = format.equals("jpg") ? Bitmap.CompressFormat.JPEG : format.equals("webm") ? Bitmap.CompressFormat.WEBP : Bitmap.CompressFormat.PNG;

        double quality = options.getDouble("quality");
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        Integer width = options.hasKey("width") ? (int) (displayMetrics.density * options.getDouble("width")) : null;
        Integer height = options.hasKey("height") ? (int) (displayMetrics.density * options.getDouble("height")) : null;
        Boolean snapshotContentContainer = options.getBoolean("snapshotContentContainer");

        try {
            File file = createTempFile(getReactApplicationContext(), format);
            UIManagerModule uiManager = this.reactContext.getNativeModule(UIManagerModule.class);
            uiManager.addUIBlock(new ViewShot(format, compressFormat, quality, width, height, file, snapshotContentContainer, reactContext, getCurrentActivity(), promise));
        } catch (Exception e) {
            e.printStackTrace();
            promise.reject(ViewShot.ERROR_UNABLE_TO_SNAPSHOT, "Failed to snapshot view");
        }
    }

    private static final String TEMP_FILE_PREFIX = "ReactNative-snapshot-image";

    @Override
    public void onHostResume() {

        Log.d(TAG, "onHostResume: ");
    }

    @Override
    public void onHostPause() {

        Log.d(TAG, "onHostPause: ");
    }

    @Override
    public void onHostDestroy() {

        Log.d(TAG, "onHostDestroy: ");
    }

    /**
     * Asynchronous task that cleans up cache dirs (internal and, if available, external) of cropped
     * image files. This is run when the catalyst instance is being destroyed (i.e. app is shutting
     * down) and when the module is instantiated, to handle the case where the app crashed.
     */
    private static class CleanTask extends GuardedAsyncTask<Void, Void>
    {
        private final Context mContext;

        private CleanTask(ReactContext context) {
            super(context);
            mContext = context;
        }

        @Override
        protected void doInBackgroundGuarded(Void... params) {

            cleanDirectory(mContext.getCacheDir());
            File externalCacheDir = mContext.getExternalCacheDir();
            if (externalCacheDir != null) {
                cleanDirectory(externalCacheDir);
            }
        }

        private void cleanDirectory(File directory) {

            File[] toDelete = directory.listFiles(
                new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String filename) {
                        return filename.startsWith(TEMP_FILE_PREFIX);
                    }
                });
            if (toDelete != null) {
                for (File file : toDelete) {
                    file.delete();
                }
            }
        }
    }

    /**
     * Create a temporary file in the cache directory on either internal or external storage,
     * whichever is available and has more free space.
     */
    private File createTempFile(Context context, String ext) throws IOException {

        File externalCacheDir = context.getExternalCacheDir();
        File internalCacheDir = context.getCacheDir();
        File cacheDir;
        if (externalCacheDir == null && internalCacheDir == null) {
            throw new IOException("No cache directory available");
        }
        if (externalCacheDir == null) {
            cacheDir = internalCacheDir;
        } else if (internalCacheDir == null) {
            cacheDir = externalCacheDir;
        } else {
            cacheDir = externalCacheDir.getFreeSpace() > internalCacheDir.getFreeSpace() ? externalCacheDir : internalCacheDir;
        }
        String suffix = "." + ext;
        File tmpFile = File.createTempFile(TEMP_FILE_PREFIX, suffix, cacheDir);
        return tmpFile;
    }

    // SEND EVENT ==================================================================================

    // CLASS =======================================================================================
}
