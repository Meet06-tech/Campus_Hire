import 'package:flutter/material.dart';

import '../services/placement_api.dart';

enum _RecoveryStep { email, otp, password, complete }

class ForgotPasswordPage extends StatefulWidget {
  const ForgotPasswordPage({super.key, required this.api});

  final PlacementApi api;

  @override
  State<ForgotPasswordPage> createState() => _ForgotPasswordPageState();
}

class _ForgotPasswordPageState extends State<ForgotPasswordPage> {
  final _email = TextEditingController();
  final _otp = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  final _formKey = GlobalKey<FormState>();

  _RecoveryStep _step = _RecoveryStep.email;
  String? _recoveryToken;
  String? _message;
  bool _loading = false;

  @override
  void dispose() {
    _email.dispose();
    _otp.dispose();
    _password.dispose();
    _confirmPassword.dispose();
    super.dispose();
  }

  Future<void> _requestOtp() async {
    if (!_formKey.currentState!.validate()) return;
    await _run(() async {
      final response = await widget.api.requestForgotPassword(
        _email.text.trim(),
      );
      if (!mounted) return;
      setState(() {
        _message =
            response['message']?.toString() ??
            'If an account exists, a verification code has been sent.';
        _step = _RecoveryStep.otp;
      });
    });
  }

  Future<void> _verifyOtp() async {
    if (!_formKey.currentState!.validate()) return;
    await _run(() async {
      final response = await widget.api.verifyForgotPassword(
        email: _email.text.trim(),
        otp: _otp.text.trim(),
      );
      final token = response['recoveryToken'];
      if (token is! String || token.isEmpty) {
        throw const ApiException('Invalid recovery response.', 200);
      }
      if (!mounted) return;
      setState(() {
        _recoveryToken = token;
        _message = null;
        _step = _RecoveryStep.password;
      });
    });
  }

