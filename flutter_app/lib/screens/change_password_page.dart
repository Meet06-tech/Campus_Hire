import 'package:flutter/material.dart';
import '../services/placement_api.dart';

class ChangePasswordPage extends StatefulWidget {
  const ChangePasswordPage({super.key, required this.api});
  final PlacementApi api;
  @override State<ChangePasswordPage> createState() => _ChangePasswordPageState();
}
class _ChangePasswordPageState extends State<ChangePasswordPage> {
  final _otp = TextEditingController(); final _password = TextEditingController(); final _confirm = TextEditingController(); final _form = GlobalKey<FormState>(); String? _email; bool _requested = false; bool _verified = false; bool _loading = false;
  @override void initState() { super.initState(); widget.api.fetchMe().then((profile) { if (mounted) setState(() => _email = profile.universityEmail); }); }
  @override void dispose() { _otp.dispose(); _password.dispose(); _confirm.dispose(); super.dispose(); }
  String _mask(String? email) { if (email == null || !email.contains('@')) return 'your registered university email'; final parts = email.split('@'); return '${parts[0].substring(0, 1)}•••@${parts[1]}'; }
  Future<void> _requestOtp() async { setState(() => _loading = true); try { await widget.api.requestPasswordOtp(); if (mounted) { setState(() => _requested = true); _message('OTP sent to ${_mask(_email)}'); } } on ApiException catch (e) { _message(e.message); } finally { if (mounted) setState(() => _loading = false); } }
  Future<void> _verify() async { if (_otp.text.length != 6) { _message('Enter the 6-digit OTP.'); return; } setState(() => _loading = true); try { await widget.api.verifyPasswordOtp(_otp.text.trim()); if (mounted) { setState(() => _verified = true); _message('OTP verified. Set your new password.'); } } on ApiException catch (e) { _message(e.message); } finally { if (mounted) setState(() => _loading = false); } }
  Future<void> _change() async { if (!_form.currentState!.validate()) return; setState(() => _loading = true); try { await widget.api.changePassword(_password.text, _confirm.text); if (mounted) { _message('Password changed successfully.'); Navigator.pop(context); } } on ApiException catch (e) { _message(e.message); } finally { if (mounted) setState(() => _loading = false); } }
  void _message(String message) { if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message))); }
  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Change Password')),
        body: Form(
          key: _form,
          child: ListView(
            padding: const EdgeInsets.all(20),
            children: [
              Text('Secure your account', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
              const SizedBox(height: 8),
              Text('We will send a one-time code to ${_mask(_email)}.'),
              const SizedBox(height: 22),
              FilledButton(onPressed: _loading ? null : _requestOtp, child: Text(_requested ? 'Send a new OTP' : 'Request OTP')),
              if (_requested) ...[
                const SizedBox(height: 18),
                TextFormField(controller: _otp, keyboardType: TextInputType.number, maxLength: 6, decoration: const InputDecoration(labelText: 'OTP'), enabled: !_verified),
                FilledButton.tonal(onPressed: _loading || _verified ? null : _verify, child: Text(_verified ? 'OTP verified' : 'Verify OTP')),
              ],
              if (_verified) ...[
                const SizedBox(height: 22),
                TextFormField(controller: _password, obscureText: true, decoration: const InputDecoration(labelText: 'New password'), validator: (v) => v == null || v.length < 8 ? 'Use at least 8 characters' : null),
                const SizedBox(height: 12),
                TextFormField(controller: _confirm, obscureText: true, decoration: const InputDecoration(labelText: 'Confirm new password'), validator: (v) => v != _password.text ? 'Passwords do not match' : null),
                const SizedBox(height: 18),
                FilledButton(onPressed: _loading ? null : _change, child: Text(_loading ? 'Saving…' : 'Change password')),
              ],
            ],
          ),
        ),
      );
}
