import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../models/student_profile.dart';
import '../services/placement_api.dart';

class ProfilePage extends StatefulWidget {
  const ProfilePage({super.key, required this.api, this.onPhotoChanged});

  final PlacementApi api;
  final ValueChanged<Uint8List?>? onPhotoChanged;

  @override
  State<ProfilePage> createState() => _ProfilePageState();
}

class _ProfilePageState extends State<ProfilePage> {
  StudentProfile? _profile;
  Uint8List? _photoBytes;
  Uint8List? _photoPreview;
  XFile? _selectedPhoto;
  String? _error;
  bool _loading = true;
  bool _photoLoading = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final profile = await widget.api.fetchMe();
      Uint8List? photo;
      try {
        photo = await widget.api.fetchProfilePhoto();
      } catch (_) {
        photo = null;
      }
      if (mounted) {
        setState(() {
          _profile = profile;
          _photoBytes = photo;
        });
      }
    } on ApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } catch (_) {
      if (mounted) setState(() => _error = 'Unable to load your profile.');
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _pickPhoto() async {
    final file = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (file == null || !mounted) {
      return;
    }
    final extension = file.name.toLowerCase().split('.').last;
    if (extension != 'jpg' && extension != 'jpeg' && extension != 'png') {
      _showMessage('Choose a JPEG or PNG image.');
      return;
    }
    final bytes = await file.readAsBytes();
    if (bytes.length > 5 * 1024 * 1024) {
      _showMessage('Profile photos must be 5 MB or smaller.');
      return;
    }
    setState(() {
      _selectedPhoto = file;
      _photoPreview = bytes;
    });
  }

  Future<void> _uploadPhoto() async {
    final file = _selectedPhoto;
    if (file == null) return;
    setState(() => _photoLoading = true);
    try {
      await widget.api.uploadProfilePhoto(file);
      final photo = await widget.api.fetchProfilePhoto();
      if (mounted) {
        setState(() {
          _photoBytes = photo;
          _photoPreview = null;
          _selectedPhoto = null;
        });
        widget.onPhotoChanged?.call(photo);
        _showMessage('Profile photo updated.');
      }
    } on ApiException catch (error) {
      if (mounted) _showMessage(error.message);
    } catch (_) {
      if (mounted) _showMessage('Could not upload the profile photo.');
    } finally {
      if (mounted) setState(() => _photoLoading = false);
    }
  }

  Future<void> _removePhoto() async {
    final shouldRemove = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Remove profile photo?'),
        content: const Text('Your default avatar will be shown instead.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(dialogContext, false), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(dialogContext, true), child: const Text('Remove')),
        ],
      ),
    );
    if (shouldRemove != true) return;
    setState(() => _photoLoading = true);
    try {
      await widget.api.removeProfilePhoto();
      if (mounted) {
        setState(() => _photoBytes = null);
        widget.onPhotoChanged?.call(null);
        _showMessage('Profile photo removed.');
      }
    } on ApiException catch (error) {
      if (mounted) _showMessage(error.message);
    } catch (_) {
      if (mounted) _showMessage('Could not remove the profile photo.');
    } finally {
      if (mounted) setState(() => _photoLoading = false);
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _edit() async {
    final profile = _profile;
    if (profile == null) return;
    final updated = await Navigator.of(context).push<StudentProfile>(
      MaterialPageRoute(
        builder: (_) => EditProfilePage(api: widget.api, profile: profile),
      ),
    );
    if (updated != null && mounted) setState(() => _profile = updated);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('My Profile'),
        actions: [
          if (_profile != null)
            IconButton(onPressed: _edit, icon: const Icon(Icons.edit_outlined)),
        ],
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? _ErrorView(message: _error!, onRetry: _load)
              : _profile == null
                  ? _ErrorView(message: 'Profile not found.', onRetry: _load)
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView(
                        physics: const AlwaysScrollableScrollPhysics(),
                        padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
                        children: [
                          _IdentityCard(
                            profile: _profile!,
                            photoBytes: _photoPreview ?? _photoBytes,
                            hasSavedPhoto: _photoBytes != null,
                            selectedPhoto: _selectedPhoto != null,
                            loading: _photoLoading,
                            onChange: _pickPhoto,
                            onUpload: _uploadPhoto,
                            onRemove: _removePhoto,
                            onCancelPreview: () => setState(() {
                              _selectedPhoto = null;
                              _photoPreview = null;
                            }),
                          ),
                          const SizedBox(height: 20),
                          _Section(
                            title: 'Personal information',
                            children: [
                              _InfoRow(label: 'University email', value: _profile!.universityEmail),
                              _InfoRow(label: 'Account status', value: _profile!.active ? 'Active' : 'Inactive'),
                              _InfoRow(label: 'Role', value: _profile!.role),
                            ],
                          ),
                          const SizedBox(height: 16),
                          _Section(
                            title: 'Academic information',
                            children: [
                              _InfoRow(label: 'Admission year', value: _value(_profile!.profile['admissionYear'])),
                              _InfoRow(label: 'Graduation year', value: _value(_profile!.profile['graduationYear'])),
                              _InfoRow(label: 'Program', value: _value(_profile!.profile['academicProgram'])),
                              _InfoRow(label: 'Current semester', value: _value(_profile!.profile['currentSemester'])),
                              _InfoRow(label: 'CGPA', value: _value(_profile!.profile['cgpa'])),
                            ],
                          ),
                          const SizedBox(height: 16),
                          _Section(
                            title: 'Contact and career',
                            children: [
                              _InfoRow(label: 'Phone number', value: _value(_profile!.profile['phoneNumber'])),
                              _InfoRow(label: 'Date of birth', value: _value(_profile!.profile['dateOfBirth'])),
                              _InfoRow(label: 'Career profile', value: _value(_profile!.profile['careerProfile'])),
                            ],
                          ),
                          const SizedBox(height: 20),
                          OutlinedButton.icon(
                            onPressed: _edit,
                            icon: const Icon(Icons.edit_outlined),
                            label: const Text('Edit profile'),
                          ),
                        ],
                      ),
                    ),
    );
  }

  String _value(dynamic value) => value == null || value.toString().isEmpty ? 'Not provided' : value.toString();
}

