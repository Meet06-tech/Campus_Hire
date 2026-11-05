import 'package:flutter/material.dart';

import '../services/placement_api.dart';
import '../theme/app_theme.dart';
import 'auth_page.dart';

class WelcomeScreen extends StatelessWidget {
  const WelcomeScreen({
    super.key,
    required this.api,
    this.themeMode = ThemeMode.light,
    this.onThemeModeChanged,
  });

  final PlacementApi api;
  final ThemeMode themeMode;
  final ValueChanged<ThemeMode>? onThemeModeChanged;

  void _navigateToAuth(BuildContext context, {required bool isRegistering}) {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => AuthPage(
          api: api,
          themeMode: themeMode,
          onThemeModeChanged: onThemeModeChanged,
          initialRegistering: isRegistering,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    // Welcome screen strictly renders in light mode per design specifications.
    final lightTheme = AppTheme.light();
    final colors = lightTheme.colorScheme;

    return Theme(
      data: lightTheme,
      child: Scaffold(
        backgroundColor: Colors.white,
        body: SafeArea(
          child: LayoutBuilder(
            builder: (context, constraints) {
              return Center(
                child: SingleChildScrollView(
                  physics: const BouncingScrollPhysics(),
                  child: Container(
                    width: double.infinity,
                    alignment: Alignment.center,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 24,
                      vertical: 28,
                    ),
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 440),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        crossAxisAlignment: CrossAxisAlignment.center,
                        children: [
                          const SizedBox(height: 12),

                          // CHARUSAT University Logo Card
                          Container(
                            width: double.infinity,
                            constraints: const BoxConstraints(maxWidth: 390),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 24,
                              vertical: 20,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.white,
                              borderRadius: BorderRadius.circular(20),
                              border: Border.all(
                                color: const Color(0xFFE2E8F0),
                                width: 1.2,
                              ),
                              boxShadow: [
                                BoxShadow(
                                  color: const Color(0xFF0F172A).withValues(alpha: 0.06),
                                  blurRadius: 18,
                                  offset: const Offset(0, 6),
                                ),
                                BoxShadow(
                                  color: const Color(0xFF0F172A).withValues(alpha: 0.02),
                                  blurRadius: 4,
                                  offset: const Offset(0, 1),
                                ),
                              ],
                            ),
                            child: Image.asset(
                              'assets/images/charusat_logo.png',
                              height: 56,
                              fit: BoxFit.contain,
                              errorBuilder: (_, _, _) => Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(
                                    Icons.school_rounded,
                                    size: 32,
                                    color: colors.primary,
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    'CHARUSAT',
                                    style: TextStyle(
                                      fontSize: 22,
                                      fontWeight: FontWeight.w800,
                                      color: colors.primary,
                                      letterSpacing: 0.5,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                          const SizedBox(height: 32),

                          // University Slogan in Hindi (CHARUSAT)
                          const Text(
                            '“चारुसैट है तो सक्सेस है”',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                              color: Color(0xFF0F172A),
                              letterSpacing: 0.2,
                            ),
                          ),
                          const SizedBox(height: 10),

                          // Placement Tagline
                          const Text(
                            'Discover placement opportunities at CHARUSAT.',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w400,
                              color: Color(0xFF64748B),
                              height: 1.45,
                            ),
                          ),
                          const SizedBox(height: 32),

                          // Action Buttons & Audience note
                          ConstrainedBox(
                            constraints: const BoxConstraints(maxWidth: 380),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                // Sign in Button (Primary Filled Pill)
                                SizedBox(
                                  height: 52,
                                  child: FilledButton(
                                    onPressed: () => _navigateToAuth(
                                      context,
                                      isRegistering: false,
                                    ),
                                    style: FilledButton.styleFrom(
                                      backgroundColor: colors.primary,
                                      foregroundColor: Colors.white,
                                      shape: const StadiumBorder(),
                                      elevation: 0,
                                      textStyle: const TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w600,
                                        letterSpacing: 0.2,
                                      ),
                                    ),
                                    child: const Text('Sign in'),
                                  ),
                                ),
                                const SizedBox(height: 14),

                                // Create an account Button (Outlined Pill)
                                SizedBox(
                                  height: 52,
                                  child: OutlinedButton(
                                    onPressed: () => _navigateToAuth(
                                      context,
                                      isRegistering: true,
                                    ),
                                    style: OutlinedButton.styleFrom(
                                      foregroundColor: colors.primary,
                                      side: BorderSide(
                                        color: colors.primary,
                                        width: 1.5,
                                      ),
                                      shape: const StadiumBorder(),
                                      textStyle: const TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w600,
                                        letterSpacing: 0.2,
                                      ),
                                    ),
                                    child: const Text('Create an account'),
                                  ),
                                ),
                                const SizedBox(height: 22),

                                // Audience text
                                const Text(
                                  'For CHARUSAT students',
                                  textAlign: TextAlign.center,
                                  style: TextStyle(
                                    fontSize: 13,
                                    fontWeight: FontWeight.w500,
                                    color: Color(0xFF64748B),
                                  ),
                                ),
                              ],
                            ),
                          ),

                          // Natural, professional spacing to footer
                          const SizedBox(height: 48),

                          // Footer
                          const Text(
                            'Terms of Service · Privacy Policy',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w400,
                              color: Color(0xFF94A3B8),
                            ),
                          ),
                          const SizedBox(height: 12),
                        ],
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}
