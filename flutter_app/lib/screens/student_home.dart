import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../models/placement_models.dart';
import '../services/placement_api.dart';
import 'profile_page.dart';
import 'settings_page.dart';

class StudentHomeShell extends StatefulWidget {
  const StudentHomeShell({
    super.key,
    required this.api,
    required this.onLogout,
    this.themeMode = ThemeMode.light,
    this.onThemeModeChanged,
  });

  final PlacementApi api;
  final VoidCallback onLogout;
  final ThemeMode themeMode;
  final ValueChanged<ThemeMode>? onThemeModeChanged;

  @override
  State<StudentHomeShell> createState() => _StudentHomeShellState();
}

class _StudentHomeShellState extends State<StudentHomeShell>
    with WidgetsBindingObserver {
  int _selectedIndex = 0;
  int _refreshKey = 0;
  Uint8List? _photoBytes;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _loadPhoto();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && mounted) {
      setState(() => _refreshKey++);
    }
  }

  void _refreshPlacementData() {
    if (mounted) setState(() => _refreshKey++);
  }

  Future<void> _loadPhoto() async {
    try {
      final photo = await widget.api.fetchProfilePhoto();
      if (mounted) setState(() => _photoBytes = photo);
    } catch (_) {
      // The default avatar is the safe fallback when no photo is available.
    }
  }

  @override
  Widget build(BuildContext context) {
    final pages = <Widget>[
      StudentDrivesTab(
        key: ValueKey('drives-$_refreshKey'),
        api: widget.api,
        onSessionExpired: widget.onLogout,
        onApplicationChanged: _refreshPlacementData,
      ),
      ApplicationsTab(
        key: ValueKey('applied-$_refreshKey'),
        api: widget.api,
        onSessionExpired: widget.onLogout,
        title: 'Applied applications',
        historyOnly: false,
      ),
      ApplicationsTab(
        key: ValueKey('history-$_refreshKey'),
        api: widget.api,
        onSessionExpired: widget.onLogout,
        title: 'Application history',
        historyOnly: true,
      ),
    ];
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
        onSettings: () => Navigator.of(context).push(
          MaterialPageRoute(
            builder: (_) => SettingsPage(
              themeMode: widget.themeMode,
              onThemeChanged: widget.onThemeModeChanged ?? (_) {},
              onLogout: widget.onLogout,
            ),
          ),
        ),
        onLogout: widget.onLogout,
      ),
      body: IndexedStack(index: _selectedIndex, children: pages),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) => setState(() {
          _selectedIndex = index;
          _refreshKey++;
        }),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.assignment_outlined),
            selectedIcon: Icon(Icons.assignment),
            label: 'Applied',
          ),
          NavigationDestination(
            icon: Icon(Icons.history_outlined),
            selectedIcon: Icon(Icons.history),
            label: 'History',
          ),
        ],
      ),
    );
  }
}

class _CampusHireAppBar extends StatelessWidget implements PreferredSizeWidget {
  const _CampusHireAppBar({
    required this.photoBytes,
    required this.onProfile,
    required this.onSettings,
    required this.onLogout,
  });

  final Uint8List? photoBytes;
  final VoidCallback onProfile;
  final VoidCallback onSettings;
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
            backgroundImage: photoBytes == null
                ? null
                : MemoryImage(photoBytes!),
            child: photoBytes == null
                ? const Icon(Icons.account_circle_outlined)
                : null,
          ),
          onSelected: (value) {
            if (value == 'profile') onProfile();
            if (value == 'settings') onSettings();
            if (value == 'logout') _confirmLogout(context);
          },
          itemBuilder: (_) => const [
            PopupMenuItem(value: 'profile', child: Text('View Profile')),
            PopupMenuItem(value: 'settings', child: Text('Settings')),
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
        content: const Text(
          'You will need to sign in again to access your account.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Logout'),
          ),
        ],
      ),
    );
    if (shouldLogout == true) onLogout();
  }
}

