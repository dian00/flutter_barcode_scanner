package com.amolg.flutterbarcodescanner;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;

import java.util.Map;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.EventChannel;

public class FlutterBarcodeScannerPlugin implements FlutterPlugin, ActivityAware, MethodChannel.MethodCallHandler, EventChannel.StreamHandler {

    private static final String CHANNEL = "flutter_barcode_scanner";
    private static final String STREAM = "flutter_barcode_scanner_receiver";
    private static final int RC_BARCODE_CAPTURE = 9001;

    private static final String TAG = FlutterBarcodeScannerPlugin.class.getSimpleName();

    public static String lineColor = "#DC143C";
    public static boolean isShowFlashIcon = false;
    public static boolean isContinuousScan = false;

    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private Activity activity;
    private static EventChannel.EventSink barcodeStream;
    private static Result pendingResult;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL);
        eventChannel = new EventChannel(binding.getBinaryMessenger(), STREAM);

        methodChannel.setMethodCallHandler(this);
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);

        methodChannel = null;
        eventChannel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        activity = binding.getActivity();
        binding.addActivityResultListener((requestCode, resultCode, data) -> onActivityResult(requestCode, resultCode, data));
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        activity = null;
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("scanBarcode")) {
            handleScanBarcode(call, result);
        } else {
            result.notImplemented();
        }
    }

    private void handleScanBarcode(MethodCall call, Result result) {
        try {
            pendingResult = result;
            Map<String, Object> arguments = call.arguments();

            // Extract arguments
            lineColor = (String) arguments.getOrDefault("lineColor", "#DC143C");
            isShowFlashIcon = (boolean) arguments.getOrDefault("isShowFlashIcon", false);
            isContinuousScan = (boolean) arguments.getOrDefault("isContinuousScan", false);
            String cancelButtonText = (String) arguments.get("cancelButtonText");

            Intent intent = new Intent(activity, BarcodeCaptureActivity.class);
            intent.putExtra("cancelButtonText", cancelButtonText);
            activity.startActivityForResult(intent, RC_BARCODE_CAPTURE);

        } catch (Exception e) {
            Log.e(TAG, "Error in handleScanBarcode: " + e.getMessage());
            result.error("ERROR", "Failed to start barcode scanner", null);
        }
    }

    private boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE && pendingResult != null) {
            if (resultCode == CommonStatusCodes.SUCCESS && data != null) {
                Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                if (barcode != null) {
                    pendingResult.success(barcode.rawValue);
                } else {
                    pendingResult.success("-1");
                }
            } else {
                pendingResult.success("-1");
            }
            pendingResult = null;
            return true;
        }
        return false;
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        barcodeStream = events;
    }

    @Override
    public void onCancel(Object arguments) {
        barcodeStream = null;
    }

    public static void onBarcodeScanReceiver(final Barcode barcode) {
        if (barcode != null && barcodeStream != null) {
            barcodeStream.success(barcode.rawValue);
        }
    }
}
