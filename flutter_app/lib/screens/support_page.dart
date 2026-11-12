import 'package:flutter/material.dart';
import '../services/placement_api.dart';

class SupportPage extends StatefulWidget {
  const SupportPage({super.key, required this.api});
  final PlacementApi api;
  @override State<SupportPage> createState() => _SupportPageState();
}
class _SupportPageState extends State<SupportPage> {
  final _subject = TextEditingController(); final _message = TextEditingController(); final _form = GlobalKey<FormState>(); bool _saving = false;
  @override void dispose() { _subject.dispose(); _message.dispose(); super.dispose(); }
  Future<void> _submit() async { if (!_form.currentState!.validate()) return; setState(() => _saving = true); try { await widget.api.submitComplaint(subject: _subject.text.trim(), message: _message.text.trim()); if (mounted) { _subject.clear(); _message.clear(); ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Your complaint was sent successfully.'))); } } on ApiException catch (error) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message))); } finally { if (mounted) setState(() => _saving = false); } }
  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Support')),
        body: Form(
          key: _form,
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Text('How can we help?', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              const Text('Your complaint will be sent to the placement support team.'),
              const SizedBox(height: 24),
              TextFormField(controller: _subject, maxLength: 200, decoration: const InputDecoration(labelText: 'Subject'), validator: (value) => value == null || value.trim().isEmpty ? 'Subject is required' : null),
              const SizedBox(height: 12),
              TextFormField(controller: _message, maxLength: 10000, minLines: 6, maxLines: 10, decoration: const InputDecoration(labelText: 'Message', alignLabelWithHint: true), validator: (value) => value == null || value.trim().isEmpty ? 'Message is required' : null),
              const SizedBox(height: 18),
              FilledButton.icon(onPressed: _saving ? null : _submit, icon: const Icon(Icons.send_rounded), label: Text(_saving ? 'Sending…' : 'Send complaint')),
            ],
          ),
        ),
      );
}