class StudentDrivesTab extends StatefulWidget {
  const StudentDrivesTab({
    super.key,
    required this.api,
    required this.onSessionExpired,
    required this.onApplicationChanged,
  });

  final PlacementApi api;
  final VoidCallback onSessionExpired;
  final VoidCallback onApplicationChanged;

  @override
  State<StudentDrivesTab> createState() => _StudentDrivesTabState();
}

class _StudentDrivesTabState extends State<StudentDrivesTab> {
  List<PlacementDrive> _drives = const [];
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadDrives();
  }

  Future<void> _loadDrives() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final drives = await widget.api.fetchStudentDrives();
      if (mounted) setState(() => _drives = drives);
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        widget.onSessionExpired();
      } else if (mounted) {
        setState(() => _error = error.message);
      }
    } catch (_) {
      if (mounted) setState(() => _error = 'Could not load placement drives.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _ErrorState(message: _error!, onRetry: _loadDrives);
    }
    if (_drives.isEmpty) {
      return const _EmptyState(
        icon: Icons.work_outline,
        title: 'No placement drives yet',
        message:
            'Published opportunities matching your profile will appear here.',
      );
    }
    return RefreshIndicator(
      onRefresh: _loadDrives,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(16, 22, 16, 32),
        itemCount: _drives.length + 1,
        itemBuilder: (context, index) {
          if (index == 0) {
            return Padding(
              padding: const EdgeInsets.fromLTRB(0, 4, 0, 18),
              child: _WelcomeCard(driveCount: _drives.length),
            );
          }
          final drive = _drives[index - 1];
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: _DriveCard(
              drive: drive,
              onTap: () => Navigator.of(context)
                  .push(
                    MaterialPageRoute(
                      builder: (_) => DriveDetailsPage(
                        api: widget.api,
                        drive: drive,
                        onSessionExpired: widget.onSessionExpired,
                        onApplicationChanged: widget.onApplicationChanged,
                      ),
                    ),
                  )
                  .then((_) {
                    if (mounted) _loadDrives();
                  }),
            ),
          );
        },
      ),
    );
  }
}

class DriveDetailsPage extends StatefulWidget {
  const DriveDetailsPage({
    super.key,
    required this.api,
    required this.drive,
    required this.onSessionExpired,
    required this.onApplicationChanged,
  });

  final PlacementApi api;
  final PlacementDrive drive;
  final VoidCallback onSessionExpired;
  final VoidCallback onApplicationChanged;

  @override
  State<DriveDetailsPage> createState() => _DriveDetailsPageState();
}

class _DriveDetailsPageState extends State<DriveDetailsPage> {
  bool _applying = false;
  String? _error;

