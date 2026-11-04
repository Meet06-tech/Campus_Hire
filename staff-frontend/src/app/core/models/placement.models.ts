export interface CompanyRequest {
  name: string;
  description: string;
  website: string;
  hrContactName: string;
  hrContactEmail: string;
  hrContactPhone: string;
  employmentType?: string | null;
  positionType?: string | null;
  defaultLocation?: string | null;
  ctcAmount?: number | null;
  ctcCurrency?: string | null;
  stipend?: string | null;
  otherBenefits?: string | null;
  bondDetails?: string | null;
}

export interface Company {
  id: number;
  name: string;
  description: string | null;
  website: string | null;
  hrContactName: string | null;
  hrContactEmail: string | null;
  hrContactPhone: string | null;
  employmentType?: string | null;
  positionType?: string | null;
  defaultLocation?: string | null;
  ctcAmount?: number | null;
  ctcCurrency?: string | null;
  stipend?: string | null;
  otherBenefits?: string | null;
  bondDetails?: string | null;
}

export interface PlacementAttachment { id: number; fileName: string; contentType: string; sizeBytes: number; }

export interface DriveRequest {
  companyId: number;
  departmentId: number | null;
  title: string;
  jobDescription: string;
  jobType: 'ONSITE' | 'HYBRID' | 'REMOTE';
  location: string;
  packageSalary?: string | null;
  applicationDeadline: string;
  minimumCgpa: number | null;
  graduationYear: number | null;
  maximumActiveBacklogs: number | null;
  eligibleProgramIds: number[];
  employmentType?: string | null;
  positionType?: string | null;
  ctcAmount?: number | null;
  ctcCurrency?: string | null;
  stipend?: string | null;
  otherBenefits?: string | null;
  bondDetails?: string | null;
}

export interface PlacementDrive {
  id: number;
  company: Company;
  departmentId: number | null;
  title: string;
  jobDescription: string;
  jobType: string;
  location: string;
  packageSalary?: string | null;
  applicationDeadline: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELLED' | 'COMPLETED' | 'ARCHIVED';
  minimumCgpa: number | null;
  graduationYear: number | null;
  maximumActiveBacklogs: number | null;
  eligibleProgramCodes: string[];
  employmentType?: string | null;
  positionType?: string | null;
  ctcAmount?: number | null;
  ctcCurrency?: string | null;
  stipend?: string | null;
  otherBenefits?: string | null;
  bondDetails?: string | null;
}

export type ApplicationStatus = 'APPLIED' | 'SHORTLISTED' | 'APPLICATION_REJECTED' | 'REJECTED' | 'SELECTED' | 'WITHDRAWN';

export interface StaffApplication {
  id: number;
  studentId: number;
  studentName: string;
  universityEmail: string;
  enrollmentNumber: string;
  departmentId: number;
  departmentCode: string;
  departmentName: string;
  academicProgram: string | null;
  driveId: number;
  driveTitle: string;
  companyName: string;
  appliedAt: string;
  status: ApplicationStatus;
  studentCgpa: number | null;
  studentGraduationYear: number | null;
}

export interface StaffApplicationPage {
  items: StaffApplication[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface StaffStudent {
  id: number;
  fullName: string;
  universityEmail: string;
  enrollmentNumber: string;
  departmentId: number;
  departmentCode: string;
  departmentName: string;
  academicProgram: string | null;
  graduationYear: number | null;
  cgpa: number | null;
  applicationCount: number;
}

export interface StaffStudentPage {
  items: StaffStudent[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface DashboardSummary {
  companies: number;
  drives: number;
  publishedDrives: number;
  applications: number;
  selectedApplications: number;
  students: number;
  studentsWithApplications: number;
}

export interface DepartmentInfo {
  id: number;
  code: string;
  name: string;
}

export interface CoordinatorRequest {
  fullName: string;
  universityEmail: string;
  departmentId: number | null;
  initialPassword: string;
}

export interface Coordinator {
  id: number;
  fullName: string;
  universityEmail: string;
  department: DepartmentInfo;
  active: boolean;
}

export type RoundResultStatus = 'PENDING' | 'PASSED' | 'FAILED';
export interface RecruitmentRound {
  id: number;
  driveId: number;
  order: number;
  name: string;
  finalRound: boolean;
  resultsPublished: boolean;
  totalCandidates: number;
  passed: number;
  failed: number;
  pending: number;
}
export interface RoundRequest { name: string; order: number; finalRound: boolean; }
export interface ApplicationResponse {
  id: number;
  driveId: number;
  studentId: number;
  appliedAt: string;
  status: ApplicationStatus;
}

export interface RoundCandidate {
  applicationId: number;
  studentId: number;
  studentName: string;
  enrollmentNumber: string;
  academicProgram: string | null;
  result: RoundResultStatus;
  applicationStatus: ApplicationStatus;
}
export interface StudentRoundProgress {
  roundId: number;
  order: number;
  name: string;
  resultsPublished: boolean;
  status: 'CLEARED' | 'NOT_CLEARED' | 'NOT_APPLICABLE' | 'AWAITING_RESULT' | 'UPCOMING';
}
export interface StudentApplicationProgress {
  applicationId: number;
  driveId: number;
  companyName: string;
  driveTitle: string;
  appliedAt: string;
  applicationStatus: ApplicationStatus;
  finalOutcome: 'SELECTED' | 'NOT_SELECTED' | null;
  rounds: StudentRoundProgress[];
}
