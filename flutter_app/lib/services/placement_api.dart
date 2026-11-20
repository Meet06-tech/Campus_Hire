import 'dart:convert';
import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';

import '../models/student_profile.dart';
import '../models/placement_models.dart';
import '../models/placement_attachment.dart';
import '../models/announcement.dart';
import '../models/student_directory.dart';
import '../models/registration_verification.dart';

class ApiException implements Exception {
  const ApiException(this.message, this.statusCode);

  final String message;
  final int statusCode;

  @override
  String toString() => message;
}

class PlacementApi {
  PlacementApi({String? baseUrl}) : baseUrl = baseUrl ?? _defaultBaseUrl;

  static String get _defaultBaseUrl {
    const configured = String.fromEnvironment('API_BASE_URL');
    return resolveBaseUrl(
      configured: configured,
      isWeb: kIsWeb,
      platform: defaultTargetPlatform,
    );
  }

  @visibleForTesting
  static String resolveBaseUrl({
    required String configured,
    required bool isWeb,
    required TargetPlatform platform,
  }) {
    if (configured.isNotEmpty) return configured;
    if (isWeb || platform != TargetPlatform.android) {
      return 'http://localhost:8080/api';
    }
    // The project uses a physical Android device with adb reverse during local
    // development. Android's loopback is therefore forwarded to the Mac.
    // Emulators can override this with API_BASE_URL=http://10.0.2.2:8080/api.
    return 'http://127.0.0.1:8080/api';
  }

  final String baseUrl;
  String? _token;

  void setToken(String token) => _token = token;

  void clearToken() => _token = null;

  Future<Map<String, dynamic>> login(String enrollmentNumber, String password) {
    return _post('/auth/login', {
      'enrollmentNumber': enrollmentNumber,
      'password': password,
    });
  }

  Future<Map<String, dynamic>> requestForgotPassword(String email) {
    return _post('/auth/forgot-password', {'email': email});
  }

  Future<Map<String, dynamic>> verifyForgotPassword({
    required String email,
    required String otp,
  }) {
    return _post('/auth/forgot-password/verify-otp', {
      'email': email,
      'otp': otp,
    });
  }

  Future<void> resetForgotPassword({
    required String recoveryToken,
    required String newPassword,
    required String confirmPassword,
  }) async {
    await _requestJson(
      'POST',
      '/auth/forgot-password/reset',
      body: {
        'recoveryToken': recoveryToken,
        'newPassword': newPassword,
        'confirmPassword': confirmPassword,
      },
    );
  }

  Future<Map<String, dynamic>> register({
    required String fullName,
    required String enrollmentNumber,
    required String universityEmail,
    required String password,
    required String confirmPassword,
  }) {
    return _post('/auth/register', {
      'fullName': fullName,
      'enrollmentNumber': enrollmentNumber,
      'universityEmail': universityEmail,
      'password': password,
      'confirmPassword': confirmPassword,
    });
  }

  Future<RegistrationVerification> beginRegistration({
    required String fullName,
    required String enrollmentNumber,
    required String universityEmail,
    required String password,
    required String confirmPassword,
  }) async {
    final response = await _post('/auth/register', {
      'fullName': fullName,
      'enrollmentNumber': enrollmentNumber,
      'universityEmail': universityEmail,
      'password': password,
      'confirmPassword': confirmPassword,
    });
    return RegistrationVerification.fromJson(response);
  }

  Future<void> verifyRegistrationOtp({
    required int registrationId,
    required String otp,
  }) async {
    await _requestJson(
      'POST',
      '/auth/register/verify',
      body: {'registrationId': registrationId, 'otp': otp},
    );
  }

  Future<RegistrationVerification> resendRegistrationOtp(
    int registrationId,
  ) async {
    final response = await _post('/auth/register/resend', {
      'registrationId': registrationId,
    });
    return RegistrationVerification.fromJson(response);
  }

  Future<StudentProfile> fetchMe() async {
    final response = await _request('GET', '/me');
    return StudentProfile.fromJson(response);
  }

  Future<List<Announcement>> fetchAnnouncements() async {
    final response = await _requestJson('GET', '/announcements');
    if (response is! List) {
      throw const ApiException('Invalid announcements response', 200);
    }
    return response
        .map(
          (item) =>
              Announcement.fromJson(Map<String, dynamic>.from(item as Map)),
        )
        .toList(growable: false);
  }

  Future<List<StudentDirectoryEntry>> fetchStudentDirectory({
    String search = '',
  }) async {
    final path = search.trim().isEmpty
        ? '/student/directory'
        : '/student/directory?search=${Uri.encodeQueryComponent(search.trim())}';
    final response = await _requestJson('GET', path);
    if (response is! List) {
      throw const ApiException('Invalid student directory response', 200);
    }
    return response
        .map(
          (item) => StudentDirectoryEntry.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList(growable: false);
  }

  Future<Uint8List?> fetchDirectoryPhoto(StudentDirectoryEntry student) async {
    if (student.profilePhotoUrl == null) return null;
    final headers = <String, String>{
      'Cache-Control': 'no-cache',
      'Pragma': 'no-cache',
    };
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final response = await http.get(
      Uri.parse('$baseUrl${student.profilePhotoUrl!}'),
      headers: headers,
    );
    if (response.statusCode == 404) return null;
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('Could not load profile photo', response.statusCode);
    }
    return response.bodyBytes;
  }