  Future<void> _apply() async {
    setState(() {
      _applying = true;
      _error = null;
    });
    try {
      await widget.api.applyToDrive(widget.drive.id);
      widget.onApplicationChanged();
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Application submitted successfully.')),
      );
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        widget.onSessionExpired();
      } else if (mounted) {
        setState(() => _error = error.message);
      }
    } catch (_) {
      if (mounted) {
        setState(() => _error = 'Could not submit your application.');
      }
    } finally {
      if (mounted) setState(() => _applying = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final drive = widget.drive;
    return Scaffold(
      appBar: AppBar(title: const Text('Drive details')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          _DriveHero(drive: drive),
          const SizedBox(height: 16),
          if (drive.jobDescription?.isNotEmpty == true)
            _DetailsCard(
              title: 'About this role',
              icon: Icons.description_outlined,
              child: Text(
                drive.jobDescription!,
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                  height: 1.45,
                ),
              ),
            ),
          _DetailsCard(
            title: 'Drive information',
            icon: Icons.work_outline,
            child: Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                _DetailTile(label: 'Job type', value: drive.jobType),
                _DetailTile(label: 'Location', value: drive.location),
                _DetailTile(label: 'Package', value: drive.packageSalary),
                _DetailTile(
                  label: 'Deadline',
                  value: _formatDate(drive.applicationDeadline),
                ),
              ],
            ),
          ),
          _DetailsCard(
            title: 'Eligibility',
            icon: Icons.verified_user_outlined,
            child: Column(
              children: [
                _EligibilityRow(
                  label: 'Minimum CGPA',
                  value: drive.minimumCgpa?.toString() ?? 'Not specified',
                ),
                _EligibilityRow(
                  label: 'Graduation year',
                  value: drive.graduationYear?.toString() ?? 'Not specified',
                ),
                _EligibilityRow(
                  label: 'Active backlogs',
                  value:
                      drive.maximumActiveBacklogs?.toString() ??
                      'Not specified',
                ),
                _EligibilityRow(
                  label: 'Programs',
                  value: drive.eligibleProgramCodes.isEmpty
                      ? 'Open to all eligible programs'
                      : drive.eligibleProgramCodes.join(', '),
                  isLast: true,
                ),
              ],
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.errorContainer,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Text(
                _error!,
                style: TextStyle(
                  color: Theme.of(context).colorScheme.onErrorContainer,
                ),
              ),
            ),
          ],
          const SizedBox(height: 24),
          if (drive.status == 'PUBLISHED')
            SizedBox(
              height: 54,
              child: FilledButton.icon(
                onPressed: _applying ? null : _apply,
                icon: _applying
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.send_rounded),
                label: Text(
                  _applying
                      ? 'Submitting application...'
                      : 'Apply for this drive',
                ),
              ),
            )
          else
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerHighest,
                borderRadius: BorderRadius.circular(14),
              ),
              child: Text('Applications are closed for this drive.'),
            ),
        ],
      ),
    );
  }
}

class ApplicationsTab extends StatefulWidget {
  const ApplicationsTab({
    super.key,
    required this.api,
    required this.onSessionExpired,
    required this.title,
    required this.historyOnly,
  });

  final PlacementApi api;
  final VoidCallback onSessionExpired;
  final String title;
  final bool historyOnly;

  @override
  State<ApplicationsTab> createState() => _ApplicationsTabState();
}

class _ApplicationsTabState extends State<ApplicationsTab> {
  List<StudentApplicationProgress> _applications = const [];
  String? _error;
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadApplications();
  }

  Future<void> _loadApplications() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final applications = widget.historyOnly
          ? await widget.api.fetchMyApplicationHistory()
          : await widget.api.fetchMyApplicationProgress();
      if (mounted) setState(() => _applications = applications);
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        widget.onSessionExpired();
      } else if (mounted) {
        setState(() => _error = error.message);
      }
    } catch (_) {
      if (mounted) setState(() => _error = 'Could not load your applications.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _decideOffer(
    String decision,
    StudentApplicationProgress application,
  ) async {
    final action = decision == 'ACCEPTED' ? 'accept' : 'decline';
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('${action[0].toUpperCase()}${action.substring(1)} offer?'),
        content: Text('This decision cannot be changed later.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(action[0].toUpperCase() + action.substring(1)),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await widget.api.decideOffer(application.id, decision);
      await _loadApplications();
    } on ApiException catch (error) {
      if (error.statusCode == 401) {
        widget.onSessionExpired();
      } else if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(error.message)));
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_loading) return const Center(child: CircularProgressIndicator());
    if (_error != null) {
      return _ErrorState(message: _error!, onRetry: _loadApplications);
    }
    final visibleApplications = _applications;
    if (visibleApplications.isEmpty) {
      return _EmptyState(
        icon: Icons.assignment_outlined,
        title: widget.title,
        message: widget.historyOnly
            ? 'Completed placement outcomes will appear here.'
            : 'Your applications will appear here.',
      );
    }
    return RefreshIndicator(
      onRefresh: _loadApplications,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 22, 16, 32),
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(4, 0, 4, 18),
            child: Text(
              widget.title,
              style: Theme.of(context).textTheme.headlineSmall
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
          ),
          ...visibleApplications.map(
            (application) => widget.historyOnly
                ? _HistoryApplicationCard(
                    application: application,
                    onDecision: (decision) =>
                        _decideOffer(decision, application),
                  )
                : _ApplicationCard(application: application),
          ),
        ],
      ),
    );
  }
}

