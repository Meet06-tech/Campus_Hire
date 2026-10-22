import 'package:flutter/material.dart';

import '../navigation/app_navigation.dart';
import '../services/placement_api.dart';
import 'student_home.dart';

class AuthPage extends StatefulWidget {
  const AuthPage({super.key, required this.api});

  final PlacementApi api;

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  final _name = TextEditingController();
  final _enrollment = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  bool _registering = false;
  bool _loading = false;
  String? _message;

  @override
  void dispose() {
    _name.dispose();
    _enrollment.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _message = null;
    });
    try {
      final data = _registering
          ? await widget.api.register(
              fullName: _name.text.trim(),
              enrollmentNumber: _enrollment.text.trim(),
              universityEmail: _email.text.trim(),
              password: _password.text,
            )
          : await widget.api.login(_enrollment.text.trim(), _password.text);
      if (!_registering && data['token'] is String) {
        widget.api.setToken(data['token'] as String);
        if (!mounted) return;
        await Navigator.of(context).pushReplacement(
          MaterialPageRoute(
            builder: (_) => StudentHomeShell(
              api: widget.api,
              onLogout: () => _logout(),
            ),
          ),
        );
      } else if (mounted) {
        setState(() => _message = 'Account created. You can now sign in.');
      }
    } on ApiException catch (error) {
      if (mounted) setState(() => _message = error.message);
    } catch (_) {
      if (mounted) {
        setState(() => _message = 'Could not connect to the backend.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _logout() {
    widget.api.clearToken();
    appNavigatorKey.currentState?.pushAndRemoveUntil(
      MaterialPageRoute(builder: (_) => AuthPage(api: widget.api)),
      (_) => false,
    );
  }

  String? _required(String? value, String label) {
    if (value == null || value.trim().isEmpty) return '$label is required';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    CircleAvatar(
                      radius: 30,
                      backgroundColor: colors.primaryContainer,
                      child: Icon(Icons.school_outlined, color: colors.primary),
                    ),
                    const SizedBox(height: 20),
                    const Text(
                      'CampusHire',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 18, fontWeight: FontWeight.w700),
                    ),
                    const SizedBox(height: 4),
                    const Text('Placement Management System', textAlign: TextAlign.center),
                    const SizedBox(height: 16),
                    Text(
                      _registering ? 'Create student account' : 'Welcome back',
                      style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                            fontWeight: FontWeight.w700,
                          ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      _registering
                          ? 'Use your university email to get started.'
                          : 'Sign in to view your placement profile.',
                      style: TextStyle(color: colors.onSurfaceVariant),
                    ),
                    const SizedBox(height: 28),
                    if (_registering) ...[
                      TextFormField(
                        controller: _name,
                        textCapitalization: TextCapitalization.words,
                        decoration: const InputDecoration(labelText: 'Full name'),
                        validator: (value) => _required(value, 'Full name'),
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _email,
                        keyboardType: TextInputType.emailAddress,
                        decoration: const InputDecoration(labelText: 'University email'),
                        validator: (value) => _required(value, 'University email'),
                      ),
                      const SizedBox(height: 14),
                    ],
                    TextFormField(
                      controller: _enrollment,
                      decoration: const InputDecoration(labelText: 'Enrollment number'),
                      validator: (value) => _required(value, 'Enrollment number'),
                    ),
                    const SizedBox(height: 14),
                    TextFormField(
                      controller: _password,
                      obscureText: true,
                      decoration: const InputDecoration(labelText: 'Password'),
                      validator: (value) => (value == null || value.length < 8)
                          ? 'Password must contain at least 8 characters'
                          : null,
                    ),
                    const SizedBox(height: 24),
                    FilledButton(
                      onPressed: _loading ? null : _submit,
                      child: Text(_loading ? 'Please wait…' : (_registering ? 'Register' : 'Sign in')),
                    ),
                    TextButton(
                      onPressed: _loading
                          ? null
                          : () => setState(() {
                                _registering = !_registering;
                                _message = null;
                              }),
                      child: Text(_registering
                          ? 'Already registered? Sign in'
                          : 'New student? Create an account'),
                    ),
                    if (_message != null) ...[
                      const SizedBox(height: 10),
                      Text(_message!, textAlign: TextAlign.center),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
