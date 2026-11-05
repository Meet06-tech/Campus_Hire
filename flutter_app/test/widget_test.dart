import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:placement_management_app/main.dart';
import 'package:placement_management_app/models/placement_models.dart';
import 'package:placement_management_app/screens/student_home.dart';
import 'package:placement_management_app/services/placement_api.dart';

void main() {
  testWidgets('shows the welcome and authentication screens', (WidgetTester tester) async {
    await tester.pumpWidget(const PlacementManagementApp());

    expect(find.text('“चारुसैट है तो सक्सेस है”'), findsOneWidget);
    expect(find.text('Discover placement opportunities at CHARUSAT.'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
    expect(find.text('Create an account'), findsOneWidget);

    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle();

    expect(find.text('Placement Management System'), findsOneWidget);
    expect(find.text('Welcome back'), findsOneWidget);
  });

  testWidgets('student shell switches between the three local destinations', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: StudentHomeShell(api: _FakePlacementApi(), onLogout: () {}),
      ),
    );

    await tester.pumpAndSettle();
    expect(find.text('No placement drives yet'), findsOneWidget);
    await tester.tap(find.text('Applied'));
    await tester.pumpAndSettle();
    expect(find.text('Applied applications'), findsOneWidget);

    await tester.tap(find.text('History'));
    await tester.pumpAndSettle();
    expect(find.text('Application history'), findsOneWidget);
  });
}

class _FakePlacementApi extends PlacementApi {
  _FakePlacementApi() : super(baseUrl: 'http://test');

  @override
  Future<List<PlacementDrive>> fetchStudentDrives() async => const [];

  @override
  Future<List<PlacementApplication>> fetchMyApplications() async => const [];

  @override
  Future<List<StudentApplicationProgress>> fetchMyApplicationProgress() async => const [];

  @override
  Future<List<StudentApplicationProgress>> fetchMyApplicationHistory() async => const [];

  @override
  Future<Uint8List?> fetchProfilePhoto() async => null;
}
