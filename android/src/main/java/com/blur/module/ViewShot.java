package com.blur.module;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.uimanager.NativeViewHierarchyManager;
import com.facebook.react.uimanager.UIBlock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Snapshot utility class allow to screenshot a view.
 */
public class ViewShot implements UIBlock
{

    static final String ERROR_UNABLE_TO_SNAPSHOT = "E_UNABLE_TO_SNAPSHOT";
    static final String TAG = "TouchIDManagerModule";

    private String extension;
    private Bitmap.CompressFormat format;
    private double quality;
    private Integer width;
    private Integer height;
    private File output;
    private Promise promise;
    private Boolean snapshotContentContainer;
    private ReactApplicationContext reactContext;
    private Activity currentActivity;
    private static final String BLURRED_IMAGE = "BLURRED_IMAGE";

    public ViewShot(
            String extension,
            Bitmap.CompressFormat format,
            double quality,
            @Nullable Integer width,
            @Nullable Integer height,
            File output,
            Boolean snapshotContentContainer,
            ReactApplicationContext reactContext,
            Activity currentActivity,
            Promise promise) {

        this.extension = extension;
        this.format = format;
        this.quality = quality;
        this.width = width;
        this.height = height;
        this.output = output;
        this.snapshotContentContainer = snapshotContentContainer;
        this.reactContext = reactContext;
        this.currentActivity = currentActivity;
        this.promise = promise;
    }

    @Override
    public void execute(NativeViewHierarchyManager nativeViewHierarchyManager) {

        OutputStream os = null;
        try {
            View view = currentActivity.getWindow().getDecorView().findViewById(android.R.id.content);
            if (view == null) {
                promise.reject(ERROR_UNABLE_TO_SNAPSHOT, "No view found with reactTag");
                return;
            }
            os = new FileOutputStream(output);
            captureView(view, os);
            String uri = Uri.fromFile(output).toString();
            storeBlurredImage(uri);
            promise.resolve(uri);
        } catch (Exception e) {
            e.printStackTrace();
            promise.reject(ERROR_UNABLE_TO_SNAPSHOT, "Failed to capture view snapshot");
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @ReactMethod
    private void storeBlurredImage(String uri) {

        Log.d(TAG, "BlurManagerModule storeBlurredImage: " + uri);

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.currentActivity.getBaseContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(BLURRED_IMAGE, uri);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "BlurManagerModule storeBlurredImage: " + e.toString());
        }
    }

    private List<View> getAllChildren(View v) {
        
        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<View>();

        ViewGroup viewGroup = (ViewGroup) v;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {

            View child = viewGroup.getChildAt(i);
            //Do not add any parents, just add child elements
            result.addAll(getAllChildren(child));
        }
        return result;
    }
    
    private void captureView(View view, OutputStream os) {

        try {
            int w = view.getWidth();
            int h = view.getHeight();
            if (w <= 0 || h <= 0) {
                throw new RuntimeException("Impossible to snapshot the view: view is invalid");
            }
    
            //evaluate real height
            if (snapshotContentContainer)
            {
                h = 0;
                ScrollView scrollView = (ScrollView) view;
                for (int i = 0; i < scrollView.getChildCount(); i++)
                {
                    h += scrollView.getChildAt(i).getHeight();
                }
            }
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
    
            Bitmap childBitmapBuffer;
            Canvas c = new Canvas(bitmap);
            view.draw(c);
    
            //after view is drawn, go through children
            List<View> childrenList = getAllChildren(view);
    
            for (View child : childrenList)
            {
                if (child instanceof TextureView)
                {
                    ((TextureView) child).setOpaque(false);
                    childBitmapBuffer = ((TextureView) child).getBitmap(child.getWidth(), child.getHeight());
    
                    int left = child.getLeft();
                    int top = child.getTop();
                    View parentElem = (View) child.getParent();
                    while (parentElem != null)
                    {
                        if (parentElem == view)
                        {
                            break;
                        }
                        left += parentElem.getLeft();
                        top += parentElem.getTop();
                        parentElem = (View) parentElem.getParent();
                    }
                    c.drawBitmap(childBitmapBuffer, left + child.getPaddingLeft(), top + child.getPaddingTop(), null);
                }
            }
    
            if (width != null && height != null && (width != w || height != h)) {
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }
            if (bitmap == null) {
                throw new RuntimeException("Impossible to snapshot the view");
            }
            bitmap = BlurBuilder.blur(this.currentActivity, bitmap);
            bitmap = BlurBuilder.blur(this.currentActivity, bitmap);
            bitmap = BlurBuilder.blur(this.currentActivity, bitmap);
            // bitmap = makeTransparent(bitmap, 200);
            bitmap.compress(format, (int) (100.0 * quality), os);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "captureView: ");   
        }
    }
}
