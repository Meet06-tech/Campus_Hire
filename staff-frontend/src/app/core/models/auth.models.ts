/** Matches the backend AuthDtos.AuthResponse record. */
export interface AuthResponse {
  token: string;
  userId: string;
  fullName: string;
  enrollmentNumber: string | null;
  role: StaffRole;
  department: DepartmentInfo | null;
}

export interface DepartmentInfo {
  id: number;
  code: string;
  name: string;
}

/** Matches the backend MeDtos.MeResponse record. */
export interface MeResponse {
  userId: number;
  fullName: string;
  universityEmail: string;
  enrollmentNumber: string | null;
  role: StaffRole;
  active: boolean;
  department: DepartmentResponse | null;
  profile: ProfileResponse | null;
  profilePhotoUrl: string | null;
}

export interface DepartmentResponse {
  id: number;
  code: string;
  name: string;
}

export interface ProfileResponse {
  admissionYear: number | null;
  graduationYear: number | null;
  dateOfBirth: string | null;
  phoneNumber: string | null;
  academicProgram: string | null;
  currentSemester: number | null;
  cgpa: number | null;
  careerProfile: string | null;
}

/** Staff-only roles — STUDENT is excluded from the staff frontend. */
export type StaffRole = 'UTPO' | 'DTPO';

/** Staff login request using the identifier (email) field. */
export interface StaffLoginRequest {
  identifier: string;
  password: string;
}
