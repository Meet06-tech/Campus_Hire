import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:placement_management_app/main.dart';
import 'package:placement_management_app/screens/student_home.dart';
import 'package:placement_management_app/services/placement_api.dart';

void main() {
  testWidgets('shows the authentication screen', (WidgetTester tester) async {
    await tester.pumpWidget(const PlacementManagementApp());

    expect(find.text('Placement Management System'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
    expect(find.text('New student? Create an account'), findsOneWidget);
  });

  testWidgets('student shell switches between the three local destinations', (WidgetTester tester) async {
    await tester.pumpWidget(MaterialApp(
      home: StudentHomeShell(api: PlacementApi(), onLogout: () {}),
    ));

    expect(find.text('Welcome back'), findsOneWidget);
    await tester.tap(find.text('Applied'));
    await tester.pumpAndSettle();
    expect(find.text('Your applications will appear here.'), findsOneWidget);

    await tester.tap(find.text('History'));
    await tester.pumpAndSettle();
    expect(find.text('Your placement history will appear here.'), findsOneWidget);
  });
}

