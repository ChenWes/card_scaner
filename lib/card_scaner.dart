import 'dart:async';

import 'package:flutter/services.dart';

class CardScaner {
  static const MethodChannel _channel = MethodChannel('card_scaner');
  static const EventChannel _eventChannel = EventChannel('card_scaner/event');
  static late Stream _eventStream;

  /// 获取平台版本
  static Future<String?> get platformVersion async {
    final String? version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  /// 打开设备
  static Future<String?> get openDevice async {
    try {
      final String? result = await _channel.invokeMethod('openDevice');
      return result;
    } on PlatformException catch (ex) {
      if (ex.message == 'DeviceOpenFail') {
        final String? result =
            await Future.delayed(const Duration(seconds: 2), () {
          return openDevice;
        });
        return result;
      } else {
        return null;
      }
    }
  }

  /// 配置设备
  static Future<String?> get configDevice async {
    final String? result = await _channel.invokeMethod('configDevice');
    return result;
  }

  /// 关闭设备
  static Future<String?> get closeDevice async {
    final String? result = await _channel.invokeMethod('closeDevice');
    return result;
  }

  /// Stream(Event) coming from Android
  /// 调用数据流
  static Stream get receiveStream {
    _eventStream = _eventChannel
        .receiveBroadcastStream()
        .map<String>((dynamic value) => value);
    return _eventStream;
  }
}
