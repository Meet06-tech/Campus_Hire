import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;

void main() {
  runApp(const PlacementManagementApp());
}

class PlacementManagementApp extends StatelessWidget {
  const PlacementManagementApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Placement Management System',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.indigo),
        useMaterial3: true,
      ),
      home: const AuthPage(),
    );
  }
}

class AuthPage extends StatefulWidget {
  const AuthPage({super.key});

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  final _name = TextEditingController();
  final _enrollment = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
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
    setState(() {
      _loading = true;
      _message = null;
    });
    final path = _registering ? 'register' : 'login';
    final body = _registering
        ? {
            'fullName': _name.text,
            'enrollmentNumber': _enrollment.text,
            'universityEmail': _email.text,
            'password': _password.text,
          }
        : {
            'enrollmentNumber': _enrollment.text,
            'password': _password.text,
          };
    try {
      final response = await http.post(
        Uri.parse('http://localhost:8080/api/auth/$path'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );
      final data = jsonDecode(response.body) as Map<String, dynamic>;
      setState(() {
        _message = response.statusCode < 300
            ? 'Success. Welcome, ${data['fullName']}.'
            : (data['detail'] ?? data['message'] ?? 'Request failed').toString();
      });
    } catch (_) {
      setState(() {
        _message = 'Could not connect to the backend. Start Spring Boot first.';
      });
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Placement Management System')),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Icon(Icons.school, size: 64),
                const SizedBox(height: 16),
                Text(_registering ? 'Create student account' : 'Sign in',
                    style: Theme.of(context).textTheme.headlineSmall),
                const SizedBox(height: 20),
                if (_registering) ...[
                  TextField(controller: _name, decoration: const InputDecoration(labelText: 'Full name')),
                  const SizedBox(height: 12),
                  TextField(controller: _email, decoration: const InputDecoration(labelText: 'University email')),
                  const SizedBox(height: 12),
                ],
                TextField(controller: _enrollment, decoration: const InputDecoration(labelText: 'Enrollment number')),
                const SizedBox(height: 12),
                TextField(controller: _password, obscureText: true, decoration: const InputDecoration(labelText: 'Password')),
                const SizedBox(height: 20),
                FilledButton(
                  onPressed: _loading ? null : _submit,
                  child: Text(_loading ? 'Please wait...' : (_registering ? 'Register' : 'Login')),
                ),
                TextButton(
                  onPressed: _loading ? null : () => setState(() {
                    _registering = !_registering;
                    _message = null;
                  }),
                  child: Text(_registering ? 'Already registered? Sign in' : 'New student? Create an account'),
                ),
                if (_message != null) ...[
                  const SizedBox(height: 12),
                  Text(_message!, textAlign: TextAlign.center),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}

