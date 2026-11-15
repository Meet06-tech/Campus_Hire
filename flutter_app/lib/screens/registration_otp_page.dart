import 'dart:async';

import 'package:flutter/material.dart';

import '../services/placement_api.dart';
import '../models/registration_verification.dart';

class RegistrationOtpPage extends StatefulWidget {
  const RegistrationOtpPage({super.key, required this.api, required this.verification});

  final PlacementApi api;
  final RegistrationVerification verification;

  @override
  State<RegistrationOtpPage> createState() => _RegistrationOtpPageState();
}

class _RegistrationOtpPageState extends State<RegistrationOtpPage> {
  final _otp = TextEditingController();
  bool _loading = false;
  int _cooldown = 60;
  Timer? _timer;
  late RegistrationVerification _verification;

  @override
  void initState() {
    super.initState();
    _verification = widget.verification;
    _startCooldown();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _otp.dispose();
    super.dispose();
  }

  void _startCooldown() {
    _timer?.cancel();
    setState(() => _cooldown = 60);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return timer.cancel();
      if (_cooldown <= 1) {
        timer.cancel();
        setState(() => _cooldown = 0);
      } else {
        setState(() => _cooldown--);
      }
    });
  }

  Future<void> _verify() async {
    if (_otp.text.trim().length != 6) {
      _message('Enter the 6-digit OTP from your university email.');
      return;
    }
    setState(() => _loading = true);
    try {
      await widget.api.verifyRegistrationOtp(registrationId: _verification.registrationId, otp: _otp.text.trim());
      if (mounted) Navigator.pop(context, true);
    } on ApiException catch (error) {
      _message(error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _resend() async {
    if (_cooldown > 0 || _loading) return;
    setState(() => _loading = true);
    try {
      final next = await widget.api.resendRegistrationOtp(_verification.registrationId);
      if (mounted) {
        setState(() => _verification = next);
        _startCooldown();
        _message('A new OTP was sent to ${next.maskedEmail}.');
      }
    } on ApiException catch (error) {
      _message(error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _message(String message) {
    if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Verify your email')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Icon(Icons.mark_email_read_rounded, size: 64, color: Theme.of(context).colorScheme.primary),
          const SizedBox(height: 18),
          Text('Enter your registration OTP', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w800)),
          const SizedBox(height: 8),
          Text('We sent a 6-digit code to ${_verification.maskedEmail}. Your account will be created only after verification.'),
          const SizedBox(height: 28),
          TextField(
            controller: _otp,
            autofocus: true,
            maxLength: 6,
            keyboardType: TextInputType.number,
            textAlign: TextAlign.center,
            decoration: const InputDecoration(labelText: 'OTP', prefixIcon: Icon(Icons.lock_outline_rounded)),
          ),
          const SizedBox(height: 14),
          FilledButton(onPressed: _loading ? null : _verify, child: Text(_loading ? 'Verifying…' : 'Verify OTP')),
          const SizedBox(height: 8),
          TextButton(onPressed: _cooldown == 0 && !_loading ? _resend : null, child: Text(_cooldown == 0 ? 'Resend OTP' : 'Resend available in ${_cooldown}s')),
        ],
      ),
    );
  }
}