class _HistoryApplicationCard extends StatelessWidget {
  const _HistoryApplicationCard({
    required this.application,
    required this.onDecision,
  });

  final StudentApplicationProgress application;
  final ValueChanged<String> onDecision;

  @override
  Widget build(BuildContext context) {
    final selected =
        application.finalOutcome == 'SELECTED' ||
        application.applicationStatus == 'SELECTED';
    final colors = Theme.of(context).colorScheme;
    final normalizedOfferStatus = (application.offerStatus ?? 'PENDING')
        .toUpperCase();
    final offerPending = selected && normalizedOfferStatus == 'PENDING';
    final offerLabel = switch (normalizedOfferStatus) {
      'ACCEPTED' => 'Offer Accepted',
      'DECLINED' => 'Offer Declined',
      _ => 'Offer Pending',
    };
    final offerBadgeBackground = switch (normalizedOfferStatus) {
      'ACCEPTED' => Colors.green.shade50,
      'DECLINED' => Colors.red.shade50,
      _ => colors.primaryContainer,
    };
    final offerBadgeText = switch (normalizedOfferStatus) {
      'ACCEPTED' => Colors.green.shade700,
      'DECLINED' => Colors.red.shade700,
      _ => colors.onPrimaryContainer,
    };
    final companyResult = selected ? 'Selected' : 'Rejected';
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Column(
          children: [
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: CircleAvatar(
                backgroundColor: Theme.of(context).colorScheme.primaryContainer,
                child: Icon(
                  Icons.business_outlined,
                  color: Theme.of(context).colorScheme.primary,
                ),
              ),
              title: Text(
                application.companyName,
                style: const TextStyle(fontWeight: FontWeight.w800),
              ),
              subtitle: Text('Applied ${_formatDate(application.appliedAt)}'),
              trailing: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 7,
                ),
                decoration: BoxDecoration(
                  color: selected
                      ? offerBadgeBackground
                      : colors.errorContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  selected ? offerLabel : 'Not Selected',
                  style: TextStyle(
                    color: selected ? offerBadgeText : colors.onErrorContainer,
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            ),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                'Company result: $companyResult',
                style: TextStyle(color: colors.onSurfaceVariant, fontSize: 13),
              ),
            ),
            if (offerPending) ...[
              const Divider(),
              const Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Offer decision',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => onDecision('DECLINED'),
                      child: const Text('Decline'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      onPressed: () => onDecision('ACCEPTED'),
                      child: const Text('Accept'),
                    ),
                  ),
                ],
              ),
            ] else if (selected && normalizedOfferStatus != 'PENDING')
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Decision recorded ${application.offerDecidedAt == null ? '' : 'on ${_formatDate(application.offerDecidedAt)}'}'
                      .trim(),
                  style: TextStyle(
                    color: colors.onSurfaceVariant,
                    fontSize: 13,
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}

class _DriveCard extends StatelessWidget {
  const _DriveCard({required this.drive, required this.onTap});