  Future<void> submitComplaint({
    required String subject,
    required String message,
  }) async {
    await _requestJson(
      'POST',
      '/student/support/complaints',
      body: {'subject': subject, 'message': message},
    );
  }

  Future<void> requestPasswordOtp() async =>
      _requestJson('POST', '/student/password/otp');

  Future<void> verifyPasswordOtp(String otp) async =>
      _requestJson('POST', '/student/password/verify-otp', body: {'otp': otp});

  Future<void> changePassword(
    String newPassword,
    String confirmPassword,
  ) async => _requestJson(
    'PUT',
    '/student/password',
    body: {'newPassword': newPassword, 'confirmPassword': confirmPassword},
  );

  Future<StudentProfile> updateMe(Map<String, dynamic> update) async {
    final response = await _request('PUT', '/me', body: update);
    return StudentProfile.fromJson(response);
  }

  Future<Map<String, dynamic>> uploadProfilePhoto(XFile file) async {
    final request = http.MultipartRequest(
      'POST',
      Uri.parse('$baseUrl/me/profile-photo'),
    );
    if (_token != null) request.headers['Authorization'] = 'Bearer $_token';
    request.files.add(
      http.MultipartFile.fromBytes(
        'file',
        await file.readAsBytes(),
        filename: file.name,
      ),
    );
    final response = await request.send();
    final text = await response.stream.bytesToString();
    final decoded = text.isEmpty ? <String, dynamic>{} : jsonDecode(text);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final data = decoded is Map ? Map<String, dynamic>.from(decoded) : {};
      throw ApiException(
        (data['detail'] ?? data['message'] ?? 'Photo upload failed').toString(),
        response.statusCode,
      );
    }
    return decoded is Map ? Map<String, dynamic>.from(decoded) : {};
  }

  Future<Uint8List?> fetchProfilePhoto() async {
    final headers = <String, String>{};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final response = await http.get(
      Uri.parse('$baseUrl/me/profile-photo'),
      headers: headers,
    );
    if (response.statusCode == 404) return null;
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('Could not load profile photo', response.statusCode);
    }
    return response.bodyBytes;
  }

  Future<void> removeProfilePhoto() async {
    final headers = <String, String>{};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final response = await http.delete(
      Uri.parse('$baseUrl/me/profile-photo'),
      headers: headers,
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final decoded = response.body.isEmpty
          ? <String, dynamic>{}
          : jsonDecode(response.body);
      final data = decoded is Map ? Map<String, dynamic>.from(decoded) : {};
      throw ApiException(
        (data['detail'] ?? data['message'] ?? 'Photo removal failed')
            .toString(),
        response.statusCode,
      );
    }
  }

  Future<List<PlacementDrive>> fetchStudentDrives() async {
    final response = await _requestJson('GET', '/placement/student/drives');
    if (response is! List) {
      throw const ApiException('Invalid drives response', 200);
    }
    return response
        .map(
          (item) =>
              PlacementDrive.fromJson(Map<String, dynamic>.from(item as Map)),
        )
        .toList(growable: false);
  }

  Future<List<PlacementAttachment>> fetchDriveAttachments(int driveId) async {
    final response = await _requestJson(
      'GET',
      '/placement/drives/$driveId/attachments',
    );
    if (response is! List) {
      throw const ApiException('Invalid attachments response', 200);
    }
    return response
        .map(
          (item) => PlacementAttachment.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList(growable: false);
  }

  Future<List<PlacementAttachment>> fetchCompanyAttachments(
    int companyId,
  ) async {
    final response = await _requestJson(
      'GET',
      '/placement/companies/$companyId/attachments',
    );
    if (response is! List) {
      throw const ApiException('Invalid attachments response', 200);
    }
    return response
        .map(
          (item) => PlacementAttachment.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList(growable: false);
  }

  Future<Uint8List> downloadDriveAttachment(
    int driveId,
    PlacementAttachment file,
  ) => _downloadAttachment(
    '/placement/drives/$driveId/attachments/${file.id}',
    file,
  );

  Future<Uint8List> downloadCompanyAttachment(
    int companyId,
    PlacementAttachment file,
  ) => _downloadAttachment(
    '/placement/companies/$companyId/attachments/${file.id}',
    file,
  );

  Future<Uint8List> _downloadAttachment(
    String path,
    PlacementAttachment file,
  ) async {
    final headers = <String, String>{};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final response = await http.get(
      Uri.parse('$baseUrl$path'),
      headers: headers,
    );
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException(
        'Attachment could not be downloaded',
        response.statusCode,
      );
    }
    if (response.bodyBytes.isEmpty ||
        (file.sizeBytes > 0 && response.bodyBytes.length != file.sizeBytes) ||
        response.headers['content-type']?.startsWith('text/html') == true) {
      throw const ApiException('The downloaded attachment is invalid', 200);
    }
    return response.bodyBytes;
  }

  Future<PlacementApplication> applyToDrive(int driveId) async {
    final response = await _postJson(
      'POST',
      '/placement/student/drives/$driveId/applications',
    );
    if (response is! Map) {
      throw const ApiException('Invalid application response', 200);
    }
    return PlacementApplication.fromJson(Map<String, dynamic>.from(response));
  }

  Future<List<PlacementApplication>> fetchMyApplications() async {
    final response = await _requestJson(
      'GET',
      '/placement/student/applications',
    );
    if (response is! List) {
      throw const ApiException('Invalid applications response', 200);
    }
    return response
        .map(
          (item) => PlacementApplication.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList(growable: false);
  }

  Future<List<StudentApplicationProgress>> fetchMyApplicationProgress() async {
    final response = await _requestJson(
      'GET',
      '/placement/student/applications/progress',
    );
    if (response is! List) {
      throw const ApiException('Invalid application progress response', 200);
    }
    return response
        .map(
          (item) => StudentApplicationProgress.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList(growable: false);
  }

  Future<List<StudentApplicationProgress>> fetchMyApplicationHistory() async {
    final response = await _requestJson(
      'GET',
      '/placement/student/applications/history',
    );
    if (response is! List) {
      throw const ApiException('Invalid application history response', 200);
    }
    return response
        .map(
          (item) => StudentApplicationProgress.fromJson(
            Map<String, dynamic>.from(item as Map),
          ),
        )
        .toList(growable: false);
  }

  Future<PlacementApplication> decideOffer(
    int applicationId,
    String decision,
  ) async {
    final response = await _requestJson(
      'PUT',
      '/placement/student/applications/$applicationId/offer',
      body: {'decision': decision},
    );
    if (response is! Map) {
      throw const ApiException('Invalid offer decision response', 200);
    }
    return PlacementApplication.fromJson(Map<String, dynamic>.from(response));
  }

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) {
    return _request('POST', path, body: body);
  }

  Future<dynamic> _postJson(String method, String path) {
    return _requestJson(method, path);
  }

  Future<Map<String, dynamic>> _request(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    final decoded = await _requestJson(method, path, body: body);
    if (decoded is! Map) throw const ApiException('Invalid response', 200);
    return Map<String, dynamic>.from(decoded);
  }

  Future<dynamic> _requestJson(
    String method,
    String path, {
    Map<String, dynamic>? body,
  }) async {
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final uri = Uri.parse('$baseUrl$path');
    final request = http.Request(method, uri)..headers.addAll(headers);
    if (body != null) request.body = jsonEncode(body);

    final client = http.Client();
    final timer = Stopwatch()..start();
    late http.Response response;
    try {
      if (kDebugMode) {
        debugPrint('[CampusHire HTTP] $method $uri started');
      }
      final streamed = await client
          .send(request)
          .timeout(const Duration(seconds: 15));
      response = await http.Response.fromStream(streamed);
      if (kDebugMode) {
        debugPrint(
          '[CampusHire HTTP] $method $uri -> ${response.statusCode} '
          'in ${timer.elapsedMilliseconds} ms',
        );
      }
    } on TimeoutException catch (error) {
      if (kDebugMode) {
        debugPrint(
          '[CampusHire HTTP] $method $uri failed after '
          '${timer.elapsedMilliseconds} ms: ${error.runtimeType}: $error',
        );
      }
      throw const ApiException('The server took too long to respond.', 0);
    } on http.ClientException catch (error) {
      if (kDebugMode) {
        debugPrint(
          '[CampusHire HTTP] $method $uri failed after '
          '${timer.elapsedMilliseconds} ms: ${error.runtimeType}: $error',
        );
      }
      throw const ApiException(
        'Cannot reach the server. Check that the backend is running and the app is using the correct host.',
        0,
      );
    } catch (error) {
      if (kDebugMode) {
        debugPrint(
          '[CampusHire HTTP] $method $uri failed after '
          '${timer.elapsedMilliseconds} ms: ${error.runtimeType}: $error',
        );
      }
      rethrow;
    } finally {
      timer.stop();
      client.close();
    }
    final text = response.body;
    dynamic decoded;
    try {
      decoded = text.isEmpty ? <String, dynamic>{} : jsonDecode(text);
    } on FormatException {
      decoded = <String, dynamic>{};
    }
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final data = decoded is Map ? Map<String, dynamic>.from(decoded) : {};
      throw ApiException(
        (data['detail'] ??
                data['message'] ??
                data['error'] ??
                _fallbackMessage(response.statusCode))
            .toString(),
        response.statusCode,
      );
    }
    return decoded;
  }

  String _fallbackMessage(int statusCode) {
    switch (statusCode) {
      case 401:
        return 'Your session has expired. Please sign in again.';
      case 403:
        return 'You are not allowed to perform this action.';
      case 404:
        return 'The requested record was not found.';
      case 409:
        return 'This action conflicts with an existing record.';
      case 400:
        return 'Please check the information and try again.';
      default:
        return statusCode >= 500
            ? 'The server is temporarily unavailable.'
            : 'Request could not be completed.';
    }
  }
}
