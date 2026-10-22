import 'package:flutter/material.dart';

import 'navigation/app_navigation.dart';
import 'screens/auth_page.dart';
import 'services/placement_api.dart';

void main() {
  runApp(const PlacementManagementApp());
}

class PlacementManagementApp extends StatelessWidget {
  const PlacementManagementApp({super.key, this.api});

  final PlacementApi? api;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorKey: appNavigatorKey,
      title: 'CampusHire',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF2563EB)),
        useMaterial3: true,
        scaffoldBackgroundColor: const Color(0xFFF8FAFC),
        inputDecorationTheme: const InputDecorationTheme(
          filled: true,
          fillColor: Colors.white,
          border: OutlineInputBorder(),
        ),
        cardTheme: const CardThemeData(
          color: Colors.white,
          margin: EdgeInsets.zero,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.all(Radius.circular(16))),
        ),
      ),
      home: AuthPage(api: api ?? PlacementApi()),
    );
  }
}