  final PlacementDrive drive;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: colors.primaryContainer,
                      borderRadius: BorderRadius.circular(14),
                    ),
                    child: Icon(Icons.business_rounded, color: colors.primary),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          drive.company.name,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            color: colors.primary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          'Placement opportunity',
                          style: TextStyle(
                            color: colors.onSurfaceVariant,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Icon(
                    Icons.arrow_forward_ios_rounded,
                    size: 16,
                    color: colors.onSurfaceVariant,
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Text(
                drive.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleLarge?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 14),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (drive.jobType?.isNotEmpty == true)
                    _InfoPill(icon: Icons.work_outline, text: drive.jobType!),
                  if (drive.location?.isNotEmpty == true)
                    _InfoPill(
                      icon: Icons.location_on_outlined,
                      text: drive.location!,
                    ),
                  if (drive.packageSalary?.isNotEmpty == true)
                    _InfoPill(
                      icon: Icons.payments_outlined,
                      text: drive.packageSalary!,
                    ),
                ],
              ),
              if (drive.applicationDeadline != null) ...[
                const SizedBox(height: 16),
                Divider(height: 1, color: colors.outlineVariant),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Icon(Icons.schedule_rounded, size: 17, color: colors.error),
                    const SizedBox(width: 7),
                    Text(
                      'Apply by ${_formatDate(drive.applicationDeadline)}',
                      style: TextStyle(
                        color: colors.onSurface,
                        fontWeight: FontWeight.w600,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _DriveHero extends StatelessWidget {
  const _DriveHero({required this.drive});

  final PlacementDrive drive;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: colors.primary,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.16),
                  borderRadius: BorderRadius.circular(14),
                ),
                child: const Icon(Icons.business_rounded, color: Colors.white),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  drive.company.name,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w700,
                    fontSize: 16,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 22),
          Text(
            drive.title,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
              fontSize: 26,
              height: 1.12,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            'Published placement opportunity',
            style: TextStyle(color: Colors.white.withValues(alpha: 0.8)),
          ),
        ],
      ),
    );
  }
}

class _DetailsCard extends StatelessWidget {
  const _DetailsCard({
    required this.title,
    required this.icon,
    required this.child,
  });

  final String title;
  final IconData icon;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      margin: const EdgeInsets.only(bottom: 14),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 20, color: colors.primary),
                const SizedBox(width: 9),
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
            child,
          ],
        ),
      ),
    );
  }
}

class _DetailTile extends StatelessWidget {
  const _DetailTile({required this.label, required this.value});

  final String label;
  final String? value;

  @override
  Widget build(BuildContext context) {
    if (value == null || value!.trim().isEmpty) return const SizedBox.shrink();
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: 145,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest.withValues(alpha: 0.45),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(color: colors.onSurfaceVariant, fontSize: 12),
          ),
          const SizedBox(height: 5),
          Text(
            value!,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

class _EligibilityRow extends StatelessWidget {
  const _EligibilityRow({
    required this.label,
    required this.value,
    this.isLast = false,
  });

  final String label;
  final String value;
  final bool isLast;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: isLast
          ? null
          : BoxDecoration(
              border: Border(bottom: BorderSide(color: colors.outlineVariant)),
            ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Text(
              label,
              style: TextStyle(color: colors.onSurfaceVariant),
            ),
          ),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
          ),
        ],
      ),
    );
  }
}

class _ApplicationCard extends StatelessWidget {
  const _ApplicationCard({required this.application});

