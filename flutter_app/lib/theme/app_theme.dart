import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

// ─────────────────────────────────────────────────────────────────────────────
// CampusHire Design System — CHARUSAT & CDPC Placement Management System
// ─────────────────────────────────────────────────────────────────────────────

/// Core brand colors for CHARUSAT University and CDPC.
abstract final class CHColors {
  // Official CHARUSAT & CDPC Brand Colors
  static const Color charusatBlue = Color(0xFF0060AA);
  static const Color charusatBlueDark = Color(0xFF004B87);
  static const Color charusatBlueLight = Color(0xFFE8F1FA);
  static const Color cdpcOrange = Color(0xFFEF781C);
  static const Color cdpcOrangeLight = Color(0xFFFFF3E6);

  // Hybrid & Primary aliases
  static const Color primaryBlue = charusatBlue;
  static const Color electricIndigo = Color(0xFF0060AA); // Aligned to official CHARUSAT blue
  static const Color deepViolet = Color(0xFF004B87);
  static const Color royalBlue = Color(0xFF0060AA);

  // Accent / CTA
  static const Color emerald = Color(0xFF10B981);
  static const Color emeraldDark = Color(0xFF059669);
  static const Color amber = Color(0xFFF59E0B);
  static const Color coral = Color(0xFFEF4444);

  // Semantic
  static const Color success = Color(0xFF22C55E);
  static const Color warning = Color(0xFFF59E0B);
  static const Color error = Color(0xFFEF4444);
  static const Color info = Color(0xFF0060AA);

  // Light theme surfaces
  static const Color lightBg = Color(0xFFF8FAFC);
  static const Color lightSurface = Color(0xFFFFFFFF);
  static const Color lightSurfaceDim = Color(0xFFF1F5F9);
  static const Color lightCard = Color(0xFFFFFFFF);
  static const Color lightBorder = Color(0xFFE2E8F0);

  // Dark theme surfaces
  static const Color darkBg = Color(0xFF0B0F1A);
  static const Color darkSurface = Color(0xFF111827);
  static const Color darkSurfaceElevated = Color(0xFF1F2937);
  static const Color darkCard = Color(0xFF151C2C);
  static const Color darkBorder = Color(0xFF374151);

  // Text
  static const Color lightTextPrimary = Color(0xFF0F172A);
  static const Color lightTextSecondary = Color(0xFF64748B);
  static const Color lightTextTertiary = Color(0xFF94A3B8);
  static const Color darkTextPrimary = Color(0xFFF9FAFB);
  static const Color darkTextSecondary = Color(0xFF9CA3AF);
  static const Color darkTextTertiary = Color(0xFF6B7280);

