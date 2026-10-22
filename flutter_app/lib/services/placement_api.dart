import 'dart:convert';
import 'dart:typed_data';

import 'package:http/http.dart' as http;
import 'package:image_picker/image_picker.dart';

import '../models/student_profile.dart';

class ApiException implements Exception {
  const ApiException(this.message, this.statusCode);

  final String message;
  final int statusCode;

  @override
  String toString() => message;
}

class PlacementApi {
  PlacementApi({this.baseUrl = 'http://localhost:8080/api'});

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

  Future<Map<String, dynamic>> register({
    required String fullName,
    required String enrollmentNumber,
    required String universityEmail,
    required String password,
  }) {
    return _post('/auth/register', {
      'fullName': fullName,
      'enrollmentNumber': enrollmentNumber,
      'universityEmail': universityEmail,
      'password': password,
    });
  }

  Future<StudentProfile> fetchMe() async {
    final response = await _request('GET', '/me');
    return StudentProfile.fromJson(response);
  }

  Future<StudentProfile> updateMe(Map<String, dynamic> update) async {
    final response = await _request('PUT', '/me', body: update);
    return StudentProfile.fromJson(response);
  }

  Future<Map<String, dynamic>> uploadProfilePhoto(XFile file) async {
    final request = http.MultipartRequest('POST', Uri.parse('$baseUrl/me/profile-photo'));
    if (_token != null) request.headers['Authorization'] = 'Bearer $_token';
    request.files.add(http.MultipartFile.fromBytes(
      'file',
      await file.readAsBytes(),
      filename: file.name,
    ));
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
    final response = await http.get(Uri.parse('$baseUrl/me/profile-photo'), headers: headers);
    if (response.statusCode == 404) return null;
    if (response.statusCode < 200 || response.statusCode >= 300) {
      throw ApiException('Could not load profile photo', response.statusCode);
    }
    return response.bodyBytes;
  }

  Future<void> removeProfilePhoto() async {
    final headers = <String, String>{};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final response = await http.delete(Uri.parse('$baseUrl/me/profile-photo'), headers: headers);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final decoded = response.body.isEmpty ? <String, dynamic>{} : jsonDecode(response.body);
      final data = decoded is Map ? Map<String, dynamic>.from(decoded) : {};
      throw ApiException(
        (data['detail'] ?? data['message'] ?? 'Photo removal failed').toString(),
        response.statusCode,
      );
    }
  }

  Future<Map<String, dynamic>> _post(String path, Map<String, dynamic> body) {
    return _request('POST', path, body: body);
  }

  Future<Map<String, dynamic>> _request(String method, String path,
      {Map<String, dynamic>? body}) async {
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (_token != null) headers['Authorization'] = 'Bearer $_token';
    final request = http.Request(method, Uri.parse('$baseUrl$path'))
      ..headers.addAll(headers);
    if (body != null) request.body = jsonEncode(body);

    final response = await http.Client().send(request);
    final text = await response.stream.bytesToString();
    final decoded = text.isEmpty ? <String, dynamic>{} : jsonDecode(text);
    if (response.statusCode < 200 || response.statusCode >= 300) {
      final data = decoded is Map ? Map<String, dynamic>.from(decoded) : {};
      throw ApiException(
        (data['detail'] ?? data['message'] ?? 'Request failed').toString(),
        response.statusCode,
      );
    }
    return decoded is Map ? Map<String, dynamic>.from(decoded) : {};
  }
}
