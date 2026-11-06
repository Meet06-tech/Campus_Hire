import 'package:flutter/material.dart';

import '../navigation/app_navigation.dart';
import '../services/placement_api.dart';
import '../theme/app_theme.dart';
import 'student_home.dart';
import 'welcome_screen.dart';

class AuthPage extends StatefulWidget {
  const AuthPage({
    super.key,
    required this.api,
    this.themeMode = ThemeMode.light,
    this.onThemeModeChanged,
    this.initialRegistering = false,
  });

  final PlacementApi api;
  final ThemeMode themeMode;
  final ValueChanged<ThemeMode>? onThemeModeChanged;
  final bool initialRegistering;

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> {
  final _name = TextEditingController();
  final _enrollment = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _confirmPassword = TextEditingController();
  final _formKey = GlobalKey<FormState>();
  late bool _registering;
  bool _loading = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  String? _message;

  @override
  void initState() {
    super.initState();
    _registering = widget.initialRegistering;
  }

  @override
  void dispose() {
    _name.dispose();
    _enrollment.dispose();
    _email.dispose();
    _password.dispose();
    _confirmPassword.dispose();
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
              confirmPassword: _confirmPassword.text,
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
              themeMode: widget.themeMode,
              onThemeModeChanged: widget.onThemeModeChanged,
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
      MaterialPageRoute(
        builder: (_) => WelcomeScreen(
          api: widget.api,
          themeMode: widget.themeMode,
          onThemeModeChanged: widget.onThemeModeChanged,
        ),
      ),
      (_) => false,
    );
  }

  void _toggleTheme() {
    final scope = AppThemeScope.maybeOf(context);
    if (scope != null) {
      scope.toggleTheme(context);
    } else if (widget.onThemeModeChanged != null) {
      final isDark = Theme.of(context).brightness == Brightness.dark;
      widget.onThemeModeChanged!(isDark ? ThemeMode.light : ThemeMode.dark);
    }
  }

