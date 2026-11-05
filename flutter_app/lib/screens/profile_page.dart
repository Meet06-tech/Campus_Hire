import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../models/student_profile.dart';
import '../services/placement_api.dart';
import '../theme/app_theme.dart';

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
        content: const Text('Your default initials avatar will be shown instead.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
              foregroundColor: Colors.white,
            ),
            child: const Text('Remove'),
          ),
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
            IconButton(
              onPressed: _edit,
              icon: const Icon(Icons.edit_outlined),
              tooltip: 'Edit profile',
            ),
          const SizedBox(width: 8),
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
                      child: Center(
                        child: ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 680),
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
                              const SizedBox(height: 18),
                              _Section(
                                title: 'Personal Information',
                                icon: Icons.person_outline_rounded,
                                children: [
                                  _InfoRow(
                                    label: 'University email',
                                    value: _profile!.universityEmail,
                                  ),
                                  _InfoRow(
                                    label: 'Account status',
                                    value: _profile!.active ? 'Active' : 'Inactive',
                                  ),
                                  _InfoRow(
                                    label: 'Role',
                                    value: _profile!.role,
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              _Section(
                                title: 'Academic Information',
                                icon: Icons.school_outlined,
                                children: [
                                  _InfoRow(
                                    label: 'Admission year',
                                    value: _value(_profile!.profile['admissionYear']),
                                  ),
                                  _InfoRow(
                                    label: 'Graduation year',
                                    value: _value(_profile!.profile['graduationYear']),
                                  ),
                                  _InfoRow(
                                    label: 'Program',
                                    value: _value(_profile!.profile['academicProgram']),
                                  ),
                                  _InfoRow(
                                    label: 'Current semester',
                                    value: _value(_profile!.profile['currentSemester']),
                                  ),
                                  _InfoRow(
                                    label: 'CGPA',
                                    value: _value(_profile!.profile['cgpa']),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              _Section(
                                title: 'Technical Skills',
                                icon: Icons.code_rounded,
                                children: [
                                  if (_profile!.profile['skills'] is List &&
                                      (_profile!.profile['skills'] as List).isNotEmpty)
                                    Wrap(
                                      spacing: 8,
                                      runSpacing: 8,
                                      children: (_profile!.profile['skills'] as List)
                                          .map(
                                            (skill) => Chip(
                                              label: Text(
                                                skill.toString(),
                                                style: const TextStyle(fontSize: 12),
                                              ),
                                              backgroundColor: Theme.of(context)
                                                  .colorScheme
                                                  .primaryContainer
                                                  .withValues(alpha: 0.5),
                                              side: BorderSide(
                                                color: Theme.of(context)
                                                    .colorScheme
                                                    .primary
                                                    .withValues(alpha: 0.3),
                                              ),
                                            ),
                                          )
                                          .toList(),
                                    )
                                  else
                                    Text(
                                      'No skills added yet.',
                                      style: TextStyle(
                                        color: Theme.of(context)
                                            .colorScheme
                                            .onSurfaceVariant,
                                      ),
                                    ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              _Section(
                                title: 'Contact & Career Details',
                                icon: Icons.work_outline_rounded,
                                children: [
                                  _InfoRow(
                                    label: 'Phone number',
                                    value: _value(_profile!.profile['phoneNumber']),
                                  ),
                                  _InfoRow(
                                    label: 'Date of birth',
                                    value: _value(_profile!.profile['dateOfBirth']),
                                  ),
                                  _InfoRow(
                                    label: 'Career profile',
                                    value: _value(_profile!.profile['careerProfile']),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 24),
                              OutlinedButton.icon(
                                onPressed: _edit,
                                icon: const Icon(Icons.edit_outlined),
                                label: const Text('Edit profile'),
                              ),
                              const SizedBox(height: 32),
                              Center(
                                child: Column(
                                  children: [
                                    ClipRRect(
                                      borderRadius: BorderRadius.circular(10),
                                      child: Image.asset(
                                        'assets/images/cdpc_logo_square.jpg',
                                        height: 44,
                                        width: 44,
                                        fit: BoxFit.cover,
                                        errorBuilder: (_, _, _) => const Icon(
                                          Icons.school_rounded,
                                          size: 32,
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: 8),
                                    Text(
                                      'CHARUSAT · CDPC',
                                      style: TextStyle(
                                        fontWeight: FontWeight.w700,
                                        fontSize: 13,
                                        color: Theme.of(context).colorScheme.onSurface,
                                      ),
                                    ),
                                    const SizedBox(height: 2),
                                    Text(
                                      'Career Development and Placement Cell',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
    );
  }

  String _value(dynamic value) =>
      value == null || value.toString().isEmpty ? 'Not provided' : value.toString();
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
  late final TextEditingController _customSkill;
  final Set<String> _skills = {};
  bool _saving = false;

  static const _predefinedSkills = [
    'Java',
    'Python',
    'SQL',
    'Spring Framework',
    'AI/ML',
    'Django',
    'Flutter',
    'Angular',
    'JavaScript',
    'PostgreSQL',
  ];

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
    _customSkill = TextEditingController();
    final rawSkills = p['skills'];
    if (rawSkills is List) {
      _skills.addAll(
        rawSkills
            .map((value) => value.toString().trim())
            .where((value) => value.isNotEmpty),
      );
    }
  }

  String _text(dynamic value) => value?.toString() ?? '';

  @override
  void dispose() {
    for (final controller in [
      _name,
      _phone,
      _program,
      _admissionYear,
      _graduationYear,
      _dob,
      _semester,
      _cgpa,
      _career,
      _customSkill,
    ]) {
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
        'skills': _skills.toList(),
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Profile updated successfully.')),
        );
        Navigator.of(context).pop(updated);
      }
    } on ApiException catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(error.message)),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not save your profile.')),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  String? _nullable(String value) => value.trim().isEmpty ? null : value.trim();
  int? _number(String value) =>
      value.trim().isEmpty ? null : int.tryParse(value.trim());
  double? _decimal(String value) =>
      value.trim().isEmpty ? null : double.tryParse(value.trim());

  String? _numberValidator(String? value) {
    if (value == null || value.trim().isEmpty) return null;
    return int.tryParse(value.trim()) == null ? 'Enter a whole number' : null;
  }

  String? _semesterValidator(String? value) {
    final basic = _numberValidator(value);
    if (basic != null || value == null || value.trim().isEmpty) return basic;
    final semester = int.parse(value.trim());
    if (semester <= 0) return 'Semester must be positive';
    final duration = widget.profile.profile['programDurationSemesters'];
    if (duration is num && semester > duration) {
      return 'Semester must be within your program duration of ${duration.toInt()} semesters';
    }
    return null;
  }

  void _addCustomSkill() {
    final skill = _customSkill.text.trim();
    if (skill.isEmpty) return;
    if (skill.length > 100) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Skill names must be 100 characters or fewer.'),
        ),
      );
      return;
    }
    setState(() {
      _skills.add(skill);
      _customSkill.clear();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Edit profile')),
      body: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 680),
          child: Form(
            key: _formKey,
            child: ListView(
              padding: const EdgeInsets.fromLTRB(20, 12, 20, 32),
              children: [
                TextFormField(
                  controller: _name,
                  decoration: const InputDecoration(labelText: 'Full name'),
                  validator: (value) => value == null || value.trim().isEmpty
                      ? 'Full name is required'
                      : null,
                ),
                const SizedBox(height: 14),
                TextFormField(
                  initialValue: widget.profile.enrollmentNumber,
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: 'Enrollment number (Read-only)',
                  ),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  initialValue: widget.profile.universityEmail,
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: 'University email (Read-only)',
                  ),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  initialValue:
                      widget.profile.department?['name']?.toString() ??
                          'Not assigned',
                  readOnly: true,
                  decoration: const InputDecoration(
                    labelText: 'Department (Read-only)',
                  ),
                ),
                const Divider(height: 36),
                Text(
                  'Academic Information',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                ),
                const SizedBox(height: 14),
                _numberField(_admissionYear, 'Admission year'),
                const SizedBox(height: 14),
                _numberField(_graduationYear, 'Graduation year'),
                const SizedBox(height: 14),
                _numberField(
                  _semester,
                  'Current semester',
                  validator: _semesterValidator,
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _program,
                  decoration: const InputDecoration(labelText: 'Academic program'),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _cgpa,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(labelText: 'CGPA (0 - 10)'),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) return null;
                    final number = double.tryParse(value.trim());
                    return number == null || number < 0 || number > 10
                        ? 'Enter a CGPA between 0 and 10'
                        : null;
                  },
                ),
                const Divider(height: 36),
                Text(
                  'Skills',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 8,
                  runSpacing: 6,
                  children: _predefinedSkills
                      .map(
                        (skill) => FilterChip(
                          label: Text(skill),
                          selected: _skills.contains(skill),
                          onSelected: (selected) => setState(
                            () => selected
                                ? _skills.add(skill)
                                : _skills.remove(skill),
                          ),
                        ),
                      )
                      .toList(),
                ),
                if (_skills.any((skill) => !_predefinedSkills.contains(skill))) ...[
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8,
                    runSpacing: 6,
                    children: _skills
                        .where((skill) => !_predefinedSkills.contains(skill))
                        .map(
                          (skill) => InputChip(
                            label: Text(skill),
                            onDeleted: () =>
                                setState(() => _skills.remove(skill)),
                          ),
                        )
                        .toList(),
                  ),
                ],
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _customSkill,
                        decoration: const InputDecoration(
                          labelText: 'Add other skill',
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton.filled(
                      onPressed: _addCustomSkill,
                      icon: const Icon(Icons.add_rounded),
                      tooltip: 'Add skill',
                    ),
                  ],
                ),
                const Divider(height: 36),
                Text(
                  'Contact & Career Details',
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _phone,
                  keyboardType: TextInputType.phone,
                  decoration: const InputDecoration(labelText: 'Phone number'),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _dob,
                  decoration: const InputDecoration(
                    labelText: 'Date of birth',
                    hintText: 'YYYY-MM-DD',
                  ),
                ),
                const SizedBox(height: 14),
                TextFormField(
                  controller: _career,
                  minLines: 3,
                  maxLines: 5,
                  decoration: const InputDecoration(
                    labelText: 'Career profile & aspirations',
                    alignLabelWithHint: true,
                  ),
                ),
                const SizedBox(height: 28),
                FilledButton(
                  onPressed: _saving ? null : _save,
                  child: Text(_saving ? 'Saving changes…' : 'Save changes'),
                ),
                const SizedBox(height: 10),
                TextButton(
                  onPressed: _saving ? null : () => Navigator.of(context).pop(),
                  child: const Text('Cancel'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _numberField(
    TextEditingController controller,
    String label, {
    String? Function(String?)? validator,
  }) {
    return TextFormField(
      controller: controller,
      keyboardType: TextInputType.number,
      decoration: InputDecoration(labelText: label),
      validator: validator ?? _numberValidator,
    );
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
    final colors = Theme.of(context).colorScheme;
    final initials = profile.fullName.trim().isEmpty
        ? '?'
        : profile.fullName
            .trim()
            .split(RegExp(r'\s+'))
            .take(2)
            .map((part) => part[0])
            .join()
            .toUpperCase();

    final cgpa = profile.profile['cgpa'];

    return Card(
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
        child: Column(
          children: [
            Stack(
              alignment: Alignment.bottomRight,
              children: [
                CircleAvatar(
                  radius: 46,
                  backgroundColor: colors.primaryContainer,
                  backgroundImage:
                      photoBytes == null ? null : MemoryImage(photoBytes!),
                  child: photoBytes == null
                      ? Text(
                          initials,
                          style: TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.w700,
                            color: colors.primary,
                          ),
                        )
                      : null,
                ),
                if (!loading && !selectedPhoto)
                  GestureDetector(
                    onTap: onChange,
                    child: Container(
                      padding: const EdgeInsets.all(6),
                      decoration: BoxDecoration(
                        color: colors.primary,
                        shape: BoxShape.circle,
                        border: Border.all(color: Colors.white, width: 2),
                      ),
                      child: const Icon(
                        Icons.camera_alt_rounded,
                        size: 16,
                        color: Colors.white,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            Text(
              profile.fullName,
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.2,
                  ),
            ),
            const SizedBox(height: 4),
            Text(
              profile.enrollmentNumber,
              style: TextStyle(
                fontWeight: FontWeight.w600,
                color: colors.primary,
                fontSize: 14,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              profile.department?['name']?.toString() ??
                  'Department not assigned',
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: 13,
                color: colors.onSurfaceVariant,
              ),
            ),
            if (cgpa != null && cgpa.toString().isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                decoration: BoxDecoration(
                  color: colors.secondaryContainer.withValues(alpha: 0.5),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: colors.secondary.withValues(alpha: 0.4),
                  ),
                ),
                child: Text(
                  'CGPA: $cgpa',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    color: colors.secondary,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 16),
            if (loading)
              const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            else if (selectedPhoto)
              Wrap(
                spacing: 8,
                alignment: WrapAlignment.center,
                children: [
                  FilledButton.icon(
                    onPressed: onUpload,
                    icon: const Icon(Icons.cloud_upload_outlined, size: 18),
                    label: const Text('Save photo'),
                  ),
                  TextButton(
                    onPressed: onCancelPreview,
                    child: const Text('Cancel'),
                  ),
                ],
              )
            else
              Wrap(
                spacing: 8,
                alignment: WrapAlignment.center,
                children: [
                  OutlinedButton.icon(
                    onPressed: onChange,
                    icon: const Icon(Icons.photo_camera_outlined, size: 18),
                    label: Text(hasSavedPhoto ? 'Change photo' : 'Upload photo'),
                  ),
                  if (hasSavedPhoto)
                    TextButton.icon(
                      onPressed: onRemove,
                      icon: const Icon(Icons.delete_outline, size: 18),
                      style: TextButton.styleFrom(
                        foregroundColor: colors.error,
                      ),
                      label: const Text('Remove'),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}

class _Section extends StatelessWidget {
  const _Section({
    required this.title,
    required this.icon,
    required this.children,
  });

  final String title;
  final IconData icon;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Card(
      elevation: 0,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(18, 16, 18, 14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icon, size: 18, color: colors.primary),
                const SizedBox(width: 8),
                Text(
                  title,
                  style: Theme.of(context).textTheme.titleSmall?.copyWith(
                        fontWeight: FontWeight.w700,
                        color: colors.primary,
                      ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ...children,
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  const _InfoRow({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: TextStyle(
                fontSize: 13,
                color: colors.onSurfaceVariant,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              softWrap: true,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ErrorView extends StatelessWidget {
  const _ErrorView({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) => Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline_rounded, size: 48, color: Colors.redAccent),
              const SizedBox(height: 12),
              Text(message, textAlign: TextAlign.center),
              const SizedBox(height: 16),
              OutlinedButton(onPressed: onRetry, child: const Text('Retry')),
            ],
          ),
        ),
      );
}