  final StudentApplicationProgress application;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final statusColor = _statusColor(colors, application);
    final statusLabel = _applicationStatusLabel(application);
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                Container(
                  width: 46,
                  height: 46,
                  decoration: BoxDecoration(
                    color: colors.primaryContainer,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(
                    Icons.work_history_rounded,
                    color: colors.primary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        application.driveTitle,
                        style: const TextStyle(fontWeight: FontWeight.w800),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        '${application.companyName} · Applied ${_formatDate(application.appliedAt)}',
                        style: TextStyle(
                          color: colors.onSurfaceVariant,
                          fontSize: 13,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 7,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Text(
                    statusLabel,
                    style: TextStyle(
                      color: statusColor,
                      fontWeight: FontWeight.w700,
                      fontSize: 12,
                    ),
                  ),
                ),
              ],
            ),
            if (application.rounds.isNotEmpty) ...[
              const Divider(height: 28),
              const Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Recruitment progress',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
              const SizedBox(height: 12),
              ...application.rounds.map(
                (round) => _RoundProgressRow(round: round),
              ),
            ] else ...[
              const SizedBox(height: 14),
              const Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Recruitment rounds have not been configured yet.',
                  style: TextStyle(fontSize: 13),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _RoundProgressRow extends StatelessWidget {
  const _RoundProgressRow({required this.round});

  final StudentRoundProgress round;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final visible = round.resultsPublished;
    final label = visible ? _roundStatusLabel(round.status) : 'Results pending';
    final color = visible && round.status == 'CLEARED'
        ? Colors.green.shade700
        : visible && round.status == 'NOT_CLEARED'
        ? colors.error
        : colors.onSurfaceVariant;
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          CircleAvatar(
            radius: 14,
            backgroundColor: colors.primaryContainer,
            child: Text(
              '${round.order}',
              style: TextStyle(color: colors.primary),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(child: Text(round.name)),
          Text(
            label,
            style: TextStyle(color: color, fontWeight: FontWeight.w700),
          ),
        ],
      ),
    );
  }
}

String _applicationStatusLabel(StudentApplicationProgress application) {
  if (application.finalOutcome == 'SELECTED' ||
      application.applicationStatus == 'SELECTED') {
    return 'Selected';
  }
  if (application.finalOutcome == 'NOT_SELECTED' ||
      application.applicationStatus == 'REJECTED') {
    return 'Not selected';
  }
  if (application.applicationStatus == 'WITHDRAWN') {
    return 'Withdrawn';
  }
  return 'In progress';
}

String _roundStatusLabel(String status) {
  switch (status) {
    case 'CLEARED':
      return 'Cleared';
    case 'NOT_CLEARED':
      return 'Not cleared';
    case 'AWAITING_RESULT':
      return 'Awaiting result';
    default:
      return 'Upcoming';
  }
}

Color _statusColor(ColorScheme colors, StudentApplicationProgress application) {
  if (application.finalOutcome == 'SELECTED' ||
      application.applicationStatus == 'SELECTED') {
    return Colors.green.shade700;
  }
  if (application.finalOutcome == 'NOT_SELECTED' ||
      application.applicationStatus == 'REJECTED') {
    return colors.error;
  }
  if (application.applicationStatus == 'SHORTLISTED') {
    return Colors.orange.shade800;
  }
  if (application.applicationStatus == 'WITHDRAWN') {
    return colors.onSurfaceVariant;
  }
  return colors.primary;
}

class _InfoPill extends StatelessWidget {
  const _InfoPill({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: colors.surfaceContainerHighest.withValues(alpha: 0.55),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 15, color: colors.primary),
          const SizedBox(width: 6),
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 180),
            child: Text(text, overflow: TextOverflow.ellipsis),
          ),
        ],
      ),
    );
  }
}

class _WelcomeCard extends StatelessWidget {
  const _WelcomeCard({required this.driveCount});

  final int driveCount;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [colors.primary, const Color(0xFF1D4ED8)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(
            color: colors.primary.withValues(alpha: 0.22),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Your next opportunity\nstarts here',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 22,
                    height: 1.15,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '$driveCount published ${driveCount == 1 ? 'drive' : 'drives'} matched to your profile',
                  style: const TextStyle(
                    color: Color(0xFFDCEBFF),
                    height: 1.35,
                  ),
                ),
              ],
            ),
          ),
          Container(
            width: 58,
            height: 58,
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.16),
              shape: BoxShape.circle,
            ),
            child: const Icon(
              Icons.rocket_launch_rounded,
              color: Colors.white,
              size: 28,
            ),
          ),
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({
    required this.icon,
    required this.title,
    required this.message,
  });

  final IconData icon;
  final String title;
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
            CircleAvatar(
              radius: 34,
              backgroundColor: colors.primaryContainer,
              child: Icon(icon, color: colors.primary, size: 30),
            ),
            const SizedBox(height: 18),
            Text(
              title,
              style: Theme.of(context).textTheme.headlineSmall
                  ?.copyWith(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            Text(
              message,
              textAlign: TextAlign.center,
              style: TextStyle(color: colors.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final Future<void> Function() onRetry;

  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(28),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            Icons.error_outline,
            size: 42,
            color: Theme.of(context).colorScheme.error,
          ),
          const SizedBox(height: 12),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 16),
          OutlinedButton(onPressed: onRetry, child: const Text('Try again')),
        ],
      ),
    ),
  );
}

String _formatDate(DateTime? date) {
  if (date == null) return 'Not specified';
  final local = date.toLocal();
  return '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')}/${local.year}';
}
