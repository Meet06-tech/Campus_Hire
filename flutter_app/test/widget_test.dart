import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:placement_management_app/main.dart';
import 'package:placement_management_app/models/announcement.dart';
import 'package:placement_management_app/models/student_directory.dart';
import 'package:placement_management_app/models/placement_attachment.dart';
import 'package:placement_management_app/models/placement_models.dart';
import 'package:placement_management_app/screens/student_home.dart';
import 'package:placement_management_app/services/placement_api.dart';

void main() {
  test('physical Android defaults to the adb reverse loopback URL', () {
    expect(
      PlacementApi.resolveBaseUrl(
        configured: '',
        isWeb: false,
        platform: TargetPlatform.android,
      ),
      'http://127.0.0.1:8080/api',
    );
  });

  testWidgets('shows the welcome and authentication screens', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const PlacementManagementApp());

    expect(find.text('“चारुसैट है तो सक्सेस है”'), findsOneWidget);
    expect(
      find.text('Discover placement opportunities at CHARUSAT.'),
      findsOneWidget,
    );
    expect(find.text('Sign in'), findsOneWidget);
    expect(find.text('Create an account'), findsOneWidget);

    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle();

    expect(find.text('Career Development and Placement Cell'), findsOneWidget);
    expect(find.text('Placement Management System'), findsOneWidget);
    expect(find.text('Welcome back'), findsOneWidget);
    expect(find.text('Forgot Password?'), findsOneWidget);
    await tester.ensureVisible(find.text('Forgot Password?'));
    await tester.tap(find.text('Forgot Password?'));
    await tester.pumpAndSettle();
    expect(find.text('Password recovery'), findsOneWidget);
    expect(find.text('Registered email'), findsOneWidget);
  });

  testWidgets('student shell switches between the four local destinations', (
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

    await tester.tap(find.text('Announcements'));
    await tester.pumpAndSettle();
    expect(find.text('No announcements yet'), findsOneWidget);

    await tester.tap(find.text('More'));
    await tester.pumpAndSettle();
    expect(find.text('Change Password'), findsOneWidget);
  });

  testWidgets(
    'new student session loads its own photo after the previous shell is removed',
    (WidgetTester tester) async {
      final firstPhoto = (await rootBundle.load(
        'assets/images/charusat_logo.png',
      )).buffer.asUint8List();
      final secondPhoto = (await rootBundle.load(
        'assets/images/cdpc_logo_square.png',
      )).buffer.asUint8List();

      Future<void> showPhoto(Uint8List bytes) async {
        await tester.pumpWidget(
          MaterialApp(
            home: StudentHomeShell(
              api: _FakePlacementApi(photo: bytes),
              onLogout: () {},
            ),
          ),
        );
        await tester.pumpAndSettle();
        final avatar = tester.widget<CircleAvatar>(
          find
              .byWidgetPredicate(
                (widget) =>
                    widget is CircleAvatar &&
                    widget.backgroundImage is MemoryImage,
              )
              .first,
        );
        expect(
          (avatar.backgroundImage! as MemoryImage).bytes,
          orderedEquals(bytes),
        );
      }

      await showPhoto(firstPhoto);
      await tester.pumpWidget(const SizedBox.shrink());
      await tester.pump();
      await showPhoto(secondPhoto);
    },
  );

  testWidgets('theme toggle switches repeatedly between dark and light mode', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const PlacementManagementApp());
    await tester.tap(find.text('Sign in'));
    await tester.pumpAndSettle();

    // Initial state: Light mode
    expect(find.byTooltip('Switch to dark mode'), findsOneWidget);

    // Tap 1: Toggle to Dark mode
    await tester.tap(find.byTooltip('Switch to dark mode'));
    await tester.pumpAndSettle();
    expect(find.byTooltip('Switch to light mode'), findsOneWidget);

    // Tap 2: Toggle back to Light mode
    await tester.tap(find.byTooltip('Switch to light mode'));
    await tester.pumpAndSettle();
    expect(find.byTooltip('Switch to dark mode'), findsOneWidget);

    // Tap 3: Toggle to Dark mode again
    await tester.tap(find.byTooltip('Switch to dark mode'));
    await tester.pumpAndSettle();
    expect(find.byTooltip('Switch to light mode'), findsOneWidget);
  });

  testWidgets(
    'drive details offers real download actions for company and drive files',
    (WidgetTester tester) async {
      final api = _FakeAttachmentApi();
      final savedNames = <String>[];
      final drive = PlacementDrive.fromJson({
        'id': 9,
        'company': {'id': 4, 'name': 'Example Company'},
        'title': 'Graduate role',
        'status': 'PUBLISHED',
      });
      await tester.pumpWidget(
        MaterialApp(
          home: DriveDetailsPage(
            api: api,
            drive: drive,
            onSessionExpired: () {},
            onApplicationChanged: () {},
            attachmentSaver: (bytes, name) async {
              expect(bytes, orderedEquals([1, 2, 3]));
              savedNames.add(name);
              return 'test-downloads/$name';
            },
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.scrollUntilVisible(
        find.text('company.pdf'),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('company.pdf'));
      await tester.pumpAndSettle();
      expect(api.lastDownload, 'company-4-1');
      await tester.scrollUntilVisible(
        find.text('drive.pdf'),
        250,
        scrollable: find.byType(Scrollable).first,
      );
      await tester.tap(find.text('drive.pdf'));
      await tester.pumpAndSettle();
      expect(api.lastDownload, 'drive-9-2');
      expect(savedNames, ['company-1-company.pdf', 'drive-2-drive.pdf']);
    },
  );
}

class _FakeAttachmentApi extends _FakePlacementApi {
  String? lastDownload;
  final companyFile = const PlacementAttachment(
    id: 1,
    fileName: 'company.pdf',
    contentType: 'application/pdf',
    sizeBytes: 3,
  );
  final driveFile = const PlacementAttachment(
    id: 2,
    fileName: 'drive.pdf',
    contentType: 'application/pdf',
    sizeBytes: 3,
  );

  @override
  Future<List<PlacementAttachment>> fetchCompanyAttachments(
    int companyId,
  ) async => [companyFile];

  @override
  Future<List<PlacementAttachment>> fetchDriveAttachments(int driveId) async =>
      [driveFile];

  @override
  Future<Uint8List> downloadCompanyAttachment(
    int companyId,
    PlacementAttachment file,
  ) async {
    lastDownload = 'company-$companyId-${file.id}';
    return Uint8List.fromList([1, 2, 3]);
  }

  @override
  Future<Uint8List> downloadDriveAttachment(
    int driveId,
    PlacementAttachment file,
  ) async {
    lastDownload = 'drive-$driveId-${file.id}';
    return Uint8List.fromList([1, 2, 3]);
  }
}

class _FakePlacementApi extends PlacementApi {
  _FakePlacementApi({this.photo}) : super(baseUrl: 'http://test');

  final Uint8List? photo;

  @override
  Future<List<PlacementDrive>> fetchStudentDrives() async => const [];

  @override
  Future<List<PlacementApplication>> fetchMyApplications() async => const [];

  @override
  Future<List<StudentApplicationProgress>> fetchMyApplicationProgress() async =>
      const [];

  @override
  Future<List<StudentApplicationProgress>> fetchMyApplicationHistory() async =>
      const [];

  @override
  Future<Uint8List?> fetchProfilePhoto() async => photo;

  @override
  Future<List<Announcement>> fetchAnnouncements() async => const [];

  @override
  Future<List<StudentDirectoryEntry>> fetchStudentDirectory({
    String search = '',
  }) async => const [];
}
