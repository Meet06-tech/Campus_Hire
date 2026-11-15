import 'package:flutter/material.dart';

import '../services/placement_api.dart';
import 'change_password_page.dart';
import 'people_page.dart';
import 'profile_page.dart';
import 'support_page.dart';

class MorePage extends StatelessWidget {
  const MorePage({super.key, required this.api, required this.onLogout});
  final PlacementApi api;
  final VoidCallback onLogout;

  Future<void> _logout(BuildContext context) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Log out?'),
        content: const Text('You can sign in again whenever you are ready.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Log out')),
        ],
      ),
    );
    if (confirmed == true && context.mounted) onLogout();
  }

  void _open(BuildContext context, Widget page) => Navigator.of(context).push(MaterialPageRoute(builder: (_) => page));

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final items = [
      ('People', 'Browse the student directory', Icons.people_alt_outlined, () => _open(context, PeoplePage(api: api))),
      ('Profile', 'View and edit your information', Icons.person_outline_rounded, () => _open(context, ProfilePage(api: api))),
      ('Support', 'Send a complaint to the placement cell', Icons.support_agent_rounded, () => _open(context, SupportPage(api: api))),
      ('Change Password', 'Secure your account with email OTP', Icons.lock_outline_rounded, () => _open(context, ChangePasswordPage(api: api))),
      ('Logout', 'Sign out of this device', Icons.logout_rounded, () => _logout(context)),
    ];
    return ListView(
      padding: const EdgeInsets.fromLTRB(18, 24, 18, 32),
      children: [
        Text('More', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w800)),
        const SizedBox(height: 6),
        Text('Manage your account and get help.', style: Theme.of(context).textTheme.bodyMedium),
        const SizedBox(height: 22),
        ...items.map((item) => Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Card(
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 8),
                  leading: CircleAvatar(backgroundColor: colors.primaryContainer, foregroundColor: colors.primary, child: Icon(item.$3)),
                  title: Text(item.$1, style: const TextStyle(fontWeight: FontWeight.w700)),
                  subtitle: Text(item.$2),
                  trailing: const Icon(Icons.chevron_right_rounded),
                  onTap: item.$4,
                ),
              ),
            )),
      ],
    );
  }
}
