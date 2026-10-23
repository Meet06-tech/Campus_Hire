export interface CompanyRequest {
  name: string;
  description: string;
  website: string;
  hrContactName: string;
  hrContactEmail: string;
  hrContactPhone: string;
}

export interface Company {
  id: number;
  name: string;
  description: string | null;
  website: string | null;
  hrContactName: string | null;
  hrContactEmail: string | null;
  hrContactPhone: string | null;
}

export interface DriveRequest {
  companyId: number;
  departmentId: number | null;
  title: string;
  jobDescription: string;
  jobType: 'ONSITE' | 'HYBRID' | 'REMOTE';
  location: string;
  packageSalary: string;
  applicationDeadline: string;
  minimumCgpa: number | null;
  graduationYear: number | null;
  maximumActiveBacklogs: number | null;
  eligibleProgramIds: number[];
}

export interface PlacementDrive {
  id: number;
  company: Company;
  departmentId: number | null;
  title: string;
  jobDescription: string;
  jobType: string;
  location: string;
  packageSalary: string;
  applicationDeadline: string;
  status: 'DRAFT' | 'PUBLISHED' | 'CLOSED' | 'CANCELLED' | 'ARCHIVED';
  minimumCgpa: number | null;
  graduationYear: number | null;
  maximumActiveBacklogs: number | null;
  eligibleProgramCodes: string[];
}

export type ApplicationStatus = 'APPLIED' | 'SHORTLISTED' | 'REJECTED' | 'SELECTED' | 'WITHDRAWN';

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