  Future<void> _resetPassword() async {
    if (!_formKey.currentState!.validate()) return;
    final token = _recoveryToken;
    if (token == null || token.isEmpty) {
      setState(() => _message = 'Please verify the OTP again.');
      return;
    }
    await _run(() async {
      await widget.api.resetForgotPassword(
        recoveryToken: token,
        newPassword: _password.text,
        confirmPassword: _confirmPassword.text,
      );
      if (!mounted) return;
      setState(() {
        _recoveryToken = null;
        _password.clear();
        _confirmPassword.clear();
        _message = null;
        _step = _RecoveryStep.complete;
      });
    });
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() {
      _loading = true;
      _message = null;
    });
    try {
      await action();
    } on ApiException catch (error) {
      if (mounted) setState(() => _message = error.message);
    } catch (_) {
      if (mounted) {
        setState(() => _message = 'The request could not be completed.');
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  String? _emailValidator(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) return 'Registered email is required';
    if (!RegExp(r'^[^@\s]+@[^@\s]+$').hasMatch(email)) {
      return 'Enter a valid email address';
    }
    return null;
  }

  String? _otpValidator(String? value) {
    if (!RegExp(r'^\d{6}$').hasMatch(value?.trim() ?? '')) {
      return 'Enter the 6-digit OTP';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final title = switch (_step) {
      _RecoveryStep.email => 'Forgot password?',
      _RecoveryStep.otp => 'Verify your email',
      _RecoveryStep.password => 'Create a new password',
      _RecoveryStep.complete => 'Password updated',
    };
    final subtitle = switch (_step) {
      _RecoveryStep.email =>
        'Enter your registered university email to receive an OTP.',
      _RecoveryStep.otp => 'Enter the 6-digit code sent to your email.',
      _RecoveryStep.password =>
        'Choose a new password for your CampusHire account.',
      _RecoveryStep.complete => 'You can now sign in with your new password.',
    };

    return Scaffold(
      appBar: AppBar(title: const Text('Password recovery')),
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
                    Icon(
                      Icons.lock_reset_rounded,
                      size: 54,
                      color: Theme.of(context).colorScheme.primary,
                    ),
                    const SizedBox(height: 18),
                    Text(
                      title,
                      textAlign: TextAlign.center,
                      style: Theme.of(context).textTheme.headlineSmall
                          ?.copyWith(fontWeight: FontWeight.w800),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      subtitle,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 24),
                    if (_message != null) ...[
                      _MessageBox(message: _message!),
                      const SizedBox(height: 16),
                    ],
                    if (_step == _RecoveryStep.email) ...[
                      TextFormField(
                        controller: _email,
                        keyboardType: TextInputType.emailAddress,
                        autofillHints: const [AutofillHints.email],
                        decoration: const InputDecoration(
                          labelText: 'Registered email',
                          prefixIcon: Icon(Icons.email_outlined),
                        ),
                        validator: _emailValidator,
                      ),
                      const SizedBox(height: 18),
                      _primaryButton('Send OTP', _requestOtp),
                    ] else if (_step == _RecoveryStep.otp) ...[
                      TextFormField(
                        controller: _email,
                        enabled: false,
                        decoration: const InputDecoration(
                          labelText: 'Registered email',
                          prefixIcon: Icon(Icons.email_outlined),
                        ),
                        validator: _emailValidator,
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _otp,
                        keyboardType: TextInputType.number,
                        maxLength: 6,
                        decoration: const InputDecoration(
                          labelText: 'OTP',
                          prefixIcon: Icon(Icons.password_rounded),
                        ),
                        validator: _otpValidator,
                      ),
                      const SizedBox(height: 4),
                      _primaryButton('Verify OTP', _verifyOtp),
                      TextButton(
                        onPressed: _loading
                            ? null
                            : () => setState(() => _step = _RecoveryStep.email),
                        child: const Text('Use a different email'),
                      ),
                    ] else if (_step == _RecoveryStep.password) ...[
                      TextFormField(
                        controller: _password,
                        obscureText: true,
                        autofillHints: const [AutofillHints.newPassword],
                        decoration: const InputDecoration(
                          labelText: 'New password',
                          prefixIcon: Icon(Icons.lock_outline_rounded),
                        ),
                        validator: (value) => value == null || value.length < 8
                            ? 'Use at least 8 characters'
                            : null,
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _confirmPassword,
                        obscureText: true,
                        autofillHints: const [AutofillHints.newPassword],
                        decoration: const InputDecoration(
                          labelText: 'Confirm password',
                          prefixIcon: Icon(Icons.lock_outline_rounded),
                        ),
                        validator: (value) => value != _password.text
                            ? 'Passwords do not match'
                            : null,
                      ),
                      const SizedBox(height: 18),
                      _primaryButton('Set new password', _resetPassword),
                    ] else ...[
                      const Icon(
                        Icons.check_circle_outline_rounded,
                        color: Colors.green,
                        size: 48,
                      ),
                      const SizedBox(height: 18),
                      FilledButton(
                        onPressed: () => Navigator.of(context).pop(),
                        child: const Text('Back to sign in'),
                      ),
                    ],
                    if (_step != _RecoveryStep.complete) ...[
                      const SizedBox(height: 8),
                      TextButton(
                        onPressed: _loading
                            ? null
                            : () => Navigator.of(context).pop(),
                        child: const Text('Back to sign in'),
                      ),
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

  Widget _primaryButton(String label, Future<void> Function() action) {
    return FilledButton(
      onPressed: _loading ? null : action,
      child: _loading
          ? const SizedBox(
              height: 20,
              width: 20,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Colors.white,
              ),
            )
          : Text(label),
    );
  }
}

class _MessageBox extends StatelessWidget {
  const _MessageBox({required this.message});
  final String message;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(12),
    decoration: BoxDecoration(
      color: Theme.of(context).colorScheme.errorContainer,
      borderRadius: BorderRadius.circular(12),
    ),
    child: Text(
      message,
      textAlign: TextAlign.center,
      style: TextStyle(color: Theme.of(context).colorScheme.onErrorContainer),
    ),
  );
}
