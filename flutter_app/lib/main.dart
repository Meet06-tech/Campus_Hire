import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'navigation/app_navigation.dart';
import 'screens/auth_page.dart';
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
    final preferences = await SharedPreferences.getInstance();
    if (!mounted) return;
    setState(
      () => _themeMode = preferences.getBool('dark_mode') == true
          ? ThemeMode.dark
          : ThemeMode.light,
    );
  }

  Future<void> _setThemeMode(ThemeMode mode) async {
    setState(() => _themeMode = mode);
    final preferences = await SharedPreferences.getInstance();
    await preferences.setBool('dark_mode', mode == ThemeMode.dark);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      navigatorKey: appNavigatorKey,
      title: 'CampusHire',
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: _themeMode,
      home: AuthPage(
        api: widget.api ?? PlacementApi(),
        themeMode: _themeMode,
        onThemeModeChanged: _setThemeMode,
      ),
    );
  }
}