  // Brand gradients
  static const LinearGradient primaryGradient = LinearGradient(
    colors: [charusatBlue, Color(0xFF0284C7)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient cdpcGradient = LinearGradient(
    colors: [charusatBlue, cdpcOrange],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient heroGradient = LinearGradient(
    colors: [charusatBlue, Color(0xFF0284C7), Color(0xFF0EA5E9)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient successGradient = LinearGradient(
    colors: [Color(0xFF10B981), Color(0xFF059669)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient warningGradient = LinearGradient(
    colors: [Color(0xFFF59E0B), Color(0xFFD97706)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient errorGradient = LinearGradient(
    colors: [Color(0xFFEF4444), Color(0xFFDC2626)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );

  static const LinearGradient darkHeroGradient = LinearGradient(
    colors: [Color(0xFF1E3A8A), Color(0xFF2563EB), Color(0xFF60A5FA)],
    begin: Alignment.topLeft,
    end: Alignment.bottomRight,
  );
}

/// Typography scale used throughout the app.
abstract final class CHTypography {
  static const String fontFamily = 'Roboto';

  // Display
  static const TextStyle displayLarge = TextStyle(
    fontFamily: fontFamily,
    fontSize: 32,
    fontWeight: FontWeight.w800,
    height: 1.15,
    letterSpacing: -0.5,
  );

  static const TextStyle displayMedium = TextStyle(
    fontFamily: fontFamily,
    fontSize: 26,
    fontWeight: FontWeight.w800,
    height: 1.2,
    letterSpacing: -0.3,
  );

  // Heading
  static const TextStyle headingLarge = TextStyle(
    fontFamily: fontFamily,
    fontSize: 22,
    fontWeight: FontWeight.w700,
    height: 1.25,
    letterSpacing: -0.2,
  );

  static const TextStyle headingMedium = TextStyle(
    fontFamily: fontFamily,
    fontSize: 18,
    fontWeight: FontWeight.w700,
    height: 1.3,
  );

  static const TextStyle headingSmall = TextStyle(
    fontFamily: fontFamily,
    fontSize: 16,
    fontWeight: FontWeight.w700,
    height: 1.35,
  );

  // Body
  static const TextStyle bodyLarge = TextStyle(
    fontFamily: fontFamily,
    fontSize: 16,
    fontWeight: FontWeight.w400,
    height: 1.5,
  );

  static const TextStyle bodyMedium = TextStyle(
    fontFamily: fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w400,
    height: 1.45,
  );

  static const TextStyle bodySmall = TextStyle(
    fontFamily: fontFamily,
    fontSize: 12,
    fontWeight: FontWeight.w400,
    height: 1.4,
  );

  // Labels
  static const TextStyle labelLarge = TextStyle(
    fontFamily: fontFamily,
    fontSize: 14,
    fontWeight: FontWeight.w600,
    height: 1.4,
  );

  static const TextStyle labelMedium = TextStyle(
    fontFamily: fontFamily,
    fontSize: 12,
    fontWeight: FontWeight.w600,
    height: 1.35,
  );

  static const TextStyle labelSmall = TextStyle(
    fontFamily: fontFamily,
    fontSize: 10,
    fontWeight: FontWeight.w600,
    height: 1.3,
    letterSpacing: 0.5,
  );

  // Mono / Stats
  static const TextStyle statLarge = TextStyle(
    fontFamily: fontFamily,
    fontSize: 28,
    fontWeight: FontWeight.w800,
    height: 1.1,
    letterSpacing: -0.5,
  );

  static const TextStyle statMedium = TextStyle(
    fontFamily: fontFamily,
    fontSize: 20,
    fontWeight: FontWeight.w800,
    height: 1.15,
    letterSpacing: -0.3,
  );
}

/// Spacing constants.
abstract final class CHSpacing {
  static const double xs = 4;
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 20;
  static const double xxl = 24;
  static const double xxxl = 32;
  static const double huge = 40;
  static const double massive = 48;

  static const EdgeInsets screenPadding = EdgeInsets.fromLTRB(20, 0, 20, 0);
  static const EdgeInsets cardPadding = EdgeInsets.all(20);
  static const EdgeInsets sectionPadding = EdgeInsets.fromLTRB(20, 8, 20, 24);
}

/// Border radii.
abstract final class CHRadius {
  static const double sm = 8;
  static const double md = 12;
  static const double lg = 16;
  static const double xl = 20;
  static const double xxl = 24;
  static const double pill = 100;

  static final BorderRadius smBorder = BorderRadius.circular(sm);
  static final BorderRadius mdBorder = BorderRadius.circular(md);
  static final BorderRadius lgBorder = BorderRadius.circular(lg);
  static final BorderRadius xlBorder = BorderRadius.circular(xl);
  static final BorderRadius xxlBorder = BorderRadius.circular(xxl);
  static final BorderRadius pillBorder = BorderRadius.circular(pill);
}

/// Box shadow presets.
abstract final class CHShadows {
  static List<BoxShadow> soft(Color color) => [
        BoxShadow(
          color: color.withValues(alpha: 0.06),
          blurRadius: 16,
          offset: const Offset(0, 4),
        ),
        BoxShadow(
          color: color.withValues(alpha: 0.03),
          blurRadius: 6,
          offset: const Offset(0, 2),
        ),
      ];

  static List<BoxShadow> medium(Color color) => [
        BoxShadow(
          color: color.withValues(alpha: 0.10),
          blurRadius: 20,
          offset: const Offset(0, 6),
        ),
        BoxShadow(
          color: color.withValues(alpha: 0.04),
          blurRadius: 8,
          offset: const Offset(0, 2),
        ),
      ];

  static List<BoxShadow> glow(Color color) => [
        BoxShadow(
          color: color.withValues(alpha: 0.20),
          blurRadius: 18,
          offset: const Offset(0, 4),
        ),
      ];

  static const List<BoxShadow> none = [];
}

/// Reactive Scope Widget for theme state management.
///
/// Wraps MaterialApp and allows any screen or dialog in the application
/// to dynamically and repeatedly toggle themes without stale props.
class AppThemeScope extends InheritedWidget {
  const AppThemeScope({
    super.key,
    required this.themeMode,
    required this.onThemeModeChanged,
    required super.child,
  });

  final ThemeMode themeMode;
  final ValueChanged<ThemeMode> onThemeModeChanged;

  /// Retrieves the nearest [AppThemeScope].
  static AppThemeScope of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AppThemeScope>();
    assert(scope != null, 'No AppThemeScope found in context. Wrap MaterialApp with AppThemeScope.');
    return scope!;
  }

  /// Retrieves the nearest [AppThemeScope] if available.
  static AppThemeScope? maybeOf(BuildContext context) {
    return context.dependOnInheritedWidgetOfExactType<AppThemeScope>();
  }

  /// Toggles between light and dark mode reactively based on active brightness.
  void toggleTheme(BuildContext context) {
    final currentBrightness = Theme.of(context).brightness;
    final nextMode = currentBrightness == Brightness.dark
        ? ThemeMode.light
        : ThemeMode.dark;
    onThemeModeChanged(nextMode);
  }

  @override
  bool updateShouldNotify(AppThemeScope oldWidget) =>
      themeMode != oldWidget.themeMode;
}

/// The main app theme builder.
abstract final class AppTheme {
  static ThemeData light() {
    final scheme = ColorScheme(
      brightness: Brightness.light,
      primary: CHColors.charusatBlue,
      onPrimary: Colors.white,
      primaryContainer: CHColors.charusatBlueLight,
      onPrimaryContainer: const Color(0xFF003866),
      secondary: CHColors.cdpcOrange,
      onSecondary: Colors.white,
      secondaryContainer: CHColors.cdpcOrangeLight,
      onSecondaryContainer: const Color(0xFF8A4100),
      tertiary: CHColors.emerald,
      onTertiary: Colors.white,
      tertiaryContainer: const Color(0xFFD1FAE5),
      onTertiaryContainer: const Color(0xFF065F46),
      error: CHColors.error,
      onError: Colors.white,
      errorContainer: const Color(0xFFFEE2E2),
      onErrorContainer: const Color(0xFF991B1B),
      surface: CHColors.lightSurface,
      onSurface: CHColors.lightTextPrimary,
      onSurfaceVariant: CHColors.lightTextSecondary,
      outline: CHColors.lightBorder,
      outlineVariant: const Color(0xFFF1F5F9),
      shadow: const Color(0xFF0F172A),
    );
    return _build(scheme, CHColors.lightBg);
  }

  static ThemeData dark() {
    final scheme = ColorScheme(
      brightness: Brightness.dark,
      primary: const Color(0xFF60A5FA),
      onPrimary: const Color(0xFF002B4D),
      primaryContainer: const Color(0xFF004B87),
      onPrimaryContainer: const Color(0xFFBFDBFE),
      secondary: const Color(0xFFFBBF24),
      onSecondary: const Color(0xFF451A03),
      secondaryContainer: const Color(0xFF78350F),
      onSecondaryContainer: const Color(0xFFFDE68A),
      tertiary: const Color(0xFF34D399),
      onTertiary: const Color(0xFF064E3B),
      tertiaryContainer: const Color(0xFF064E3B),
      onTertiaryContainer: const Color(0xFFA7F3D0),
      error: const Color(0xFFFCA5A5),
      onError: const Color(0xFF7F1D1D),
      errorContainer: const Color(0xFF7F1D1D),
      onErrorContainer: const Color(0xFFFECACA),
      surface: CHColors.darkSurface,
      onSurface: CHColors.darkTextPrimary,
      onSurfaceVariant: CHColors.darkTextSecondary,
      outline: CHColors.darkBorder,
      outlineVariant: const Color(0xFF1F2937),
      shadow: Colors.black,
    );
    return _build(scheme, CHColors.darkBg);
  }

  static ThemeData _build(ColorScheme scheme, Color scaffoldBg) {
    final isDark = scheme.brightness == Brightness.dark;

    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: scaffoldBg,
      brightness: scheme.brightness,
      fontFamily: CHTypography.fontFamily,
      visualDensity: VisualDensity.adaptivePlatformDensity,

      // AppBar
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        systemOverlayStyle: isDark
            ? SystemUiOverlayStyle.light
            : SystemUiOverlayStyle.dark,
        titleTextStyle: CHTypography.headingMedium.copyWith(
          color: scheme.onSurface,
        ),
        iconTheme: IconThemeData(color: scheme.onSurface),
      ),

      // Cards
      cardTheme: CardThemeData(
        color: isDark ? CHColors.darkCard : CHColors.lightCard,
        margin: EdgeInsets.zero,
        elevation: 0,
        surfaceTintColor: Colors.transparent,
        shape: RoundedRectangleBorder(
          borderRadius: CHRadius.xlBorder,
          side: BorderSide(
            color: isDark
                ? CHColors.darkBorder.withValues(alpha: 0.6)
                : CHColors.lightBorder,
          ),
        ),
      ),

      // Inputs
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: isDark ? CHColors.darkSurfaceElevated : Colors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
        border: OutlineInputBorder(
          borderRadius: CHRadius.lgBorder,
          borderSide: BorderSide(color: scheme.outline),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: CHRadius.lgBorder,
          borderSide: BorderSide(color: scheme.outline),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: CHRadius.lgBorder,
          borderSide: BorderSide(color: scheme.primary, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: CHRadius.lgBorder,
          borderSide: BorderSide(color: scheme.error, width: 1.5),
        ),
        focusedErrorBorder: OutlineInputBorder(
          borderRadius: CHRadius.lgBorder,
          borderSide: BorderSide(color: scheme.error, width: 2),
        ),
        labelStyle: CHTypography.bodyMedium.copyWith(
          color: scheme.onSurfaceVariant,
        ),
        hintStyle: CHTypography.bodyMedium.copyWith(
          color: scheme.onSurfaceVariant.withValues(alpha: 0.6),
        ),
        floatingLabelStyle: CHTypography.labelLarge.copyWith(
          color: scheme.primary,
        ),
      ),

      // Buttons
      filledButtonTheme: FilledButtonThemeData(
        style: FilledButton.styleFrom(
          backgroundColor: scheme.primary,
          foregroundColor: scheme.onPrimary,
          minimumSize: const Size.fromHeight(50),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
          textStyle: CHTypography.labelLarge.copyWith(
            fontSize: 15,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.2,
          ),
          elevation: 0,
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: scheme.primary,
          minimumSize: const Size.fromHeight(50),
          side: BorderSide(color: scheme.primary, width: 1.5),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
          textStyle: CHTypography.labelLarge.copyWith(
            fontSize: 15,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.2,
          ),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: scheme.primary,
          textStyle: CHTypography.labelLarge.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
      ),

      // Navigation Bar
      navigationBarTheme: NavigationBarThemeData(
        backgroundColor: isDark
            ? CHColors.darkSurface.withValues(alpha: 0.98)
            : Colors.white.withValues(alpha: 0.98),
        indicatorColor: scheme.primary.withValues(alpha: 0.12),
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        height: 68,
        labelTextStyle: WidgetStatePropertyAll(
          CHTypography.labelMedium.copyWith(
            fontWeight: FontWeight.w600,
            color: scheme.onSurface,
          ),
        ),
        iconTheme: WidgetStateProperty.resolveWith((states) {
          if (states.contains(WidgetState.selected)) {
            return IconThemeData(color: scheme.primary, size: 24);
          }
          return IconThemeData(color: scheme.onSurfaceVariant, size: 24);
        }),
      ),

      // Chips
      chipTheme: ChipThemeData(
        shape: RoundedRectangleBorder(
          borderRadius: CHRadius.pillBorder,
        ),
        side: BorderSide(
          color: scheme.outline.withValues(alpha: 0.7),
        ),
        backgroundColor: isDark ? CHColors.darkSurfaceElevated : CHColors.lightSurfaceDim,
        selectedColor: scheme.primaryContainer,
        labelStyle: CHTypography.labelMedium,
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      ),

      // Divider
      dividerTheme: DividerThemeData(
        color: scheme.outline.withValues(alpha: 0.5),
        thickness: 1,
        space: 1,
      ),

      // Bottom Sheet
      bottomSheetTheme: BottomSheetThemeData(
        backgroundColor: isDark ? CHColors.darkSurface : Colors.white,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
        ),
        elevation: 0,
      ),

      // Dialog
      dialogTheme: DialogThemeData(
        backgroundColor: isDark ? CHColors.darkCard : Colors.white,
        shape: RoundedRectangleBorder(
          borderRadius: CHRadius.xxlBorder,
        ),
        elevation: 0,
      ),

      // Snackbar
      snackBarTheme: SnackBarThemeData(
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(
          borderRadius: CHRadius.lgBorder,
        ),
        backgroundColor: isDark ? CHColors.darkSurfaceElevated : CHColors.lightTextPrimary,
        contentTextStyle: CHTypography.bodyMedium.copyWith(
          color: isDark ? CHColors.darkTextPrimary : Colors.white,
        ),
      ),

      // Page transitions
      pageTransitionsTheme: const PageTransitionsTheme(
        builders: {
          TargetPlatform.android: CupertinoPageTransitionsBuilder(),
          TargetPlatform.iOS: CupertinoPageTransitionsBuilder(),
        },
      ),
    );
  }
}
