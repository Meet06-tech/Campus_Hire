import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../services/placement_api.dart';
import 'profile_page.dart';

class StudentHomeShell extends StatefulWidget {
  const StudentHomeShell({super.key, required this.api, required this.onLogout});

  final PlacementApi api;
  final VoidCallback onLogout;

  @override
  State<StudentHomeShell> createState() => _StudentHomeShellState();
}

class _StudentHomeShellState extends State<StudentHomeShell> {
  int _selectedIndex = 0;
  Uint8List? _photoBytes;

  @override
  void initState() {
    super.initState();
    _loadPhoto();
  }

  Future<void> _loadPhoto() async {
    try {
      final photo = await widget.api.fetchProfilePhoto();
      if (mounted) setState(() => _photoBytes = photo);
    } catch (_) {
      // The default avatar is the safe fallback when no photo is available.
    }
  }

  static const _pages = <Widget>[
    _HomeTab(),
    _PlaceholderTab(
      title: 'Applied',
      icon: Icons.assignment_outlined,
      message: 'Your applications will appear here.',
    ),
    _PlaceholderTab(
      title: 'History',
      icon: Icons.history_outlined,
      message: 'Your placement history will appear here.',
    ),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: _CampusHireAppBar(
        photoBytes: _photoBytes,
        onProfile: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => ProfilePage(
              api: widget.api,
              onPhotoChanged: (photo) => setState(() => _photoBytes = photo),
            ),
          ),
        ),
        onLogout: widget.onLogout,
      ),
      body: IndexedStack(index: _selectedIndex, children: _pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) => setState(() => _selectedIndex = index),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Home'),
          NavigationDestination(icon: Icon(Icons.assignment_outlined), selectedIcon: Icon(Icons.assignment), label: 'Applied'),
          NavigationDestination(icon: Icon(Icons.history_outlined), selectedIcon: Icon(Icons.history), label: 'History'),
        ],
      ),
    );
  }
}

class _CampusHireAppBar extends StatelessWidget implements PreferredSizeWidget {
  const _CampusHireAppBar({required this.photoBytes, required this.onProfile, required this.onLogout});

  final Uint8List? photoBytes;
  final VoidCallback onProfile;
  final VoidCallback onLogout;

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      titleSpacing: 12,
      title: Row(
        children: [
          Image.asset(
            'assets/images/charusat_logo.png',
            width: 104,
            height: 38,
            fit: BoxFit.contain,
            errorBuilder: (_, _, _) => const Icon(Icons.school_outlined),
          ),
          const SizedBox(width: 10),
          const Flexible(
            child: Text('CampusHire', overflow: TextOverflow.ellipsis),
          ),
        ],
      ),
      actions: [
        PopupMenuButton<String>(
          tooltip: 'Account',
          icon: CircleAvatar(
            radius: 16,
            backgroundImage: photoBytes == null ? null : MemoryImage(photoBytes!),
            child: photoBytes == null ? const Icon(Icons.account_circle_outlined) : null,
          ),
          onSelected: (value) {
            if (value == 'profile') onProfile();
            if (value == 'logout') _confirmLogout(context);
          },
          itemBuilder: (_) => const [
            PopupMenuItem(value: 'profile', child: Text('View Profile')),
            PopupMenuItem(value: 'logout', child: Text('Logout')),
          ],
        ),
      ],
    );
  }

  Future<void> _confirmLogout(BuildContext context) async {
    final shouldLogout = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Log out?'),
        content: const Text('You will need to sign in again to access your account.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Logout')),
        ],
      ),
    );
    if (shouldLogout == true) onLogout();
  }
}

class _HomeTab extends StatelessWidget {
  const _HomeTab();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return ListView(
      padding: const EdgeInsets.fromLTRB(20, 28, 20, 32),
      children: [
        ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Welcome back', style: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w700)),
              const SizedBox(height: 8),
              Text('Your student placement dashboard, all in one place.', style: TextStyle(color: colors.onSurfaceVariant)),
              const SizedBox(height: 28),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(22),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(Icons.auto_awesome_outlined, color: colors.primary, size: 30),
                      const SizedBox(width: 16),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text('Your placement journey starts here', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 17)),
                            SizedBox(height: 8),
                            Text('Placement opportunities and application updates will be available here in future updates.'),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _PlaceholderTab extends StatelessWidget {
  const _PlaceholderTab({required this.title, required this.icon, required this.message});

  final String title;
  final IconData icon;
  final String message;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(radius: 34, backgroundColor: colors.primaryContainer, child: Icon(icon, color: colors.primary, size: 30)),
            const SizedBox(height: 18),
            Text(title, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            Text(message, textAlign: TextAlign.center, style: TextStyle(color: colors.onSurfaceVariant)),
          ],
        ),
      ),
    );
  }
}
