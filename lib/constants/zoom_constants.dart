class ZoomConstants {
  static const String authTokenUrl =
      "https://zoom.us/oauth/token?grant_type=account_credentials&account_id=";
  static const String zakTokenUrl =
      "https://api.zoom.us/v2/users/me/token?type=zak";
  // Zoom methods
  static const String initZoom = 'initZoom';
  static const String joinMeeting = 'joinMeeting';
  static const String startMeeting = 'startMeeting';
  static const String statusMeeting = 'statusMeeting';

  // Zoom options
  static const String jwtToken = 'jwtToken';
  static const String domain = 'domain';
  static const String userId = 'userId';
  static const String meetingId = 'meetingId';
  static const String meetingPassword = 'meetingPassword';
  static const String disableDialIn = 'disableDialIn';
  static const String disableDrive = 'disableDrive';
  static const String disableInvite = 'disableInvite';
  static const String disableShare = 'disableShare';
  static const String disableTitlebar = 'disableTitlebar';
  static const String noDisconnectAudio = 'noDisconnectAudio';
  static const String viewOptions = 'viewOptions';
  static const String noAudio = 'noAudio';
  static const String zakToken = 'zakToken';
  static const String displayName = 'displayName';
  static const String userPassword = 'userPassword';
  static const String userType = 'userType';

  // Extra
  static const String token = "token";

  // Meeting options
  static const String noDrivingMode = 'noDrivingMode';
  static const String noInvite = 'noInvite';
  static const String noShare = 'noShare';
  static const String noTitlebar = 'noTitlebar';
  static const String noVideo = 'noVideo';
  static const String zoomToken = 'zoomToken';
  static const String zoomAccessToken = 'zoomAccessToken';
  static const String jwtAPIKey = 'jwtAPIKey';
  static const String jwtSignature = 'jwtSignature';
  static const String customMeetingId = 'customMeetingId';
  static const String customerKey = 'customerKey';
  static const String noChatMsgToast = 'noChatMsgToast';
  static const String noUnmuteConfirmDialog = 'noUnmuteConfirmDialog';
  static const String noWebinarRegisterDialog = 'noWebinarRegisterDialog';
  static const String inviteOptions = 'inviteOptions';
  static const String meetingViewsOptions = 'meetingViewsOptions';
  static const String noDialInViaPhone = 'noDialInViaPhone';
  static const String noDialOutToPhone = 'noDialOutToPhone';
  static const String noRecord = 'noRecord';
  static const String noMeetingEndMessage = 'noMeetingEndMessage';
  static const String noMeetingErrorMessage = 'noMeetingErrorMessage';
  static const String noBottomToolbar = 'noBottomToolbar';
}
