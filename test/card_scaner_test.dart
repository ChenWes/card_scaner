import 'package:flutter_test/flutter_test.dart';
import 'package:card_scaner/card_scaner.dart';
import 'package:card_scaner/card_scaner_platform_interface.dart';
import 'package:card_scaner/card_scaner_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockCardScanerPlatform
    with MockPlatformInterfaceMixin
    implements CardScanerPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final CardScanerPlatform initialPlatform = CardScanerPlatform.instance;

  test('$MethodChannelCardScaner is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelCardScaner>());
  });

  test('getPlatformVersion', () async {
    CardScaner cardScanerPlugin = CardScaner();
    MockCardScanerPlatform fakePlatform = MockCardScanerPlatform();
    CardScanerPlatform.instance = fakePlatform;

    //expect(await cardScanerPlugin.getPlatformVersion(), '42');
  });
}
