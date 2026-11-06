import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'navigation/app_navigation.dart';
import 'screens/welcome_screen.dart';
import 'services/placement_api.dart';
import 'theme/app_theme.dart';

void main() {
  runApp(const PlacementManagementApp());
}

class PlacementManagementApp extends StatefulWidget {
  const PlacementManagementApp({super.key, this.api});

  final PlacementApi? api;

  @override
  State<PlacementManagementApp> createState() => _PlacementManagementAppState();
}

class _PlacementManagementAppState extends State<PlacementManagementApp> {
  ThemeMode _themeMode = ThemeMode.light;

  @override
  void initState() {
    super.initState();
    _loadTheme();
  }

  Future<void> _loadTheme() async {
    try {
      final preferences = await SharedPreferences.getInstance();
      if (!mounted) return;
      setState(
        () => _themeMode = preferences.getBool('dark_mode') == true
            ? ThemeMode.dark
            : ThemeMode.light,
      );
    } catch (_) {
      // Graceful fallback if SharedPreferences is unavailable in mock tests
    }
  }

  Future<void> _setThemeMode(ThemeMode mode) async {
    setState(() => _themeMode = mode);
    try {
      final preferences = await SharedPreferences.getInstance();
      await preferences.setBool('dark_mode', mode == ThemeMode.dark);
    } catch (_) {
      // Graceful fallback
    }
  }

  @override
  Widget build(BuildContext context) {
    return AppThemeScope(
      themeMode: _themeMode,
      onThemeModeChanged: _setThemeMode,
      child: MaterialApp(
        debugShowCheckedModeBanner: false,
        navigatorKey: appNavigatorKey,
        title: 'CampusHire',
        theme: AppTheme.light(),
        darkTheme: AppTheme.dark(),
        themeMode: _themeMode,
        home: WelcomeScreen(
          api: widget.api ?? PlacementApi(),
          themeMode: _themeMode,
          onThemeModeChanged: _setThemeMode,
        ),
      ),
    );
  }
}
