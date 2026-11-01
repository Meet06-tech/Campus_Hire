import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// A statistics card showing a number, label, and optional icon.
class CHStatCard extends StatelessWidget {
  const CHStatCard({
    super.key,
    required this.value,
    required this.label,
    this.icon,
    this.iconColor,
    this.iconBgColor,
    this.trend,
    this.onTap,
  });

  final String value;
  final String label;
  final IconData? icon;
  final Color? iconColor;
  final Color? iconBgColor;
  final String? trend;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: isDark ? CHColors.darkCard : CHColors.lightCard,
        borderRadius: CHRadius.xlBorder,
        border: Border.all(
          color: isDark
              ? CHColors.darkBorder.withValues(alpha: 0.5)
              : CHColors.lightBorder.withValues(alpha: 0.7),
        ),
      ),
      child: InkWell(
        onTap: onTap,
        borderRadius: CHRadius.xlBorder,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (icon != null)
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: iconBgColor ?? colors.primaryContainer,
                  borderRadius: CHRadius.mdBorder,
                ),
                child: Icon(
                  icon,
                  size: 20,
                  color: iconColor ?? colors.primary,
                ),
              ),
            if (icon != null) const SizedBox(height: 12),
            Text(
              value,
              style: CHTypography.statMedium.copyWith(
                color: colors.onSurface,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              label,
              style: CHTypography.bodySmall.copyWith(
                color: colors.onSurfaceVariant,
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
            if (trend != null) ...[
              const SizedBox(height: 6),
              Text(
                trend!,
                style: CHTypography.labelSmall.copyWith(
                  color: CHColors.emerald,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
