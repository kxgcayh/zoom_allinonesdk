import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:zoom_allinonesdk/constants/zoom_constants.dart';
import 'package:zoom_allinonesdk/data/models/accesstokenmodel.dart';
import 'package:zoom_allinonesdk/data/providers/zoom_provider.dart';
import 'package:zoom_allinonesdk/data/repositories/zoom_repository.dart';

import 'data/models/meeting_options.dart';
import 'data/models/zoom_options.dart';
import 'data/util/jwt_generator.dart';
import 'data/util/zoom_error.dart';
import 'zoom_allinonesdk_platform_interface.dart';

class MethodChannelZoomAllInOneSdk extends ZoomAllInOneSdkPlatform {
  @visibleForTesting
  final MethodChannel methodChannel = const MethodChannel('zoom_allinonesdk');
  final EventChannel eventChannel =
      const EventChannel('zoom_allinonesdk/flutter_zoom_meeting_event_stream');

  final jwtGenerator = JwtGenerator();

  @override
  Future<List> initZoom({required ZoomOptions options}) async {
    try {
      String jwtSignature = jwtGenerator.generate(
          key: options.clientId ?? "", secret: options.clientSecert ?? "");
      final optionsMap = <String, dynamic>{
        ZoomConstants.jwtToken: jwtSignature,
        ZoomConstants.domain: options.domain,
      };

      // Invoke method and handle the result
      final version = await methodChannel
          .invokeMethod<List>(ZoomConstants.initZoom, optionsMap)
          .then<List>((List? value) => value ?? List.empty());

      return version;
    } catch (e) {
      throw ZoomError('Error joining meeting: $e');
    }
  }

  @override
  Future<bool> joinMeeting({required MeetingOptions meetingOptions}) async {
    try {
      // Prepare options map
      final options = <String, dynamic>{
        ZoomConstants.userId: meetingOptions.userId ?? '',
        ZoomConstants.userPassword: meetingOptions.userPassword ?? '',
        ZoomConstants.displayName: meetingOptions.displayName ?? '',
        ZoomConstants.meetingId: meetingOptions.meetingId ?? '',
        ZoomConstants.meetingPassword: meetingOptions.meetingPassword ?? '',
        ZoomConstants.noDialInViaPhone: meetingOptions.noDialInViaPhone ?? '',
        ZoomConstants.noDrivingMode: meetingOptions.noDrivingMode ?? '',
        ZoomConstants.noInvite: meetingOptions.noInvite ?? '',
        ZoomConstants.noShare: meetingOptions.noShare ?? '',
        ZoomConstants.noTitlebar: meetingOptions.noTitlebar ?? '',
        ZoomConstants.noDisconnectAudio: meetingOptions.noDisconnectAudio ?? '',
        ZoomConstants.viewOptions: meetingOptions.viewOptions ?? '',
        ZoomConstants.noAudio: meetingOptions.noAudio ?? '',
      };

      // Invoke method and handle the result
      final result = await methodChannel.invokeMethod<bool>(
        ZoomConstants.joinMeeting,
        options,
      );

      // Use null coalescing operator to return false if null
      return result ?? false;
    } catch (e) {
      throw ZoomError('Error joining meeting: $e');
    }
  }

  @override
  Future<List> startMeeting({
    required String clientId,
    required String clientSecret,
    required String accountId,
    required MeetingOptions meetingOptions,
  }) async {
    try {
      // Prepare options map
      final options = <String, dynamic>{
        ZoomConstants.meetingId: meetingOptions.meetingId ?? '',
        ZoomConstants.userId: meetingOptions.userId ?? '',
        ZoomConstants.displayName: meetingOptions.displayName ?? '',
        ZoomConstants.userPassword: meetingOptions.userPassword ?? '',
        ZoomConstants.noDialInViaPhone: meetingOptions.noDialInViaPhone ?? '',
        ZoomConstants.noDrivingMode: meetingOptions.noDrivingMode ?? '',
        ZoomConstants.noInvite: meetingOptions.noInvite ?? '',
        ZoomConstants.noShare: meetingOptions.noShare ?? '',
        ZoomConstants.noTitlebar: meetingOptions.noTitlebar ?? '',
        ZoomConstants.viewOptions: meetingOptions.viewOptions ?? '',
        ZoomConstants.noDisconnectAudio: meetingOptions.noDisconnectAudio ?? '',
        ZoomConstants.noAudio: meetingOptions.noAudio ?? '',
        ZoomConstants.userType: meetingOptions.userType ?? '',
      };

      // Instantiate ZoomProvider and ZoomRepository
      final ZoomProvider zoomProvider = ZoomProvider();
      final ZoomRepository repository =
          ZoomRepository(zoomProvider: zoomProvider);

      // Fetch access token
      final AccessTokenModel accessTokenResponse =
          await repository.fetchAccesstoken(
        accountId: accountId,
        clientId: clientId,
        clientSecret: clientSecret,
      );

      // Fetch zak token
      final String zakTokenResponse = await repository.fetchZaktoken(
        clientId: clientId,
        clientSecret: clientSecret,
        accessToken: accessTokenResponse.accessToken,
      );

      // Add zak token to options map
      options
          .addAll(<String, dynamic>{ZoomConstants.zakToken: zakTokenResponse});

      debugPrint("zak $zakTokenResponse");

      // Invoke method and handle the result
      final version = await methodChannel.invokeMethod<List>(
        ZoomConstants.startMeeting,
        options,
      );

      // Use null coalescing operator to return an empty list if null
      return version ?? [];
    } catch (e) {
      // Throw a custom error to provide a meaningful error message
      throw ZoomError('Error starting meeting: $e');
    }
  }

  @override
  Future<List> statusMeeting(String meetingId) async {
    try {
      final optionsMap = <String, dynamic>{
        ZoomConstants.statusMeeting: '',
      };

      final status = await methodChannel
          .invokeMethod<List>(
            ZoomConstants.statusMeeting,
            optionsMap,
          )
          .then<List>((List? value) => value ?? List.empty());

      return status;
    } catch (e) {
      throw ZoomError('Error status meeting: $e');
    }
  }

  @override
  Stream<dynamic> onMeetingStatus() {
    return eventChannel.receiveBroadcastStream();
  }
}
