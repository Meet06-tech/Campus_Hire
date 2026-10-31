import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// A card with a gradient background, used for hero sections and CTAs.
class CHGradientCard extends StatelessWidget {
  const CHGradientCard({
    super.key,
    required this.child,
    this.gradient,
    this.padding,
    this.borderRadius,
    this.onTap,
  });

  final Widget child;
  final Gradient? gradient;
  final EdgeInsetsGeometry? padding;
  final BorderRadius? borderRadius;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final radius = borderRadius ?? CHRadius.xxlBorder;
    return Container(
      decoration: BoxDecoration(
        gradient: gradient ?? CHColors.primaryGradient,
        borderRadius: radius,
        boxShadow: CHShadows.glow(CHColors.electricIndigo),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: radius,
          splashColor: Colors.white.withValues(alpha: 0.1),
          highlightColor: Colors.white.withValues(alpha: 0.05),
          child: Padding(
            padding: padding ?? CHSpacing.cardPadding,
            child: child,
          ),
        ),
      ),
    );
  }
}
