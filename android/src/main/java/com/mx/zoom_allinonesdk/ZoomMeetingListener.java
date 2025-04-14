package com.mx.zoom_allinonesdk;

import android.util.Log;
import android.app.Activity;
import android.widget.Toast;
import us.zoom.sdk.ZoomError;
import us.zoom.sdk.MeetingStatus;
import us.zoom.sdk.MeetingParameter;
import us.zoom.sdk.MeetingServiceListener;
import us.zoom.sdk.MeetingError;

public class ZoomMeetingListener implements MeetingServiceListener {
    private Activity activity;

    public ZoomMeetingListener(Activity activity) {
        this.activity = activity;
    }

    // Callback for meeting status changes
    @Override
    public void onMeetingStatusChanged(MeetingStatus meetingStatus, int errorCode, int internalErrorCode) {
        Log.d("ZoomAllInOneSdkPlugin", "Meeting status changed: " + meetingStatus);

        if (meetingStatus == MeetingStatus.MEETING_STATUS_CONNECTING) {
            Log.d("ZoomAllInOneSdkPlugin", "Connecting to the meeting...");
        } else if (meetingStatus == MeetingStatus.MEETING_STATUS_DISCONNECTING) {
            if (errorCode == ZoomError.ZOOM_ERROR_SUCCESS) {
                Log.d("ZoomAllInOneSdkPlugin", "Meeting disconnected successfully.");
            } else {
                Log.e("ZoomAllInOneSdkPlugin", "Meeting disconnect failed. Error: " + errorCode + ", internalErrorCode: " + internalErrorCode);
            }
        } else if (meetingStatus == MeetingStatus.MEETING_STATUS_FAILED) {
            Log.e("ZoomAllInOneSdkPlugin", "Meeting Failed");
            handleMeetingFailure(errorCode);
        }
    }

    // Callback for meeting parameter notification
    @Override
    public void onMeetingParameterNotification(MeetingParameter meetingParameter) {
        Log.d("ZoomAllInOneSdkPlugin", "onMeetingParameterNotification: " + meetingParameter);
    }

    // Handle meeting failure
    private void handleMeetingFailure(int errorCode) {
        Log.e("ZoomAllInOneSdkPlugin", "Meeting failed. Error: " + errorCode);

        // Show an appropriate message to the user
        switch (errorCode) {
            case MeetingError.MEETING_ERROR_CLIENT_INCOMPATIBLE:
                showToast("Your Zoom client version is too low");
                break;
            case MeetingError.MEETING_ERROR_INCORRECT_MEETING_NUMBER:
                showToast("The meeting number is incorrect");
                break;
            case MeetingError.MEETING_ERROR_MEETING_NOT_EXIST:
                showToast("The meeting does not exist");
                break;
            case MeetingError.MEETING_ERROR_NETWORK_UNAVAILABLE:
                showToast("Network unavailable");
                break;
            case MeetingError.MEETING_ERROR_TIMEOUT:
                showToast("Connection timeout");
                break;
            case MeetingError.MEETING_ERROR_USER_FULL:
                showToast("The meeting is full");
                break;
            case MeetingError.MEETING_ERROR_WEB_SERVICE_FAILED:
                showToast("Web service failed");
                break;
            default:
                showToast("Unknown error");
                break;
        }
    }

    // Show a toast message
    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}