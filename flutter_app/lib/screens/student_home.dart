import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../models/placement_attachment.dart';
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
      // Default avatar fallback
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
            selectedIcon: Icon(Icons.home_rounded),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.assignment_outlined),
            selectedIcon: Icon(Icons.assignment_rounded),
            label: 'Applied',
          ),
          NavigationDestination(
            icon: Icon(Icons.history_outlined),
            selectedIcon: Icon(Icons.history_rounded),
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
    final colors = Theme.of(context).colorScheme;

    return AppBar(
      titleSpacing: 22,
      title: Row(
        mainAxisSize: MainAxisSize.max,
        children: [
          // CHARUSAT Logo
          Image.asset(
            'assets/images/charusat_logo.png',
            width: 180,
            height: 42,
            fit: BoxFit.contain,
            errorBuilder: (_, _, _) =>
                Icon(Icons.school_rounded, color: colors.primary, size: 26),
          ),
          const SizedBox(width: 12),
          Container(
            height: 20,
            width: 1,
            color: colors.outline.withValues(alpha: 0.5),
          ),
          const SizedBox(width: 12),
          // CDPC Logo (exact same displayed width and height)
          Image.asset(
            'assets/images/cdpc_logo_rect.jpg',
            width: 95,
            height: 50,
            fit: BoxFit.contain,
            errorBuilder: (_, _, _) => Icon(
              Icons.work_outline_rounded,
              size: 24,
              color: colors.secondary,
            ),
          ),
        ],
      ),
      actions: [
        PopupMenuButton<String>(
          tooltip: 'Account',
          icon: CircleAvatar(
            radius: 17,
            backgroundColor: colors.primaryContainer,
            backgroundImage: photoBytes == null
                ? null
                : MemoryImage(photoBytes!),
            child: photoBytes == null
                ? Icon(
                    Icons.person_outline_rounded,
                    size: 20,
                    color: colors.primary,
                  )
                : null,
          ),
          onSelected: (value) {
            if (value == 'profile') onProfile();
            if (value == 'settings') onSettings();
            if (value == 'logout') _confirmLogout(context);
          },
          itemBuilder: (_) => const [
            PopupMenuItem(
              value: 'profile',
              child: Row(
                children: [
                  Icon(Icons.person_outline_rounded, size: 20),
                  SizedBox(width: 12),
                  Text('View Profile'),
                ],
              ),
            ),
            PopupMenuItem(
              value: 'settings',
              child: Row(
                children: [
                  Icon(Icons.settings_outlined, size: 20),
                  SizedBox(width: 12),
                  Text('Settings'),
                ],
              ),
            ),
            PopupMenuDivider(),
            PopupMenuItem(
              value: 'logout',
              child: Row(
                children: [
                  Icon(Icons.logout_rounded, size: 20, color: Colors.redAccent),
                  SizedBox(width: 12),
                  Text('Logout', style: TextStyle(color: Colors.redAccent)),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(width: 6),
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
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
              foregroundColor: Colors.white,
            ),
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
        icon: Icons.work_outline_rounded,
        title: 'No placement drives yet',
        message:
            'Published opportunities matching your profile will appear here.',
      );
    }
    return RefreshIndicator(
      onRefresh: _loadDrives,
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 860),
          child: ListView.builder(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
            itemCount: _drives.length + 1,
            itemBuilder: (context, index) {
              if (index == 0) {
                return Padding(
                  padding: const EdgeInsets.only(bottom: 20),
                  child: _WelcomeCard(driveCount: _drives.length),
                );
              }
              final drive = _drives[index - 1];
              return Padding(
                padding: const EdgeInsets.only(bottom: 14),
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
        ),
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
  bool _attachmentsLoading = true;
  List<PlacementAttachment> _attachments = const [];
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
  void initState() {
    super.initState();
    widget.api
        .fetchDriveAttachments(widget.drive.id)
        .then((items) {
          if (mounted) {
            setState(() {
              _attachments = items;
              _attachmentsLoading = false;
            });
          }
        })
        .catchError((_) {
          if (mounted) setState(() => _attachmentsLoading = false);
        });
  }

  @override
  Widget build(BuildContext context) {
    final drive = widget.drive;
    return Scaffold(
      appBar: AppBar(title: const Text('Drive details')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 860),
          child: ListView(
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
                      height: 1.5,
                    ),
                  ),
                ),
              _DetailsCard(
                title: 'Drive Information',
                icon: Icons.work_outline_rounded,
                child: Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _DetailTile(label: 'Job type', value: drive.jobType),
                    _DetailTile(label: 'Location', value: drive.location),
                    _DetailTile(
                      label: 'CTC',
                      value: _money(drive.ctcAmount, drive.ctcCurrency),
                    ),
                    _DetailTile(
                      label: 'Deadline',
                      value: _formatDate(drive.applicationDeadline),
                    ),
                  ],
                ),
              ),
              if (_hasOptionalDetails(drive))
                _DetailsCard(
                  title: 'Opportunity Details',
                  icon: Icons.payments_outlined,
                  child: Wrap(
                    spacing: 10,
                    runSpacing: 10,
                    children: [
                      _DetailTile(
                        label: 'Employment',
                        value: drive.employmentType,
                      ),
                      _DetailTile(label: 'Position', value: drive.positionType),
                      _DetailTile(
                        label: 'CTC',
                        value: _money(drive.ctcAmount, drive.ctcCurrency),
                      ),
                      _DetailTile(label: 'Stipend', value: drive.stipend),
                    ],
                  ),
                ),
              if (drive.otherBenefits?.trim().isNotEmpty == true)
                _DetailsCard(
                  title: 'Other Benefits',
                  icon: Icons.card_giftcard_outlined,
                  child: Text(drive.otherBenefits!),
                ),
              if (drive.bondDetails?.trim().isNotEmpty == true)
                _DetailsCard(
                  title: 'Bond Details',
                  icon: Icons.assignment_late_outlined,
                  child: Text(drive.bondDetails!),
                ),
              if (!_attachmentsLoading && _attachments.isNotEmpty)
                _DetailsCard(
                  title: 'Attachments',
                  icon: Icons.attach_file_rounded,
                  child: Column(
                    children: _attachments
                        .map(
                          (file) => ListTile(
                            contentPadding: EdgeInsets.zero,
                            leading: const Icon(
                              Icons.insert_drive_file_outlined,
                            ),
                            title: Text(file.fileName),
                            subtitle: Text(file.contentType),
                            trailing: const Icon(Icons.download_outlined),
                          ),
                        )
                        .toList(),
                  ),
                ),
              _DetailsCard(
                title: 'Eligibility Criteria',
                icon: Icons.verified_user_outlined,
                child: Column(
                  children: [
                    _EligibilityRow(
                      label: 'Minimum CGPA',
                      value: drive.minimumCgpa?.toString() ?? 'Not specified',
                    ),
                    _EligibilityRow(
                      label: 'Graduation year',
                      value:
                          drive.graduationYear?.toString() ?? 'Not specified',
                    ),
                    _EligibilityRow(
                      label: 'Active backlogs',
                      value:
                          drive.maximumActiveBacklogs?.toString() ??
                          'Not specified',
                    ),
                    _EligibilityRow(
                      label: 'Eligible programs',
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
                  height: 52,
                  child: FilledButton.icon(
                    onPressed: _applying ? null : _apply,
                    icon: _applying
                        ? const SizedBox(
                            width: 18,
                            height: 18,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
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
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerHighest
                        .withValues(alpha: 0.5),
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: const Center(
                    child: Text('Applications are closed for this drive.'),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  bool _hasOptionalDetails(PlacementDrive drive) => [
    drive.employmentType,
    drive.positionType,
    drive.ctcAmount?.toString(),
    drive.stipend,
  ].any((value) => value?.trim().isNotEmpty == true);

  String? _money(double? amount, String? currency) => amount == null
      ? null
      : '${currency ?? ''} ${amount.toStringAsFixed(2)}'.trim();
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
        content: const Text('This decision cannot be changed later.'),
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
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 860),
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 32),
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(4, 0, 4, 16),
                child: Text(
                  widget.title,
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.2,
                  ),
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
        ),
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
    final applicationRejected =
        application.applicationStatus == 'APPLICATION_REJECTED';
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
      margin: const EdgeInsets.only(bottom: 14),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            ListTile(
              contentPadding: EdgeInsets.zero,
              leading: Container(
                width: 44,
                height: 44,
                decoration: BoxDecoration(
                  color: colors.primaryContainer,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(Icons.business_rounded, color: colors.primary),
              ),
              title: Text(
                application.companyName,
                style: const TextStyle(fontWeight: FontWeight.w800),
              ),
              subtitle: Text(
                'Applied ${_formatDate(application.appliedAt)}',
                style: TextStyle(color: colors.onSurfaceVariant, fontSize: 12),
              ),
              trailing: Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 6,
                ),
                decoration: BoxDecoration(
                  color: selected
                      ? offerBadgeBackground
                      : colors.errorContainer,
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Text(
                  selected
                      ? offerLabel
                      : applicationRejected
                      ? 'Application Rejected'
                      : 'Not Selected',
                  style: TextStyle(
                    color: selected ? offerBadgeText : colors.onErrorContainer,
                    fontWeight: FontWeight.w700,
                    fontSize: 12,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 6),
            Align(
              alignment: Alignment.centerLeft,
              child: Text(
                applicationRejected
                    ? 'Application rejected before recruitment rounds'
                    : 'Company result: $companyResult',
                style: TextStyle(color: colors.onSurfaceVariant, fontSize: 13),
              ),
            ),
            if (offerPending) ...[
              const Divider(height: 24),
              const Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Offer decision',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () => onDecision('DECLINED'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: colors.error,
                        side: BorderSide(color: colors.error),
                      ),
                      child: const Text('Decline'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: FilledButton(
                      onPressed: () => onDecision('ACCEPTED'),
                      child: const Text('Accept'),
                    ),
                  ),
                ],
              ),
            ] else if (selected && normalizedOfferStatus != 'PENDING')
              Padding(
                padding: const EdgeInsets.only(top: 8),
                child: Align(
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
      elevation: 0,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Padding(
          padding: const EdgeInsets.all(18),
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
                            fontSize: 14,
                          ),
                        ),
                        const SizedBox(height: 2),
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
                    Icons.chevron_right_rounded,
                    size: 22,
                    color: colors.onSurfaceVariant,
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Text(
                drive.title,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.2,
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  if (drive.jobType?.isNotEmpty == true)
                    _InfoPill(
                      icon: Icons.work_outline_rounded,
                      text: drive.jobType!,
                    ),
                  if (drive.location?.isNotEmpty == true)
                    _InfoPill(
                      icon: Icons.location_on_outlined,
                      text: drive.location!,
                    ),
                  if (drive.ctcAmount != null)
                    _InfoPill(
                      icon: Icons.payments_outlined,
                      text:
                          '${drive.ctcCurrency ?? ''} ${drive.ctcAmount!.toStringAsFixed(2)}'
                              .trim(),
                    ),
                ],
              ),
              if (drive.applicationDeadline != null) ...[
                const SizedBox(height: 14),
                Divider(
                  height: 1,
                  color: colors.outline.withValues(alpha: 0.5),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Icon(
                      Icons.schedule_rounded,
                      size: 16,
                      color: colors.secondary,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      'Apply by ${_formatDate(drive.applicationDeadline)}',
                      style: TextStyle(
                        color: colors.onSurface,
                        fontWeight: FontWeight.w600,
                        fontSize: 12,
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
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [colors.primary, const Color(0xFF0284C7)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(
            color: colors.primary.withValues(alpha: 0.22),
            blurRadius: 16,
            offset: const Offset(0, 6),
          ),
        ],
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
                  color: Colors.white.withValues(alpha: 0.2),
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
          const SizedBox(height: 20),
          Text(
            drive.title,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w800,
              fontSize: 24,
              height: 1.2,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            'Published placement opportunity',
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.8),
              fontSize: 13,
            ),
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
                const SizedBox(width: 10),
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 16,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 14),
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
      width: 150,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: colors.primaryContainer.withValues(alpha: 0.3),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: colors.primary.withValues(alpha: 0.15)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: TextStyle(color: colors.onSurfaceVariant, fontSize: 12),
          ),
          const SizedBox(height: 4),
          Text(
            value!,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14),
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
              border: Border(
                bottom: BorderSide(
                  color: colors.outline.withValues(alpha: 0.4),
                ),
              ),
            ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Text(
              label,
              style: TextStyle(color: colors.onSurfaceVariant, fontSize: 13),
            ),
          ),
          const SizedBox(width: 12),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13),
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
      margin: const EdgeInsets.only(bottom: 14),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            Row(
              children: [
                Container(
                  width: 44,
                  height: 44,
                  decoration: BoxDecoration(
                    color: colors.primaryContainer,
                    borderRadius: BorderRadius.circular(12),
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
                        style: const TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(height: 3),
                      Text(
                        '${application.companyName} · Applied ${_formatDate(application.appliedAt)}',
                        style: TextStyle(
                          color: colors.onSurfaceVariant,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
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
              const Divider(height: 24),
              const Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Recruitment progress',
                  style: TextStyle(fontWeight: FontWeight.w800, fontSize: 13),
                ),
              ),
              const SizedBox(height: 10),
              ...application.rounds.map(
                (round) => _RoundProgressRow(round: round),
              ),
            ] else ...[
              const SizedBox(height: 12),
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Recruitment rounds have not been configured yet.',
                  style: TextStyle(
                    fontSize: 13,
                    color: colors.onSurfaceVariant,
                  ),
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
        : visible &&
              (round.status == 'NOT_CLEARED' ||
                  round.status == 'NOT_APPLICABLE')
        ? colors.error
        : colors.onSurfaceVariant;

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        children: [
          CircleAvatar(
            radius: 12,
            backgroundColor: colors.primaryContainer,
            child: Text(
              '${round.order}',
              style: TextStyle(
                color: colors.primary,
                fontSize: 11,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              round.name,
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w500),
            ),
          ),
          Text(
            label,
            style: TextStyle(
              color: color,
              fontWeight: FontWeight.w700,
              fontSize: 12,
            ),
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
  if (application.applicationStatus == 'APPLICATION_REJECTED') {
    return 'Application rejected';
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
    case 'NOT_APPLICABLE':
      return 'Eliminated';
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
  if (application.applicationStatus == 'APPLICATION_REJECTED') {
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
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: colors.primaryContainer.withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: colors.primary.withValues(alpha: 0.15)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: colors.primary),
          const SizedBox(width: 6),
          ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 180),
            child: Text(
              text,
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: colors.onSurface,
              ),
              overflow: TextOverflow.ellipsis,
            ),
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
      padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 22),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [colors.primary, const Color(0xFF0284C7)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(22),
        boxShadow: [
          BoxShadow(
            color: colors.primary.withValues(alpha: 0.22),
            blurRadius: 18,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 4,
                  ),
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.2),
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: const Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.stars_rounded, size: 14, color: Colors.white),
                      SizedBox(width: 6),
                      Text(
                        'CHARUSAT · CDPC',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 11,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 0.5,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
                const Text(
                  'Placement Drives',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 22,
                    height: 1.15,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  '$driveCount published ${driveCount == 1 ? 'drive' : 'drives'} matched to your profile',
                  style: const TextStyle(
                    color: Color(0xFFE0F2FE),
                    fontSize: 13,
                    height: 1.35,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 14),
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.12),
                  blurRadius: 10,
                  offset: const Offset(0, 3),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: Image.asset(
                'assets/images/cdpc_logo_square.jpg',
                width: 52,
                height: 52,
                fit: BoxFit.cover,
                errorBuilder: (_, _, _) => const Icon(
                  Icons.rocket_launch_rounded,
                  color: Color(0xFF0060AA),
                  size: 28,
                ),
              ),
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
              style: Theme.of(context).textTheme.titleLarge
                  ?.copyWith(fontWeight: FontWeight.w800),
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
            Icons.error_outline_rounded,
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