class EditProfilePage extends StatefulWidget {
  const EditProfilePage({super.key, required this.api, required this.profile});

  final PlacementApi api;
  final StudentProfile profile;

  @override
  State<EditProfilePage> createState() => _EditProfilePageState();
}

class _EditProfilePageState extends State<EditProfilePage> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name;
  late final TextEditingController _phone;
  late final TextEditingController _program;
  late final TextEditingController _admissionYear;
  late final TextEditingController _graduationYear;
  late final TextEditingController _dob;
  late final TextEditingController _semester;
  late final TextEditingController _cgpa;
  late final TextEditingController _career;
  bool _saving = false;

  @override
  void initState() {
    super.initState();
    final p = widget.profile.profile;
    _name = TextEditingController(text: widget.profile.fullName);
    _phone = TextEditingController(text: _text(p['phoneNumber']));
    _program = TextEditingController(text: _text(p['academicProgram']));
    _admissionYear = TextEditingController(text: _text(p['admissionYear']));
    _graduationYear = TextEditingController(text: _text(p['graduationYear']));
    _dob = TextEditingController(text: _text(p['dateOfBirth']));
    _semester = TextEditingController(text: _text(p['currentSemester']));
    _cgpa = TextEditingController(text: _text(p['cgpa']));
    _career = TextEditingController(text: _text(p['careerProfile']));
  }

  String _text(dynamic value) => value?.toString() ?? '';

  @override
  void dispose() {
    for (final controller in [_name, _phone, _program, _admissionYear, _graduationYear, _dob, _semester, _cgpa, _career]) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _saving = true);
    try {
      final updated = await widget.api.updateMe({
        'fullName': _name.text.trim(),
        'phoneNumber': _nullable(_phone.text),
        'academicProgram': _nullable(_program.text),
        'admissionYear': _number(_admissionYear.text),
        'graduationYear': _number(_graduationYear.text),
        'dateOfBirth': _nullable(_dob.text),
        'currentSemester': _number(_semester.text),
        'cgpa': _decimal(_cgpa.text),
        'careerProfile': _nullable(_career.text),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Profile updated')));
        Navigator.of(context).pop(updated);
      }
    } on ApiException catch (error) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.message)));
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Could not save your profile.')));
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String? _nullable(String value) => value.trim().isEmpty ? null : value.trim();
  int? _number(String value) => value.trim().isEmpty ? null : int.tryParse(value.trim());
  double? _decimal(String value) => value.trim().isEmpty ? null : double.tryParse(value.trim());

  String? _numberValidator(String? value) {
    if (value == null || value.trim().isEmpty) return null;
    return int.tryParse(value.trim()) == null ? 'Enter a whole number' : null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Edit profile')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
          children: [
            TextFormField(controller: _name, decoration: const InputDecoration(labelText: 'Full name'), validator: (value) => value == null || value.trim().isEmpty ? 'Full name is required' : null),
            const SizedBox(height: 14),
            TextFormField(initialValue: widget.profile.enrollmentNumber, readOnly: true, decoration: const InputDecoration(labelText: 'Enrollment number')),
            const SizedBox(height: 14),
            TextFormField(initialValue: widget.profile.universityEmail, readOnly: true, decoration: const InputDecoration(labelText: 'University email')),
            const SizedBox(height: 14),
            TextFormField(initialValue: widget.profile.department?['name']?.toString() ?? 'Not assigned', readOnly: true, decoration: const InputDecoration(labelText: 'Department')),
            const Divider(height: 32),
            Text('Academic information', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            _numberField(_admissionYear, 'Admission year'),
            const SizedBox(height: 14),
            _numberField(_graduationYear, 'Graduation year'),
            const SizedBox(height: 14),
            _numberField(_semester, 'Current semester'),
            const SizedBox(height: 14),
            TextFormField(controller: _program, decoration: const InputDecoration(labelText: 'Academic program')),
            const SizedBox(height: 14),
            TextFormField(controller: _cgpa, keyboardType: const TextInputType.numberWithOptions(decimal: true), decoration: const InputDecoration(labelText: 'CGPA'), validator: (value) {
              if (value == null || value.trim().isEmpty) return null;
              final number = double.tryParse(value.trim());
              return number == null || number < 0 || number > 10 ? 'Enter a CGPA between 0 and 10' : null;
            }),
            const Divider(height: 32),
            Text('Contact and career', style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 12),
            TextFormField(controller: _phone, keyboardType: TextInputType.phone, decoration: const InputDecoration(labelText: 'Phone number')),
            const SizedBox(height: 14),
            TextFormField(controller: _dob, decoration: const InputDecoration(labelText: 'Date of birth', hintText: 'YYYY-MM-DD')),
            const SizedBox(height: 14),
            TextFormField(controller: _career, minLines: 3, maxLines: 5, decoration: const InputDecoration(labelText: 'Career profile', alignLabelWithHint: true)),
            const SizedBox(height: 24),
            FilledButton(onPressed: _saving ? null : _save, child: Text(_saving ? 'Saving…' : 'Save changes')),
            TextButton(onPressed: _saving ? null : () => Navigator.of(context).pop(), child: const Text('Cancel')),
          ],
        ),
      ),
    );
  }

  Widget _numberField(TextEditingController controller, String label) {
    return TextFormField(controller: controller, keyboardType: TextInputType.number, decoration: InputDecoration(labelText: label), validator: _numberValidator);
  }
}

