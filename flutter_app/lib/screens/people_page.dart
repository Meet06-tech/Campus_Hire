import 'package:flutter/material.dart';
import '../models/student_directory.dart';
import '../services/placement_api.dart';

class PeoplePage extends StatefulWidget {
  const PeoplePage({super.key, required this.api});
  final PlacementApi api;
  @override State<PeoplePage> createState() => _PeoplePageState();
}

class _PeoplePageState extends State<PeoplePage> {
  final _search = TextEditingController();
  late Future<List<StudentDirectoryEntry>> _future;
  @override void initState() { super.initState(); _future = widget.api.fetchStudentDirectory(); }
  @override void dispose() { _search.dispose(); super.dispose(); }
  void _load() => setState(() => _future = widget.api.fetchStudentDirectory(search: _search.text));
  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('People')),
        body: Column(children: [
          Padding(padding: const EdgeInsets.fromLTRB(18, 14, 18, 8), child: TextField(controller: _search, onSubmitted: (_) => _load(), textInputAction: TextInputAction.search, decoration: InputDecoration(prefixIcon: const Icon(Icons.search), hintText: 'Search students', suffixIcon: IconButton(onPressed: _load, icon: const Icon(Icons.arrow_forward_rounded))))),
          Expanded(child: FutureBuilder<List<StudentDirectoryEntry>>(future: _future, builder: (context, snapshot) {
            if (snapshot.connectionState == ConnectionState.waiting) return const Center(child: CircularProgressIndicator());
            if (snapshot.hasError) return _MessageState(message: 'Could not load people.', onRetry: _load);
            final people = snapshot.data ?? const <StudentDirectoryEntry>[];
            if (people.isEmpty) return const _MessageState(message: 'No students found.');
            return RefreshIndicator(onRefresh: () async => _load(), child: ListView.separated(padding: const EdgeInsets.all(18), itemCount: people.length, separatorBuilder: (_, _) => const SizedBox(height: 8), itemBuilder: (_, index) {
              final person = people[index];
              return Card(child: ListTile(leading: CircleAvatar(child: Text(person.fullName.isEmpty ? '?' : person.fullName[0].toUpperCase())), title: Text(person.fullName), subtitle: Text('${person.enrollmentNumber} · ${person.academicProgram ?? 'Program not provided'}\n${person.departmentName}')));
            }));
          }))
        ]),
      );
}

class _MessageState extends StatelessWidget {
  const _MessageState({required this.message, this.onRetry});
  final String message; final VoidCallback? onRetry;
  @override Widget build(BuildContext context) => Center(child: Column(mainAxisSize: MainAxisSize.min, children: [Text(message), if (onRetry != null) TextButton(onPressed: onRetry, child: const Text('Retry'))]));
}
