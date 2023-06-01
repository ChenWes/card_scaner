import 'dart:async';

import 'package:card_scaner/card_scaner.dart';
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

  //获取回调函数
  late StreamSubscription _ssp;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // We also handle the message potentially returning null.
    try {
      // 使用Method Channel调用方法
      platformVersion =
          await CardScaner.platformVersion ?? 'Unknown platform version';

      // 声明变量获取回调函数
      // _ssp = CardScaner.receiveStream.listen((event) {
      //   print("返回的数据" + event.toString());
      // }, onError: (error) {
      //   print(error.toString());
      // });

      // 不声明变量获取回调函数
      CardScaner.receiveStream.listen((event) {
        print("返回的数据" + event.toString());

        // 返回数据
        setState(() {
          _deviceValue = _deviceValue + "=>" + event.toString();
        });
      }, onError: (error) {
        print(error.toString());
      });
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('CardScaner Plugin Demo '),
        ),
        body: Center(
            child: Column(
          children: [
            Text('Device : $_platformVersion\n'),
            Text('Value : $_deviceValue\n'),
          ],
        )),
        floatingActionButton: FloatingActionButton(
          onPressed: () async {
            try {
              CardScaner.openDevice;
            } catch (ex) {
              print("打开设备出现错误：" + ex.toString());
            }
          },
          child: Icon(Icons.open_in_browser),
        ),
      ),
    );
  }
}