  String? _required(String? value, String label) {
    if (value == null || value.trim().isEmpty) return '$label is required';
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Scaffold(
      appBar: AppBar(
        titleSpacing: 0,
        actions: [
          IconButton(
            onPressed: _toggleTheme,
            icon: Icon(
              isDark ? Icons.light_mode_rounded : Icons.dark_mode_rounded,
            ),
            tooltip: isDark ? 'Switch to light mode' : 'Switch to dark mode',
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ScrollConfiguration(
            behavior: ScrollConfiguration.of(context)
                .copyWith(scrollbars: false),
            child: SingleChildScrollView(
              physics: const BouncingScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 440),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // CDPC Branding Logo (Enlarged and clearly visible)
                      Center(
                        child: Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: isDark
                                ? const Color(0xFF1E293B)
                                : Colors.white,
                            borderRadius: BorderRadius.circular(22),
                            border: Border.all(
                              color: isDark
                                  ? const Color(0xFF334155)
                                  : const Color(0xFFE2E8F0),
                              width: 1.2,
                            ),
                            boxShadow: [
                              BoxShadow(
                                color: isDark
                                    ? Colors.black26
                                    : const Color(0xFF0F172A)
                                          .withValues(alpha: 0.08),
                                blurRadius: 16,
                                offset: const Offset(0, 5),
                              ),
                            ],
                          ),
                          child: ClipRRect(
                            borderRadius: BorderRadius.circular(5),
                            child: Image.asset(
                              'assets/images/cdpc_logo_square.png',
                              height: 80,
                              width: 80,
                              fit: BoxFit.cover,
                              errorBuilder: (_, _, _) => Icon(
                                Icons.school_rounded,
                                size: 52,
                                color: colors.primary,
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 18),

                      // Replaced App Name: Career Development and Placement Cell
                      const Text(
                        'Career Development and Placement Cell',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w800,
                          letterSpacing: -0.2,
                          height: 1.25,
                        ),
                      ),
                      const SizedBox(height: 5),
                      Text(
                        'Placement Management System',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                          color: colors.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 22),

                      // Auth State Heading
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 200),
                        transitionBuilder: (child, animation) =>
                            FadeTransition(opacity: animation, child: child),
                        child: Text(
                          _registering
                              ? 'Create student account'
                              : 'Welcome back',
                          key: ValueKey(_registering),
                          style: Theme.of(context).textTheme.headlineSmall
                              ?.copyWith(
                                fontWeight: FontWeight.w800,
                                letterSpacing: -0.3,
                              ),
                        ),
                      ),
                      const SizedBox(height: 6),
                      Text(
                        _registering
                            ? 'Use your university email to get started.'
                            : 'Sign in to access placement drives and opportunities.',
                        style: TextStyle(
                          fontSize: 14,
                          color: colors.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 22),

                      // Input Fields
                      if (_registering) ...[
                        TextFormField(
                          controller: _name,
                          textCapitalization: TextCapitalization.words,
                          decoration: const InputDecoration(
                            labelText: 'Full name',
                            prefixIcon: Icon(
                              Icons.person_outline_rounded,
                              size: 20,
                            ),
                          ),
                          validator: (value) => _required(value, 'Full name'),
                        ),
                        const SizedBox(height: 14),
                        TextFormField(
                          controller: _email,
                          keyboardType: TextInputType.emailAddress,
                          decoration: const InputDecoration(
                            labelText: 'University email',
                            prefixIcon: Icon(Icons.email_outlined, size: 20),
                          ),
                          validator: (value) =>
                              _required(value, 'University email'),
                        ),
                        const SizedBox(height: 14),
                      ],

                      TextFormField(
                        controller: _enrollment,
                        decoration: const InputDecoration(
                          labelText: 'Enrollment number',
                          prefixIcon: Icon(Icons.badge_outlined, size: 20),
                        ),
                        validator: (value) =>
                            _required(value, 'Enrollment number'),
                      ),
                      const SizedBox(height: 14),

                      TextFormField(
                        controller: _password,
                        obscureText: _obscurePassword,
                        decoration: InputDecoration(
                          labelText: 'Password',
                          prefixIcon: const Icon(
                            Icons.lock_outline_rounded,
                            size: 20,
                          ),
                          suffixIcon: IconButton(
                            icon: Icon(
                              _obscurePassword
                                  ? Icons.visibility_off_outlined
                                  : Icons.visibility_outlined,
                              size: 20,
                            ),
                            onPressed: () => setState(
                              () => _obscurePassword = !_obscurePassword,
                            ),
                          ),
                        ),
                        validator: (value) =>
                            (value == null || value.length < 8)
                            ? 'Password must contain at least 8 characters'
                            : null,
                      ),

                      if (_registering) ...[
                        const SizedBox(height: 14),
                        TextFormField(
                          controller: _confirmPassword,
                          obscureText: _obscureConfirmPassword,
                          decoration: InputDecoration(
                            labelText: 'Confirm password',
                            prefixIcon: const Icon(
                              Icons.lock_outline_rounded,
                              size: 20,
                            ),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscureConfirmPassword
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                                size: 20,
                              ),
                              onPressed: () => setState(
                                () => _obscureConfirmPassword =
                                    !_obscureConfirmPassword,
                              ),
                            ),
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Confirm password is required';
                            }
                            return value == _password.text
                                ? null
                                : 'Passwords do not match';
                          },
                        ),
                      ],

                      const SizedBox(height: 24),

                      // Primary Submit Button (Blue)
                      FilledButton(
                        onPressed: _loading ? null : _submit,
                        child: _loading
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2.2,
                                  color: Colors.white,
                                ),
                              )
                            : Text(_registering ? 'Register' : 'Sign in'),
                      ),
                      const SizedBox(height: 12),

                      // Toggle Button
                      TextButton(
                        onPressed: _loading
                            ? null
                            : () => setState(() {
                                _registering = !_registering;
                                _message = null;
                              }),
                        child: Text(
                          _registering
                              ? 'Already registered? Sign in'
                              : 'New student? Create an account',
                        ),
                      ),

                      if (_message != null) ...[
                        const SizedBox(height: 14),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 14,
                            vertical: 10,
                          ),
                          decoration: BoxDecoration(
                            color: _message!.contains('Account created')
                                ? const Color(0xFFDCFCE7)
                                : const Color(0xFFFEE2E2),
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(
                              color: _message!.contains('Account created')
                                  ? const Color(0xFF86EFAC)
                                  : const Color(0xFFFCA5A5),
                            ),
                          ),
                          child: Text(
                            _message!,
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w500,
                              color: _message!.contains('Account created')
                                  ? const Color(0xFF15803D)
                                  : const Color(0xFFB91C1C),
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