class _IdentityCard extends StatelessWidget {
  const _IdentityCard({
    required this.profile,
    required this.photoBytes,
    required this.hasSavedPhoto,
    required this.selectedPhoto,
    required this.loading,
    required this.onChange,
    required this.onUpload,
    required this.onRemove,
    required this.onCancelPreview,
  });

  final StudentProfile profile;
  final Uint8List? photoBytes;
  final bool hasSavedPhoto;
  final bool selectedPhoto;
  final bool loading;
  final VoidCallback onChange;
  final VoidCallback onUpload;
  final VoidCallback onRemove;
  final VoidCallback onCancelPreview;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final initials = profile.fullName.trim().isEmpty
        ? '?'
        : profile.fullName.trim().split(RegExp(r'\s+')).take(2).map((part) => part[0]).join().toUpperCase();
    return Card(
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            CircleAvatar(
              radius: 44,
              backgroundImage: photoBytes == null ? null : MemoryImage(photoBytes!),
              child: photoBytes == null ? Text(initials, style: theme.textTheme.headlineSmall) : null,
            ),
            const SizedBox(height: 12),
            Text(profile.fullName, style: theme.textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: 4),
            Text(profile.enrollmentNumber, style: TextStyle(color: theme.colorScheme.onSurfaceVariant)),
            Text(profile.department?['name']?.toString() ?? 'Department not assigned', style: TextStyle(color: theme.colorScheme.onSurfaceVariant)),
            const SizedBox(height: 14),
            if (loading)
              const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(strokeWidth: 2))
            else if (selectedPhoto)
              Wrap(
                spacing: 8,
                alignment: WrapAlignment.center,
                children: [
                  FilledButton.icon(onPressed: onUpload, icon: const Icon(Icons.cloud_upload_outlined), label: const Text('Upload photo')),
                  TextButton(onPressed: onCancelPreview, child: const Text('Cancel')),
                ],
              )
            else
              Wrap(
                spacing: 8,
                alignment: WrapAlignment.center,
                children: [
                  OutlinedButton.icon(onPressed: onChange, icon: const Icon(Icons.photo_camera_outlined), label: Text(hasSavedPhoto ? 'Change photo' : 'Upload photo')),
                  if (hasSavedPhoto) TextButton.icon(onPressed: onRemove, icon: const Icon(Icons.delete_outline), label: const Text('Remove')),
                ],
              ),
          ],
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.children});
  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) => Card(
        elevation: 0,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 8),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(title, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            ...children,
          ]),
        ),
      );
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 7),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          SizedBox(width: 125, child: Text(label, style: TextStyle(color: Theme.of(context).colorScheme.onSurfaceVariant))),
          Expanded(child: Text(value, softWrap: true)),
        ]),
      );
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 14),
          OutlinedButton(onPressed: onRetry, child: const Text('Retry')),
        ]),
      ));
}
