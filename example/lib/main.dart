import 'dart:async';
import 'package:card_scaner/card_scaner.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  String _deviceValue = '';

  StreamSubscription<dynamic>? _dataSubscription;

  @override
  void initState() {
    super.initState();
    initPlatformState();
    _listenDeviceData();
  }

  @override
  void dispose() {
    _dataSubscription?.cancel(); // 防内存泄漏
    super.dispose();
  }

  Future<void> initPlatformState() async {
    try {
      final version = await CardScaner.platformVersion;
      if (mounted) {
        setState(() {
          _platformVersion = version ?? 'Unknown';
        });
      }
    } on PlatformException {
      if (mounted) {
        setState(() {
          _platformVersion = 'Failed to get platform version';
        });
      }
    }
  }

  void _listenDeviceData() {
    _dataSubscription = CardScaner.receiveStream.listen((event) {
      if (kDebugMode) {
        print("收到数据 => $event");
      }
      setState(() {
        _deviceValue += "=> $event";
      });
    }, onError: (error) {
      if (kDebugMode) {
        print("监听错误：$error");
      }
    });
  }

  Future<void> openDevice() async {
    try {
      await CardScaner.openDevice;
      if (kDebugMode) {
        print("设备打开成功");
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("打开设备失败：${e.message}");
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('CardScaner 插件测试'),
        ),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  '平台版本: $_platformVersion',
                  style: const TextStyle(fontSize: 18),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 20),
                Text(
                  '设备数据: $_deviceValue',
                  style: const TextStyle(fontSize: 16, color: Colors.blue),
                  textAlign: TextAlign.center,
                ),
              ],
            ),
          ),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: openDevice,
          child: const Icon(Icons.usb),
        ),
      ),
    );
  }
}