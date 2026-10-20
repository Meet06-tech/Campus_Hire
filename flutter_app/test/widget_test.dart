import 'package:flutter_test/flutter_test.dart';

import 'package:placement_management_app/main.dart';

void main() {
  testWidgets('shows the authentication screen', (WidgetTester tester) async {
    await tester.pumpWidget(const PlacementManagementApp());

    expect(find.text('Placement Management System'), findsOneWidget);
    expect(find.text('Sign in'), findsOneWidget);
    expect(find.text('New student? Create an account'), findsOneWidget);
  });
}

