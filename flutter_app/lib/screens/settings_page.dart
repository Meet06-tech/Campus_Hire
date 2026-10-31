import 'package:flutter/material.dart';

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
    final dark = Theme.of(context).brightness == Brightness.dark;
    return Scaffold(
      appBar: AppBar(title: const Text('Settings')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
        children: [
          Text(
            'Preferences',
            style: Theme.of(context).textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 12),
          Card(
            child: SwitchListTile.adaptive(
              contentPadding: const EdgeInsets.symmetric(
                horizontal: 18,
                vertical: 6,
              ),
              secondary: CircleAvatar(
                backgroundColor: colors.primaryContainer,
                child: Icon(
                  dark ? Icons.dark_mode_rounded : Icons.light_mode_rounded,
                  color: colors.primary,
                ),
              ),
              title: const Text(
                'Dark mode',
                style: TextStyle(fontWeight: FontWeight.w800),
              ),
              subtitle: Text(
                dark
                    ? 'Easier on the eyes at night'
                    : 'Bright and clear during the day',
              ),
              value: dark,
              onChanged: (value) =>
                  onThemeChanged(value ? ThemeMode.dark : ThemeMode.light),
            ),
          ),
          const SizedBox(height: 24),
          Text(
            'Account',
            style: Theme.of(context).textTheme.titleLarge
                ?.copyWith(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 12),
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
                style: TextStyle(fontWeight: FontWeight.w800),
              ),
              subtitle: const Text('End your current session on this device'),
              trailing: const Icon(Icons.chevron_right_rounded),
              onTap: onLogout,
            ),
          ),
          const SizedBox(height: 28),
          Center(
            child: Text(
              'CampusHire · Placement Management',
              style: TextStyle(color: colors.onSurfaceVariant, fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}
