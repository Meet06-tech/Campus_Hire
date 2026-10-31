import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

enum BadgeVariant { primary, success, warning, error, neutral, info }

/// A status badge / pill with semantic coloring.
class CHStatusBadge extends StatelessWidget {
  const CHStatusBadge({
    super.key,
    required this.label,
    this.variant = BadgeVariant.primary,
    this.icon,
    this.compact = false,
  });

  final String label;
  final BadgeVariant variant;
  final IconData? icon;
  final bool compact;

  Color _bgColor(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return switch (variant) {
      BadgeVariant.primary =>
        isDark ? const Color(0xFF312E81) : const Color(0xFFEEF2FF),
      BadgeVariant.success =>
        isDark ? const Color(0xFF064E3B) : const Color(0xFFD1FAE5),
      BadgeVariant.warning =>
        isDark ? const Color(0xFF78350F) : const Color(0xFFFEF3C7),
      BadgeVariant.error =>
        isDark ? const Color(0xFF7F1D1D) : const Color(0xFFFEE2E2),
      BadgeVariant.neutral =>
        isDark ? const Color(0xFF374151) : const Color(0xFFF3F4F6),
      BadgeVariant.info =>
        isDark ? const Color(0xFF1E3A5F) : const Color(0xFFDBEAFE),
    };
  }

  Color _fgColor(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return switch (variant) {
      BadgeVariant.primary =>
        isDark ? const Color(0xFFC7D2FE) : CHColors.electricIndigo,
      BadgeVariant.success =>
        isDark ? const Color(0xFF6EE7B7) : const Color(0xFF059669),
      BadgeVariant.warning =>
        isDark ? const Color(0xFFFCD34D) : const Color(0xFFD97706),
      BadgeVariant.error =>
        isDark ? const Color(0xFFFCA5A5) : const Color(0xFFDC2626),
      BadgeVariant.neutral =>
        isDark ? const Color(0xFFD1D5DB) : const Color(0xFF4B5563),
      BadgeVariant.info =>
        isDark ? const Color(0xFF93C5FD) : const Color(0xFF2563EB),
    };
  }

  @override
  Widget build(BuildContext context) {
    final bg = _bgColor(context);
    final fg = _fgColor(context);
    return Container(
      padding: EdgeInsets.symmetric(
        horizontal: compact ? 8 : 10,
        vertical: compact ? 4 : 6,
      ),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: CHRadius.pillBorder,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: compact ? 12 : 14, color: fg),
            SizedBox(width: compact ? 4 : 5),
          ],
          Text(
            label,
            style: (compact ? CHTypography.labelSmall : CHTypography.labelMedium)
                .copyWith(color: fg),
          ),
        ],
      ),
    );
  }
}
