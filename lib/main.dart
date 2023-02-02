import 'dart:ffi';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.blue,
      ),
      home: const MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  File? selectedImage;
  static const platform = MethodChannel("com.example/testChannel");


  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          title: const Text("Image Picker Example"),
        ),
        body: Center(
          child: Column(
            children: [
              MaterialButton(
                  color: Colors.blue,
                  onPressed: _startActivity,
                  child: const Text("Pick Image from Gallery",
                      style: TextStyle(
                          color: Colors.white70, fontWeight: FontWeight.bold))),
              MaterialButton(
                  color: Colors.blue,
                  onPressed: _startPdfActivity,
                  child: const Text("Pick Pdf",
                      style: TextStyle(
                          color: Colors.white70, fontWeight: FontWeight.bold))),
              displayImage(), /*Container(child: Image.file(
                selectedImage,
                width: 350, height: 350,
              ),)*/
            ],
          ),
        ));
  }

  Widget displayImage() {
    //selectedImage = File("content://com.miui.gallery.open/raw/%2Fstorage%2Femulated%2F0%2FDCIM%2FScreenshots%2FScreenshot_2023-01-09-12-12-20-060_com.google.android.youtube.jpg");
    if (selectedImage == null || selectedImage == "") {
      return Text("No Image Selected!");
    } else {
      return Image.file(selectedImage!, width: 350, height: 450);
    }
  }

  Future<void> _startActivity() async {
    try {
      final String result = await platform.invokeMethod('StartSecondActivity');
      //String data = await loadAsset(path);
      if(result != "")
      setState(() => selectedImage = File(result));
      debugPrint('Result: $result ');
    } on PlatformException catch (e) {
      debugPrint("Error: '${e.message}'.");
    }
  }

  Future<void> _startPdfActivity() async {
    try {
      final String result = await platform.invokeMethod('StartPdfActivity');
      //String data = await loadAsset(path);
      if(result != "")
      setState(() => selectedImage = File(result));
      debugPrint('Result: $result ');
    } on PlatformException catch (e) {
      debugPrint("Error: '${e.message}'.");
    }
  }

  Future loadAsset(String assetPath) async {
    return await rootBundle.loadString(assetPath);
  }
}


