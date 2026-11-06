import 'package:flutter/material.dart';

import '../models/announcement.dart';
import '../services/placement_api.dart';

class AnnouncementsPage extends StatefulWidget {
  const AnnouncementsPage({super.key, required this.api, required this.onSessionExpired});

  final PlacementApi api;
  final VoidCallback onSessionExpired;

  @override
  State<AnnouncementsPage> createState() => _AnnouncementsPageState();
}

class _AnnouncementsPageState extends State<AnnouncementsPage> {
  late Future<List<Announcement>> _future;

  @override
  void initState() {
    super.initState();
    _future = widget.api.fetchAnnouncements();
  }

  Future<void> _reload() async {
    setState(() => _future = widget.api.fetchAnnouncements());
    try {
      await _future;
    } on ApiException catch (error) {
      if (error.statusCode == 401 && mounted) widget.onSessionExpired();
    }
  }

  String _date(DateTime value) {
    final local = value.toLocal();
    final date = '${local.day.toString().padLeft(2, '0')}/${local.month.toString().padLeft(2, '0')}/${local.year}';
    final time = TimeOfDay.fromDateTime(local).format(context);
    return '$date · $time';
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return RefreshIndicator(
      onRefresh: _reload,
      child: FutureBuilder<List<Announcement>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            final expired = snapshot.error is ApiException && (snapshot.error as ApiException).statusCode == 401;
            if (expired) {
              WidgetsBinding.instance.addPostFrameCallback((_) => widget.onSessionExpired());
            }
            return ListView(children: [
              SizedBox(height: MediaQuery.sizeOf(context).height * .28),
              Icon(Icons.wifi_off_rounded, size: 44, color: colors.outline),
              const SizedBox(height: 12),
              Center(child: Text('Could not load announcements', style: Theme.of(context).textTheme.titleMedium)),
              const SizedBox(height: 6),
              Center(child: Text('Pull down to try again.', style: Theme.of(context).textTheme.bodyMedium)),
            ]);
          }
          final announcements = snapshot.data ?? const <Announcement>[];
          if (announcements.isEmpty) {
            return ListView(children: [
              SizedBox(height: MediaQuery.sizeOf(context).height * .28),
              Icon(Icons.campaign_outlined, size: 52, color: colors.primary),
              const SizedBox(height: 14),
              Center(child: Text('No announcements yet', style: Theme.of(context).textTheme.titleMedium)),
              const SizedBox(height: 6),
              Center(child: Text('Important placement updates will appear here.', style: Theme.of(context).textTheme.bodyMedium)),
            ]);
          }
          return ListView.separated(
            padding: const EdgeInsets.fromLTRB(18, 20, 18, 28),
            itemCount: announcements.length,
            separatorBuilder: (_, _) => const SizedBox(height: 14),
            itemBuilder: (context, index) => _AnnouncementCard(announcement: announcements[index], date: _date(announcements[index].publishedAt)),
          );
        },
      ),
    );
  }
}

class _AnnouncementCard extends StatelessWidget {
  const _AnnouncementCard({required this.announcement, required this.date});

  final Announcement announcement;
  final String date;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Row(children: [
            CircleAvatar(backgroundColor: colors.primaryContainer, foregroundColor: colors.primary, child: const Icon(Icons.campaign_rounded)),
            const SizedBox(width: 12),
            Expanded(child: Text(announcement.subject, style: Theme.of(context).textTheme.labelLarge?.copyWith(color: colors.primary, fontWeight: FontWeight.w700))),
          ]),
          const SizedBox(height: 14),
          Text(announcement.title, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          Text(announcement.message, style: Theme.of(context).textTheme.bodyLarge?.copyWith(height: 1.5)),
          const SizedBox(height: 16),
          Text('${announcement.publisherName} · ${announcement.publisherRole}${announcement.departmentName == null ? ' · University-wide' : ' · ${announcement.departmentName}'}', style: Theme.of(context).textTheme.bodySmall),
          const SizedBox(height: 4),
          Text(date, style: Theme.of(context).textTheme.bodySmall?.copyWith(color: colors.outline)),
        ]),
      ),
    );
  }
}
