import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'card_scaner_platform_interface.dart';

/// An implementation of [CardScanerPlatform] that uses method channels.
class MethodChannelCardScaner extends CardScanerPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('card_scaner');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }
}
