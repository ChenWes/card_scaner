# card_scaner

CH34 刷卡器 Flutter 插件，用于通过 CH34x 芯片的串口通信读取刷卡数据。本插件专为工业环境（如 MES 系统）设计，支持 Android 平台。

> **芯片方案**: [沁恒 CH34x 系列](http://www.wch.cn/products/CH340.html)

## 功能特性

- **设备管理**: 打开、配置、关闭 CH34 串口设备
- **数据监听**: 通过 `Stream` 实时接收刷卡数据
- **自动重试**: 设备打开失败时自动延时重试
- **数据解析**: 自动将原始串口数据转换为 8 位 16 进制卡号
- **工业级稳定性**: 专为 24/7 运行的工厂环境设计

## 安装

### 1. 添加依赖

在 `pubspec.yaml` 文件中添加：

```yaml
dependencies:
  card_scaner:
    git:
      url: https://github.com/ChenWes/card_scaner
      ref: main
```

### 2. 导入插件

```dart
import 'package:card_scaner/card_scaner.dart';
```

## 使用方法

### 基本流程

1. **打开设备**: 调用 `CardScaner.openDevice`
2. **监听数据**: 订阅 `CardScaner.receiveStream`
3. **处理刷卡**: 在 Stream 回调中获取卡号
4. **关闭设备**: 应用退出时调用 `CardScaner.closeDevice`

### 完整示例

```dart
import 'dart:async';
import 'package:card_scaner/card_scaner.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class CardScannerPage extends StatefulWidget {
  const CardScannerPage({Key? key}) : super(key: key);

  @override
  State<CardScannerPage> createState() => _CardScannerPageState();
}

class _CardScannerPageState extends State<CardScannerPage> {
  String _cardNumber = '等待刷卡...';
  StreamSubscription<dynamic>? _dataSubscription;

  @override
  void initState() {
    super.initState();
    _initializeScanner();
  }

  @override
  void dispose() {
    _dataSubscription?.cancel();
    _closeDevice();
    super.dispose();
  }

  Future<void> _initializeScanner() async {
    // 1. 打开设备
    try {
      await CardScaner.openDevice;
      if (kDebugMode) {
        print('设备打开成功');
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('打开设备失败: ${e.message}');
      }
    }

    // 2. 监听数据流
    _dataSubscription = CardScaner.receiveStream.listen((event) {
      // event 为 8 位 16 进制卡号（如 "12345678"）
      setState(() {
        _cardNumber = event.toString();
      });
      
      // 触发业务逻辑
      _handleCardScan(event.toString());
    }, onError: (error) {
      if (kDebugMode) {
        print('刷卡错误: $error');
      }
    });
  }

  void _handleCardScan(String cardNumber) {
    // 处理刷卡逻辑，如发送到服务器
    print('刷卡成功: $cardNumber');
  }

  Future<void> _closeDevice() async {
    try {
      await CardScaner.closeDevice;
      if (kDebugMode) {
        print('设备已关闭');
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print('关闭设备失败: ${e.message}');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('刷卡器测试'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(
              Icons.credit_card,
              size: 80,
              color: Colors.blue,
            ),
            const SizedBox(height: 20),
            const Text(
              '当前卡号:',
              style: TextStyle(fontSize: 18),
            ),
            const SizedBox(height: 10),
            Text(
              _cardNumber,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: Colors.green,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
```

## API 参考

### CardScaner 类

#### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| `platformVersion` | `Future<String?>` | 获取平台版本信息（Android 系统版本） |
| `openDevice` | `Future<String?>` | 打开串口设备，返回成功或错误消息 |
| `closeDevice` | `Future<String?>` | 关闭设备，释放资源 |
| `receiveStream` | `Stream` | 刷卡数据流，返回 8 位 16 进制卡号字符串 |

#### 方法

##### `openDevice`
```dart
static Future<String?> get openDevice;
```
打开 CH34 串口设备。内部会自动申请 USB 权限、初始化设备并启动数据监听线程。

**返回值**:
- `"DeviceOpenSuccess"`: 打开成功
- `null` 或异常: 打开失败

**注意**: 如果打开失败（`DeviceOpenFail`），插件会自动等待 2 秒后重试一次。

##### `closeDevice`
```dart
static Future<String?> get closeDevice;
```
关闭设备，停止数据监听线程。

**返回值**:
- `"DeviceCloseSuccess"`: 关闭成功
- `null` 或异常: 关闭失败

##### `receiveStream`
```dart
static Stream get receiveStream;
```
获取刷卡数据流。数据格式为 8 位大写 16 进制字符串（如 `"12345678"`）。

**使用示例**:
```dart
CardScaner.receiveStream.listen((cardNumber) {
  print('刷卡: $cardNumber');
});
```

## 数据格式说明

### 原始数据解析流程

1. **串口读取**: 设备发送原始字节数据（如 `"12345678\r\n"`）
2. **行分隔**: 按 `\r\n` 分割完整数据帧
3. **16 进制转换**: 将十进制字符串转换为 16 进制
4. **补位对齐**: 补零至 8 位长度
5. **字节重排**: 每 2 个字符为一组，倒序排列（符合刷卡器协议）

### 示例转换

原始串口数据: `"12345678\r\n"`  
→ 十进制值: `12345678`  
→ 16 进制: `"bc614e"`  
→ 补零至 8 位: `"00bc614e"`  
→ 重排 (2 字符一组): `"00"` `"bc"` `"61"` `"4e"`  
→ 倒序: `"4e"` `"61"` `"bc"` `"00"`  
→ 最终卡号: `"4E61BC00"`

## 设备配置

当前版本使用固定配置（不可自定义）:
- **波特率**: 9600
- **数据位**: 8
- **停止位**: 1
- **奇偶校验**: 无 (0)
- **流控制**: 无 (0)

> 后续版本将支持自定义配置。

## 权限说明

### Android 权限
插件需要以下权限：
- USB 设备访问权限
- 系统会弹出权限请求对话框

### 自动授权方案（适用于 MES 系统）
由于每次打开串口都会弹出授权对话框，在工业环境中不方便操作，建议配合**辅助应用**实现自动授权：

1. **辅助应用**: [cf-mes-client-crossplatform-helper](https://github.com/ChenWes/cf-mes-client-crossplatform-helper)
2. **工作原理**: 
   - 辅助应用开机自启
   - 监控目标应用（MES 系统）的进程
   - 自动点击 USB 权限对话框的"确定"按钮
3. **部署要求**: 需要将辅助应用预装到设备并授予无障碍权限

## 故障排查

### 常见问题

#### 1. 设备打开失败（`DeviceOpenFail`）
- **可能原因**: USB 线缆未连接、设备未供电、驱动未安装
- **解决方案**: 
  - 检查硬件连接
  - 确认设备指示灯正常
  - 重启设备或应用

#### 2. 无法接收数据
- **可能原因**: 波特率不匹配、数据线接触不良
- **解决方案**:
  - 确认设备配置与插件一致（9600-8-N-1）
  - 更换 USB 线缆

#### 3. 权限对话框不弹出
- **可能原因**: 系统权限缓存、辅助应用干扰
- **解决方案**:
  - 清除应用数据
  - 暂时禁用辅助应用

#### 4. 数据格式错误
- **可能原因**: 刷卡器协议不一致
- **解决方案**:
  - 确认刷卡器型号为 CH34x 系列
  - 检查数据解析逻辑（见[数据格式说明](#数据格式说明)）

### 调试信息

启用调试模式查看详细日志：

```dart
if (kDebugMode) {
  CardScaner.receiveStream.listen((event) {
    print('原始数据: $event');
  }, onError: (error) {
    print('错误: $error');
  });
}
```

## 项目结构

```
card_scaner/
├── android/                  # Android 平台实现
│   └── src/main/java/com/cf/card_scaner/
│       ├── CardScanerPlugin.java  # 插件主类
│       └── libs/CH34xUARTDriver.jar  # 沁恒官方驱动
├── lib/                      # Dart 接口
│   ├── card_scaner.dart            # 主接口类
│   ├── card_scaner_platform_interface.dart
│   └── card_scaner_method_channel.dart
├── example/                  # 示例应用
│   └── lib/main.dart
└── test/                     # 单元测试
```

## 开发说明

### 环境要求
- Flutter 3.3.0+或Flutter 3.41.6分支版本
- Dart 3.0.0+
- Android SDK 21+

### 编译测试
```bash
# 运行示例应用
cd example
flutter run

# 运行单元测试
flutter test
```

### 添加新功能
1. 修改 `lib/card_scaner.dart` 添加 Dart 接口
2. 更新 `android/src/main/java/.../CardScanerPlugin.java` 实现 Native 功能
3. 在 `example/` 中测试功能
4. 更新本文档

