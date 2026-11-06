import 'package:flutter/material.dart';

import '../theme/app_theme.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({
    super.key,
    required this.themeMode,
    required this.onThemeChanged,
    required this.onLogout,
  });

  final ThemeMode themeMode;
  final ValueChanged<ThemeMode> onThemeChanged;
  final VoidCallback onLogout;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 16, 20, 32),
        children: [
          Text(
            'Appearance',
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: colors.primary,
                  letterSpacing: 0.5,
                ),
          ),
          const SizedBox(height: 10),
          Card(
            child: SwitchListTile.adaptive(
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 18,
                vertical: 6,
              ),
              secondary: CircleAvatar(
                backgroundColor: colors.primaryContainer,
                child: Icon(
                  isDark ? Icons.dark_mode_rounded : Icons.light_mode_rounded,
                  color: colors.primary,
                ),
              ),
              title: const Text(
                'Dark mode',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
              subtitle: Text(
                isDark
                    ? 'Easier on the eyes at night'
                    : 'Clean and bright during the day',
                style: TextStyle(
                  fontSize: 13,
                  color: colors.onSurfaceVariant,
                ),
              ),
              value: isDark,
              activeTrackColor: colors.primary,
              onChanged: (_) {
                final scope = AppThemeScope.maybeOf(context);
                if (scope != null) {
                  scope.toggleTheme(context);
                } else {
                  onThemeChanged(isDark ? ThemeMode.light : ThemeMode.dark);
                }
              },
            ),
          ),
          const SizedBox(height: 28),

          Text(
            'Account & Security',
            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                  fontWeight: FontWeight.w700,
                  color: colors.primary,
                  letterSpacing: 0.5,
                ),
          ),
          const SizedBox(height: 10),
          Card(
            child: ListTile(
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 18,
                vertical: 6,
              ),
              leading: CircleAvatar(
                backgroundColor: colors.errorContainer,
                child: Icon(
                  Icons.logout_rounded,
                  color: colors.onErrorContainer,
                ),
              ),
              title: const Text(
                'Sign out',
                style: TextStyle(fontWeight: FontWeight.w700),
              ),
              subtitle: Text(
                'End your current student session on this device',
                style: TextStyle(
                  fontSize: 13,
                  color: colors.onSurfaceVariant,
                ),
              ),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: onLogout,
            ),
          ),
          const SizedBox(height: 36),

          // CDPC & CHARUSAT Placement cell footer
          Center(
            child: Column(
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Image.asset(
                    'assets/images/cdpc_logo_square.jpg',
                    height: 40,
                    width: 40,
                    fit: BoxFit.cover,
                    errorBuilder: (_, _, _) => Icon(
                      Icons.school_rounded,
                      size: 28,
                      color: colors.primary,
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  'CHARUSAT · CDPC',
                  style: TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 13,
                    color: colors.onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Career Development and Placement Cell',
                  style: TextStyle(
                    fontSize: 12,
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
