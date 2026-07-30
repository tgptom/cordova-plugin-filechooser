package com.megster.cordova;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileChooser extends CordovaPlugin {

    private static final String TAG = "FileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;

    public static final String MIME = "mime";

    private CallbackContext callback;
    private boolean chooserRequestPending;

    @Override
    public boolean execute(String action, JSONArray inputs, CallbackContext callbackContext) throws JSONException {

        if (action.equals(ACTION_OPEN)) {
            JSONObject filters = inputs != null ? inputs.optJSONObject(0) : null;
            chooseFile(filters, callbackContext);
            return true;
        }

        return false;
    }

    public void chooseFile(JSONObject filter, CallbackContext callbackContext) {
        if (callback != null) {
            callbackContext.error("File chooser already in progress.");
            return;
        }

        String uriFilter = "*/*";
        if (filter != null && filter.has(MIME)) {
            String requestedMime = filter.optString(MIME, "*/*");
            if (requestedMime != null && !requestedMime.trim().isEmpty()) {
                uriFilter = requestedMime;
            }
        }

        // type and title should be configurable

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType(uriFilter);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        Intent chooser = Intent.createChooser(intent, "Select File");
        callback = callbackContext;
        chooserRequestPending = true;

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);

        try {
            cordova.startActivityForResult(this, chooser, PICK_FILE_REQUEST);
        } catch (Exception ex) {
            Log.e(TAG, "Failed to open file chooser", ex);
            finishWithError("Failed to open file chooser.");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_FILE_REQUEST) {
            return;
        }

        if (callback == null) {
            chooserRequestPending = false;
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data != null ? data.getData() : null;
            if (uri != null) {
                persistUriPermission(uri, data);
                Log.d(TAG, "File selected: " + uri);
                callback.success(uri.toString());
                clearPendingCallback();
            } else {
                finishWithError("File uri was null");
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            // keep this string the same as in iOS document picker plugin
            // https://github.com/iampossible/Cordova-DocPicker
            finishWithError("User canceled.");
        } else {
            finishWithError(resultCode);
        }
    }

    @Override
    public Bundle onSaveInstanceState() {
        if (!chooserRequestPending) {
            return null;
        }

        Bundle state = new Bundle();
        state.putBoolean("chooserRequestPending", true);
        return state;
    }

    @Override
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        if (state == null || !state.getBoolean("chooserRequestPending", false)) {
            return;
        }

        callback = callbackContext;
        chooserRequestPending = true;
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onReset() {
        if (callback != null) {
            finishWithError("User canceled.");
        }
    }

    @Override
    public void onDestroy() {
        if (callback != null) {
            finishWithError("User canceled.");
        }
    }

    private void persistUriPermission(Uri uri, Intent data) {
        if (data == null) {
            return;
        }

        int grantFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        grantFlags |= Intent.FLAG_GRANT_READ_URI_PERMISSION;
        try {
            cordova.getActivity().getContentResolver().takePersistableUriPermission(uri, grantFlags);
        } catch (SecurityException | IllegalArgumentException ex) {
            Log.w(TAG, "Could not persist URI permission for " + uri, ex);
        }
    }

    private void finishWithError(String message) {
        if (callback != null) {
            callback.error(message);
        }
        clearPendingCallback();
    }

    private void finishWithError(int errorCode) {
        if (callback != null) {
            callback.error(errorCode);
        }
        clearPendingCallback();
    }

    private void clearPendingCallback() {
        callback = null;
        chooserRequestPending = false;
    }
}
