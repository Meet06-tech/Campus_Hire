# placement_management_app

## Run against the local Spring Boot backend

Start the backend on the Mac at port 8080 first. The Flutter client already
supports the `API_BASE_URL` Dart define, so the host can be selected without
changing source code.

For a physical Android phone connected through USB debugging, create the
reverse tunnel and run the debug app. The physical-device loopback URL is the
Android default in this project:

```bash
adb reverse tcp:8080 tcp:8080
adb reverse --list
flutter run
```

The reverse rule is tied to the current ADB connection and should be recreated
after reconnecting or restarting the phone. The debug Android manifest permits
the local HTTP connection; release builds do not enable that override.

For an Android emulator, use the host bridge address instead:

```bash
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
```

For Flutter desktop or web, the default is
`http://localhost:8080/api`.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Learn Flutter](https://docs.flutter.dev/get-started/learn-flutter)
- [Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Flutter learning resources](https://docs.flutter.dev/reference/learning-resources)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.
