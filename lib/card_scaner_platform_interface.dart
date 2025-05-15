import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'card_scaner_method_channel.dart';

abstract class CardScanerPlatform extends PlatformInterface {
  /// Constructs a CardScanerPlatform.
  CardScanerPlatform() : super(token: _token);

  static final Object _token = Object();

  static CardScanerPlatform _instance = MethodChannelCardScaner();

  /// The default instance of [CardScanerPlatform] to use.
  ///
  /// Defaults to [MethodChannelCardScaner].
  static CardScanerPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [CardScanerPlatform] when
  /// they register themselves.
  static set instance(CardScanerPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
