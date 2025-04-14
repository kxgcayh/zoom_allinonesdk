package com.mx.zoom_allinonesdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.Result;
import us.zoom.sdk.CustomizedNotificationData;
import us.zoom.sdk.InMeetingNotificationHandle;
import us.zoom.sdk.JoinMeetingOptions;
import us.zoom.sdk.JoinMeetingParams;
import us.zoom.sdk.MeetingParameter;
import us.zoom.sdk.MeetingService;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.InMeetingService;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.StartMeetingOptions;
import us.zoom.sdk.StartMeetingParams4NormalUser;
import us.zoom.sdk.ZoomAuthenticationError;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.ZoomSDK;
import us.zoom.sdk.ZoomSDKAuthenticationListener;
import us.zoom.sdk.ZoomSDKInitParams;
import us.zoom.sdk.ZoomSDKInitializeListener;
import us.zoom.sdk.MeetingViewsOptions;
import us.zoom.sdk.StartMeetingParamsWithoutLogin;
import us.zoom.sdk.InMeetingAudioController;
import us.zoom.sdk.InMeetingRemoteController;
import us.zoom.sdk.MobileRTCSDKError;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.mx.zoom_allinonesdk.constants.ZoomConstants;
import com.mx.zoom_allinonesdk.ZoomMeetingListener;

public class ZoomAllInOneSdkPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ZoomSDKInitializeListener, ActivityAware {

    private MethodChannel channel;
    private Activity activity;
    private EventChannel meetingStatusChannel;
    private Context context;
    private ZoomMeetingListener meetingListener;
    private ZoomSDK zoomSDK = ZoomSDK.getInstance();

    @Override
    public void onAttachedToEngine(FlutterPlugin.FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "zoom_allinonesdk");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();

        meetingStatusChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "zoom_allinonesdk/flutter_zoom_meeting_event_stream");
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case ZoomConstants.INIT_ZOOM:
                initZoom(call, result);
                break;
            case ZoomConstants.JOIN_MEETING:
                joinMeeting(call, result);
                break;
            case ZoomConstants.START_MEETING:
                startMeeting(call, result);
                break;
            case ZoomConstants.STATUS_MEETING:
                statusMeeting(result);
                break;
            default:
                result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(FlutterPlugin.FlutterPluginBinding binding) {
        Log.d("ZoomAllInOneSdkPlugin", "onDetachedFromEngine");
        channel.setMethodCallHandler(null);
    }

    // Initialize Zoom SDK
    private void initZoom(MethodCall call, Result result) {
        Map<String, String> options = call.arguments();

        // Check for required parameters
        if (options.get(ZoomConstants.JWT_TOKEN) == null) {
            result.error("JWT_TOKEN_NULL", "JWT Token cannot be null", null);
            return;
        }

        // Check if Zoom SDK is already initialized
        if (zoomSDK.isInitialized()) {
            List<Integer> response = Arrays.asList(0, 0);
            result.success(response);
            return;
        }

        // Configure ZoomSDKInitParams
        ZoomSDKInitParams initParams = new ZoomSDKInitParams();
        initParams.jwtToken = options.get(ZoomConstants.JWT_TOKEN);
        initParams.domain = options.get(ZoomConstants.DOMAIN);
        initParams.enableLog = true;

        // Initialize Zoom SDK
        ZoomSDKInitializeListener zoomSDKInitializeListener = new ZoomSDKInitializeListener() {
            @Override
            public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
                MeetingService meetingService = zoomSDK.getMeetingService();
                meetingStatusChannel.setStreamHandler(new StatusStreamHandler(meetingService));

                handleZoomSDKInitializationResult(errorCode, internalErrorCode, result);
            }

            @Override
            public void onZoomAuthIdentityExpired() {
                Log.d("ZoomAllInOneSdkPlugin", "Zoom authentication identity expired.");
            }
        };

        zoomSDK.initialize(context, zoomSDKInitializeListener, initParams);
    }

    // Handle Zoom SDK Initialization Result
    private void handleZoomSDKInitializationResult(int errorCode, int internalErrorCode, Result result) {
        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
            Toast.makeText(activity, "Failed Download Zoom. Error: " + errorCode + ", internalErrorCode=" + internalErrorCode, Toast.LENGTH_LONG).show();
            result.error("SDK_INIT_ERROR", "Failed to initialize Zoom SDK", errorCode);
        } else {
            List<Integer> response = Arrays.asList(0, 0);
            result.success(response);
        }
    }

    // Join a Zoom meeting
    private void joinMeeting(MethodCall methodCall, Result result) {
        Map<String, String> options = methodCall.arguments();

        // Check if Zoom SDK is initialized
        if (!zoomSDK.isInitialized()) {
            Log.e("ZoomAllInOneSdkPlugin", "Zoom SDK is not initialized");
            result.success(false);
            return;
        }

        toggleMeetingServiceListener(true);

        // Get MeetingService instance
        MeetingService meetingService = zoomSDK.getMeetingService();

        // Check if a meeting is already in progress
        MeetingStatus meetingStatus = meetingService.getMeetingStatus();
        Log.d("ZoomAllInOneSdkPlugin", "Meeting status is " + meetingStatus);

        if (meetingStatus == MeetingStatus.MEETING_STATUS_WAITINGFORHOST || meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
            Log.d("ZoomAllInOneSdkPlugin", "Meeting is already in progress. Redirecting to the meeting.");
            // If a meeting is already in progress, show the meeting activity
            meetingService.returnToMeeting(activity);
            InMeetingService inMeetingService = zoomSDK.getInMeetingService();
            InMeetingRemoteController remoteController = inMeetingService.getInMeetingRemoteController();
            remoteController.grabRemoteControl();
            // Log.d("ZoomAllInOneSdkPlugin", "grabRemoteControl(): " + grab);
            remoteController.startRemoteControl();
            // Log.d("ZoomAllInOneSdkPlugin", "startRemoteControl(): " + start);
            Log.d("ZoomAllInOneSdkPlugin", "Redirecting to the ongoing meeting.");
            result.success(true);
            return;
        }

        // Configure JoinMeetingOptions and JoinMeetingParams
        JoinMeetingOptions opts = new JoinMeetingOptions();

        opts.no_invite = parseBoolean(options, ZoomConstants.MEETING_NO_INVITE);
        opts.no_share = parseBoolean(options, ZoomConstants.MEETING_NO_SHARE);
        opts.no_driving_mode = parseBoolean(options, ZoomConstants.MEETING_NO_DRIVING_MODE);
        opts.no_invite = parseBoolean(options, ZoomConstants.MEETING_NO_INVITE);
        opts.no_share = parseBoolean(options, ZoomConstants.MEETING_NO_SHARE);
        opts.no_titlebar = parseBoolean(options, ZoomConstants.MEETING_NO_TITLEBAR);
        opts.no_disconnect_audio = parseBoolean(options, ZoomConstants.MEETING_NO_DISCONNECT_AUDIO);
        opts.no_audio = parseBoolean(options, ZoomConstants.MEETING_NO_AUDIO);
        opts.no_video = parseBoolean(options, ZoomConstants.MEETING_NO_VIDEO);
        opts.no_chat_msg_toast = parseBoolean(options, ZoomConstants.MEETING_NO_CHAT_MSG_TOAST);
        opts.no_unmute_confirm_dialog = parseBoolean(options, ZoomConstants.MEETING_NO_UNMUTE_CONFIRM_DIALOG);
        opts.no_webinar_register_dialog = parseBoolean(options, ZoomConstants.MEETING_NO_WEBINAR_REGISTER_DIALOG);
        opts.no_dial_in_via_phone = parseBoolean(options, ZoomConstants.MEETING_NO_DIAL_IN_VIA_PHONE);
        opts.no_dial_out_to_phone = parseBoolean(options, ZoomConstants.MEETING_NO_DIAL_OUT_TO_PHONE);
        opts.no_record = parseBoolean(options, ZoomConstants.MEETING_NO_RECORD);
        opts.no_meeting_end_message = parseBoolean(options, ZoomConstants.MEETING_NO_MEETING_END_MESSAGE);
        opts.no_meeting_error_message = parseBoolean(options, ZoomConstants.MEETING_NO_MEETING_ERROR_MESSAGE);
        opts.no_bottom_toolbar = parseBoolean(options, ZoomConstants.MEETING_NO_BOTTOM_TOOLBAR);

        JoinMeetingParams params = new JoinMeetingParams();
        params.displayName = options.get(ZoomConstants.DISPLAY_NAME);
        params.meetingNo = options.get(ZoomConstants.MEETING_ID);
        params.password = options.get(ZoomConstants.MEETING_PASSWORD);

        // Join the meeting
        meetingService.joinMeetingWithParams(activity, params, opts);
        MeetingStatus status = meetingService.getMeetingStatus();
        result.success(status != null ? Arrays.asList(status.name(), "") : Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
    }


    private void startMeeting(MethodCall methodCall, Result result) {
        Map<String, String> options = methodCall.arguments();

        ZoomSDK zoomSDK = ZoomSDK.getInstance();

        if (!zoomSDK.isInitialized()) {
            sendReply(result, Arrays.asList("SDK ERROR", "001"));
            return;
        }

        MeetingService meetingService = zoomSDK.getMeetingService();

        if (meetingService.getMeetingStatus() != MeetingStatus.MEETING_STATUS_IDLE) {
            Log.d("ZoomAllInOneSdkPlugin", "Cannot start a new meeting while another meeting is in progress.");
            sendReply(result, Arrays.asList("MEETING ERROR", "002"));
            return;
        }

        StartMeetingOptions startMeetingOptions = new StartMeetingOptions();
        StartMeetingParamsWithoutLogin startMeetingParamsWithoutLogin = new StartMeetingParamsWithoutLogin();

        // Set meeting parameters
        startMeetingParamsWithoutLogin.displayName = options.get(ZoomConstants.DISPLAY_NAME);
        startMeetingParamsWithoutLogin.userType = Integer.parseInt(options.get(ZoomConstants.USER_TYPE));
        startMeetingParamsWithoutLogin.meetingNo = options.get(ZoomConstants.MEETING_ID);
        startMeetingParamsWithoutLogin.zoomAccessToken = options.get(ZoomConstants.ZAK_TOKEN);

        // Start the meeting
        meetingService.startMeetingWithParams(context, startMeetingParamsWithoutLogin, startMeetingOptions);
        sendReply(result, Arrays.asList("MEETING SUCCESS", "200"));
    }

    private void statusMeeting(Result result) {
        ZoomSDK zoomSDK = ZoomSDK.getInstance();
        if (!zoomSDK.isInitialized()) {
            Log.d("ZoomAllInOneSdkPlugin-onZoomSDKInitializeResult", "statusMeeting: Not initialized!!!!!!");
            result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "SDK not initialized"));
            return;
        }

        MeetingService meetingService = zoomSDK.getMeetingService();
        if (meetingService == null) {
            result.success(Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
            return;
        }

        MeetingStatus status = meetingService.getMeetingStatus();
        result.success(status != null ? Arrays.asList(status.name(), "") : Arrays.asList("MEETING_STATUS_UNKNOWN", "No status available"));
    }

    // Send a reply to Flutter
    private void sendReply(Result result, List<String> response) {
        result.success(response);
    }

    // Callback for Zoom SDK initialization result
    @Override
    public void onZoomSDKInitializeResult(int errorCode, int internalErrorCode) {
        Log.d("ZoomAllInOneSdkPlugin-onZoomSDKInitializeResult", "onZoomSDKInitializeResult: " + errorCode + " , " + internalErrorCode);
    }

    // Callback for Zoom authentication identity expiration
    @Override
    public void onZoomAuthIdentityExpired() {
        Log.d("ZoomAllInOneSdkPlugin-TAG", "onZoomAuthIdentityExpired");
    }

    // Callback when attached to an activity
    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        Log.d("ZoomAllInOneSdkPlugin", "onAttachedToActivity");
        activity = binding.getActivity();
        meetingListener = new ZoomMeetingListener(activity);
    }

    // Callback when detached from an activity due to configuration changes
    @Override
    public void onDetachedFromActivityForConfigChanges() {
        Log.d("ZoomAllInOneSdkPlugin", "onDetachedFromActivityForConfigChanges");
    }

    // Callback when reattached to an activity after configuration changes
    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        Log.d("ZoomAllInOneSdkPlugin", "onReattachedToActivityForConfigChanges");
        activity = binding.getActivity();
    }

    // Callback when detached from an activity
    @Override
    public void onDetachedFromActivity() {
        Log.d("ZoomAllInOneSdkPlugin", "onDetachedFromActivity");
        channel.setMethodCallHandler(null);
        toggleMeetingServiceListener(false);
    }

    // Safer Helper Function for parsing value that might be a String or Boolean
    private boolean parseBoolean(Map<String, ?> options, String property) {
        Object value = options.get(property);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        } else {
            return false; // default
        }
    }

    // Helper Function to create StartMeetingOptions
    private StartMeetingOptions createStartMeetingOptions(Map<String, String> options) {
        StartMeetingOptions opts = new StartMeetingOptions();
        // Implement based on your requirements
        return opts;
    }

    // Helper Function to create StartMeetingParams4NormalUser
    private StartMeetingParams4NormalUser createStartMeetingParams(Map<String, String> options) {
        StartMeetingParams4NormalUser params = new StartMeetingParams4NormalUser();
        // Implement based on your requirements
        return params;
    }

    private void toggleMeetingServiceListener(boolean status) {
		MeetingService meetingService = zoomSDK.getMeetingService();

		if(meetingService != null && status == true) {
			meetingService.addListener(meetingListener);
		}

        if(zoomSDK.isInitialized() && status == false) {
			meetingService.removeListener(meetingListener);
		}
	}
}
