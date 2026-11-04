import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin, of } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { PlacementService } from '../../../core/services/placement.service';
import { MeResponse, StaffRole } from '../../../core/models/auth.models';
import { ApplicationStatus, Company, CompanyRequest, Coordinator, CoordinatorRequest, DashboardSummary, DepartmentInfo, DriveRequest, PlacementAttachment, PlacementDrive, RecruitmentRound, RoundCandidate, RoundRequest, StaffApplication, StaffStudent } from '../../../core/models/placement.models';

type DashboardSection = 'overview' | 'drives' | 'companies' | 'applications' | 'students' | 'reports' | 'coordinators' | 'rounds';

interface DashboardNavItem {
  id: DashboardSection;
  label: string;
  icon: string;
}

interface DashboardMetric {
  label: string;
  value: string;
  helper: string;
  tone: 'blue' | 'green' | 'amber' | 'slate';
}

@Component({
  selector: 'app-staff-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './staff-dashboard.component.html',
  styleUrl: './staff-dashboard.component.scss'
})
export class StaffDashboardComponent implements OnInit {
  @Input({ required: true }) role!: StaffRole;

  private readonly baseNavItems: DashboardNavItem[] = [
    { id: 'overview', label: 'Overview', icon: '⌂' },
    { id: 'drives', label: 'Placement drives', icon: '▣' },
    { id: 'companies', label: 'Companies', icon: '◈' },
    { id: 'applications', label: 'Applied students', icon: '✓' },
    { id: 'students', label: 'Students', icon: '♙' },
    { id: 'reports', label: 'Reports', icon: '▥' }
  ];

  user = signal<MeResponse | null>(null);
  companies = signal<Company[]>([]);
  drives = signal<PlacementDrive[]>([]);
  roundDrives = signal<PlacementDrive[]>([]);
  applications = signal<StaffApplication[]>([]);
  students = signal<StaffStudent[]>([]);
  coordinators = signal<Coordinator[]>([]);
  departments = signal<DepartmentInfo[]>([]);
  rounds = signal<RecruitmentRound[]>([]);
  candidates = signal<RoundCandidate[]>([]);
  selectedDrive = signal<PlacementDrive | null>(null);
  selectedRound = signal<RecruitmentRound | null>(null);
  summary = signal<DashboardSummary | null>(null);
  activeSection = signal<DashboardSection>('overview');
  loading = signal(true);
  saving = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showCompanyForm = signal(false);
  showDriveForm = signal(false);
  showCoordinatorForm = signal(false);
  searchTerm = signal('');
  statusFilter = signal('ALL');
  companyFilter = signal<number | 'ALL'>('ALL');
  minCgpaFilter = signal<number | null>(null);
  maxCgpaFilter = signal<number | null>(null);
  applicationSearch = signal('');
  reportDownloading = signal<string | null>(null);
  reportDepartmentId = signal<number | null>(null);

  companyForm: CompanyRequest = this.emptyCompany();
  driveForm: DriveRequest = this.emptyDrive();
  coordinatorForm: CoordinatorRequest = this.emptyCoordinator();
  roundForm: RoundRequest = { name: '', order: 1, finalRound: false };
  attachmentFiles: File[] = [];
  companyAttachmentFiles: File[] = [];
  selectedCompanyAttachments: PlacementAttachment[] = [];
  editingCompanyId: number | null = null;

  get navItems(): DashboardNavItem[] {
    return this.role === 'UTPO'
      ? [...this.baseNavItems, { id: 'coordinators', label: 'Department coordinators', icon: '♙' }]
      : [...this.baseNavItems, { id: 'rounds', label: 'Round management', icon: '◉' }];
  }
  get selectedCompany(): Company | null {
    return this.companies().find(company => company.id === this.driveForm.companyId) ?? null;
  }
  selectCompany(companyId: number): void {
    this.driveForm.companyId = companyId;
    const company = this.companies().find(item => item.id === companyId);
    if (!company) { this.selectedCompanyAttachments = []; return; }
    this.driveForm = { ...this.driveForm, title: company.name, location: company.defaultLocation ?? '', employmentType: company.employmentType ?? '', positionType: company.positionType ?? '', ctcAmount: company.ctcAmount ?? null, ctcCurrency: company.ctcCurrency ?? 'INR', stipend: company.stipend ?? '', jobDescription: company.description ?? '', otherBenefits: company.otherBenefits ?? '', bondDetails: company.bondDetails ?? '' };
    this.placementService.listCompanyAttachments(companyId).subscribe({ next: files => this.selectedCompanyAttachments = files, error: error => this.errorMessage.set(this.apiError(error, 'Company details loaded, but attachments could not be loaded.')) });
  }

  constructor(private authService: AuthService, private placementService: PlacementService) {}

  ngOnInit(): void {
    forkJoin({
      user: this.authService.me(),
      companies: this.placementService.listCompanies(),
      drives: this.placementService.listDrives(),
      applications: this.placementService.listApplications(0, 100),
      students: this.placementService.listStudents(0, 100),
      summary: this.placementService.dashboardSummary(),
      departments: this.role === 'UTPO' ? this.placementService.listDepartments() : of([] as DepartmentInfo[]),
      coordinators: this.role === 'UTPO' ? this.placementService.listCoordinators() : of([] as Coordinator[])
    }).subscribe({
      next: ({ user, companies, drives, applications, students, summary, departments, coordinators }) => {
        this.user.set(user);
        this.companies.set(companies);
        this.drives.set(drives);
        this.applications.set(applications.items);
        this.students.set(students.items);
        this.summary.set(summary);
        this.departments.set(departments);
        this.coordinators.set(coordinators);
        this.loading.set(false);
      },
      error: (error: { status?: number }) => {
        this.loading.set(false);
        if (error.status === 401) this.authService.logout();
        else this.errorMessage.set('Unable to load the dashboard. Please try again.');
      }
    });
  }

  nav(section: DashboardSection): void {
    this.activeSection.set(section);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    if (section === 'rounds') {
      this.placementService.listRoundManagementDrives().subscribe({
        next: drives => this.roundDrives.set(drives),
        error: error => this.errorMessage.set(this.apiError(error, 'Could not load active drives for round management.'))
      });
    }
  }

  get roleLabel(): string { return this.role === 'DTPO' ? 'Department TPO' : 'University TPO'; }
  get scopeLabel(): string { return this.role === 'DTPO' ? (this.user()?.department?.name ?? 'Assigned department') : 'All departments'; }
  companyDriveCount(companyId: number): number { return this.drives().filter(drive => drive.company.id === companyId).length; }
  conversionRate(): number { const total = this.summary()?.applications ?? 0; return total === 0 ? 0 : Math.round(((this.summary()?.selectedApplications ?? 0) / total) * 100); }
  studentParticipationRate(): number { const total = this.summary()?.students ?? 0; return total === 0 ? 0 : Math.round(((this.summary()?.studentsWithApplications ?? 0) / total) * 100); }

  metrics(): DashboardMetric[] {
    const summary = this.summary();
    if (!summary) return [];
    return [
      { label: 'Placement drives', value: String(summary.drives), helper: `${summary.publishedDrives} published`, tone: 'blue' },
      { label: 'Companies', value: String(summary.companies), helper: 'Available in this workspace', tone: 'green' },
      { label: 'Applications', value: String(summary.applications), helper: `${summary.selectedApplications} selected`, tone: 'amber' },
      { label: 'Students tracked', value: String(summary.students), helper: 'Scoped to your access', tone: 'slate' }
    ];
  }

  filteredDrives(): PlacementDrive[] {
    const query = this.searchTerm().trim().toLowerCase();
    const status = this.statusFilter();
    return this.drives().filter(drive => {
      const matchesStatus = status === 'ALL' || drive.status === status;
      const matchesQuery = !query || drive.title.toLowerCase().includes(query) || drive.company.name.toLowerCase().includes(query);
      return matchesStatus && matchesQuery;
    });
  }

  filteredApplications(): StaffApplication[] {
    const company = this.companyFilter();
    const min = this.minCgpaFilter();
    const max = this.maxCgpaFilter();
    return this.applications().filter(application => {
      const matchesCompany = company === 'ALL' || this.companies().some(item => item.id === company && item.name === application.companyName);
      const cgpa = application.studentCgpa;
      const matchesMin = min === null || (cgpa !== null && cgpa >= min);
      const matchesMax = max === null || (cgpa !== null && cgpa <= max);
      return matchesCompany && matchesMin && matchesMax;
    });
  }

  loadFilteredApplications(): void {
    const companyFilter = this.companyFilter();
    const selectedCompany = typeof companyFilter === 'number' ? companyFilter : undefined;
    const applicationStatuses: ApplicationStatus[] = ['APPLIED', 'SHORTLISTED', 'APPLICATION_REJECTED', 'SELECTED', 'REJECTED', 'WITHDRAWN'];
    const selectedStatus = this.statusFilter();
    const status = applicationStatuses.includes(selectedStatus as ApplicationStatus)
      ? selectedStatus as ApplicationStatus
      : '';
    this.placementService.listApplications(0, 100, this.applicationSearch(), status, selectedCompany, this.minCgpaFilter() ?? undefined, this.maxCgpaFilter() ?? undefined).subscribe({
      next: page => this.applications.set(page.items),
      error: error => {
        if (error.status === 401) this.authService.logout();
        else this.errorMessage.set('Could not refresh applications for the selected filters.');
      }
    });
  }

  onApplicationStatusFilterChange(value: string): void {
    this.statusFilter.set(value);
    this.loadFilteredApplications();
  }

  onCompanyFilterChange(value: number | 'ALL'): void {
    this.companyFilter.set(value);
    this.loadFilteredApplications();
  }

  onMinCgpaFilterChange(value: string | number): void {
    this.minCgpaFilter.set(value === '' ? null : Number(value));
    this.loadFilteredApplications();
  }

  onMaxCgpaFilterChange(value: string | number): void {
    this.maxCgpaFilter.set(value === '' ? null : Number(value));
    this.loadFilteredApplications();
  }
  onApplicationSearchChange(value: string): void { this.applicationSearch.set(value); this.loadFilteredApplications(); }
  resetApplicationSearch(): void { this.applicationSearch.set(''); this.loadFilteredApplications(); }

  archiveDrive(drive: PlacementDrive): void {
    if (!confirm(`Remove ${drive.title} from operational lists? Its ${drive.status.toLowerCase()} history will be preserved.`)) return;
    this.startSave();
    this.placementService.archiveDrive(drive.id).subscribe({
      next: archived => { this.drives.update(items => items.filter(item => item.id !== archived.id)); this.finishSave('Drive archived from operational lists. History was preserved.'); },
      error: error => this.failSave(this.apiError(error, 'Only cancelled or completed drives can be archived.'))
    });
  }

  downloadReport(path: string, departmentId?: number): void {
    if (this.reportDownloading()) return;
    this.reportDownloading.set(path);
    this.errorMessage.set(null);
    this.placementService.downloadReport(path, departmentId).pipe(finalize(() => this.reportDownloading.set(null))).subscribe({
      next: blob => { const url = URL.createObjectURL(blob); const anchor = document.createElement('a'); anchor.href = url; anchor.download = path; anchor.click(); URL.revokeObjectURL(url); },
      error: error => this.errorMessage.set(this.apiError(error, 'The report could not be downloaded.')),
      complete: () => this.successMessage.set(`${path.endsWith('.xlsx') ? 'Excel' : 'CSV'} report downloaded.`)
    });
  }
  parseDepartmentId(value: string): number | undefined { return value ? Number(value) : undefined; }

  submitCompany(): void {
    if (!this.companyForm.name.trim()) {
      this.errorMessage.set('Company name is required.');
      return;
    }
    this.startSave();
    const request = this.editingCompanyId ? this.placementService.updateCompany(this.editingCompanyId, this.companyForm) : this.placementService.createCompany(this.companyForm);
    request.subscribe({
      next: company => {
        this.companies.update(items => this.editingCompanyId ? items.map(item => item.id === company.id ? company : item) : [...items, company]);
        const uploads = this.companyAttachmentFiles.map(file => this.placementService.uploadCompanyAttachment(company.id, file));
        if (uploads.length) forkJoin(uploads).subscribe({ error: error => this.errorMessage.set(this.apiError(error, 'Company saved, but an attachment could not be uploaded.')) });
        this.companyAttachmentFiles = [];
        this.companyForm = this.emptyCompany();
        this.showCompanyForm.set(false);
        this.editingCompanyId = null;
        this.finishSave('Company saved successfully.');
      },
      error: error => this.failSave(this.apiError(error, 'Company could not be created. Check the details and try again.'))
    });
  }
  editCompany(company: Company): void {
    this.editingCompanyId = company.id;
    this.companyForm = { name: company.name, description: company.description ?? '', website: company.website ?? '', hrContactName: company.hrContactName ?? '', hrContactEmail: company.hrContactEmail ?? '', hrContactPhone: company.hrContactPhone ?? '', employmentType: company.employmentType ?? '', positionType: company.positionType ?? '', defaultLocation: company.defaultLocation ?? '', ctcAmount: company.ctcAmount ?? null, ctcCurrency: company.ctcCurrency ?? 'INR', stipend: company.stipend ?? '', otherBenefits: company.otherBenefits ?? '', bondDetails: company.bondDetails ?? '' };
    this.placementService.listCompanyAttachments(company.id).subscribe({ next: files => this.selectedCompanyAttachments = files, error: () => this.selectedCompanyAttachments = [] });
    this.showCompanyForm.set(true);
  }

  submitDrive(): void {
    if (!this.driveForm.companyId || !this.driveForm.title.trim() || !this.driveForm.applicationDeadline || !this.driveForm.jobDescription.trim() || !this.driveForm.location.trim() || this.driveForm.ctcAmount == null || !this.driveForm.ctcCurrency?.trim()) {
      this.errorMessage.set('Complete the required drive fields before saving.');
      return;
    }
    const deadline = new Date(`${this.driveForm.applicationDeadline}T23:59:59`);
    if (Number.isNaN(deadline.getTime()) || deadline.getTime() < Date.now()) {
      this.errorMessage.set('Application deadline must be today or a future date.');
      return;
    }
    this.startSave();
    const request = { ...this.driveForm, departmentId: this.role === 'DTPO' ? null : this.driveForm.departmentId, applicationDeadline: this.toInstant(this.driveForm.applicationDeadline) };
    this.placementService.createDrive(request).subscribe({
      next: drive => {
        this.drives.update(items => [...items, drive]);
        const uploads = this.attachmentFiles.map(file => this.placementService.uploadAttachment(drive.id, file));
        if (uploads.length) forkJoin(uploads).subscribe({ error: error => this.errorMessage.set(this.apiError(error, 'The drive was saved, but an attachment could not be uploaded.')) });
        this.attachmentFiles = [];
        this.driveForm = this.emptyDrive();
        this.showDriveForm.set(false);
        this.finishSave('Placement drive created as a draft.');
      },
      error: error => this.failSave(this.apiError(error, 'Placement drive could not be created. Confirm the active academic year and required fields.'))
    });
  }
  onAttachmentFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.attachmentFiles = input.files ? Array.from(input.files) : [];
  }
  onCompanyFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.companyAttachmentFiles = input.files ? Array.from(input.files) : [];
  }

  publishDrive(drive: PlacementDrive): void {
    this.startSave();
    this.placementService.publishDrive(drive.id).subscribe({
      next: published => {
        this.drives.update(items => items.map(item => item.id === published.id ? published : item));
        this.finishSave('Placement drive published successfully.');
      },
      error: error => this.failSave(this.apiError(error, 'This drive could not be published. It may no longer be a draft or may fail backend validation.'))
    });
  }

  cancelDrive(drive: PlacementDrive): void {
    if (!confirm(`Cancel ${drive.title}? Students will no longer be able to apply, but existing applications will remain.`)) return;
    this.startSave();
    this.placementService.cancelDrive(drive.id).subscribe({
      next: cancelled => {
        this.drives.update(items => items.map(item => item.id === cancelled.id ? cancelled : item));
        this.finishSave('Placement drive cancelled. Existing application history was preserved.');
      },
      error: error => this.failSave(this.apiError(error, 'This drive could not be cancelled. Only draft or published drives can be cancelled.'))
    });
  }

  completeDrive(drive: PlacementDrive): void {
    if (!confirm(`Complete ${drive.title}? Recruitment activity must already be finished.`)) return;
    this.startSave();
    this.placementService.completeDrive(drive.id).subscribe({
      next: completed => { this.drives.update(items => items.map(item => item.id === completed.id ? completed : item)); this.finishSave('Placement drive completed. History was preserved.'); },
      error: error => this.failSave(this.apiError(error, 'This drive cannot be completed until all recruitment rounds are finished.'))
    });
  }

  openRounds(drive: PlacementDrive): void {
    this.selectedDrive.set(drive);
    this.rounds.set([]);
    this.candidates.set([]);
    this.selectedRound.set(null);
    this.nav('rounds');
    this.placementService.listRounds(drive.id).subscribe({ next: rounds => { this.rounds.set(rounds); if (rounds[0]) this.selectRound(rounds[0]); }, error: error => this.errorMessage.set(this.apiError(error, 'Could not load drive rounds.')) });
  }
  selectRound(round: RecruitmentRound): void {
    const drive = this.selectedDrive();
    if (!drive) return;
    this.selectedRound.set(round);
    this.placementService.listRoundCandidates(drive.id, round.id).subscribe({ next: candidates => this.candidates.set(candidates), error: error => this.errorMessage.set(this.apiError(error, 'Could not load round candidates.')) });
  }
  submitRound(): void {
    const drive = this.selectedDrive();
    if (!drive || !this.roundForm.name.trim() || this.roundForm.order < 1) { this.errorMessage.set('Enter a round name and positive order.'); return; }
    this.startSave();
    this.placementService.createRound(drive.id, { ...this.roundForm, name: this.roundForm.name.trim() }).subscribe({
      next: round => { this.rounds.update(items => [...items, round].sort((a, b) => a.order - b.order)); this.roundForm = { name: '', order: this.rounds().length + 1, finalRound: false }; this.finishSave('Round added successfully.'); },
      error: error => this.failSave(this.apiError(error, 'Round could not be added. Structural edits are blocked after results exist.'))
    });
  }
  removeRound(round: RecruitmentRound): void {
    const drive = this.selectedDrive();
    if (!drive || !confirm(`Remove ${round.name}?`)) return;
    this.startSave();
    this.placementService.deleteRound(drive.id, round.id).subscribe({ next: () => { this.rounds.update(items => items.filter(item => item.id !== round.id)); this.finishSave('Round removed.'); }, error: error => this.failSave(this.apiError(error, 'This round cannot be removed after results have been recorded.')) });
  }
  saveResult(candidate: RoundCandidate, result: 'PASSED' | 'FAILED'): void {
    const drive = this.selectedDrive(); const round = this.selectedRound();
    if (!drive || !round) return;
    this.placementService.saveRoundResult(drive.id, round.id, candidate.applicationId, result).subscribe({ next: () => { this.candidates.update(items => items.map(item => item.applicationId === candidate.applicationId ? { ...item, result } : item)); this.successMessage.set('Result saved.'); }, error: error => this.errorMessage.set(this.apiError(error, 'Result could not be saved.')) });
  }
  publishRoundResults(): void {
    const drive = this.selectedDrive(); const round = this.selectedRound();
    if (!drive || !round) return;
    this.startSave();
    this.placementService.publishRound(drive.id, round.id).subscribe({ next: updated => { this.rounds.update(items => items.map(item => item.id === updated.id ? updated : item)); this.selectedRound.set(updated); this.finishSave('Round results published.'); }, error: error => this.failSave(this.apiError(error, 'Round results could not be published.')) });
  }
  setFinalOutcome(candidate: RoundCandidate, outcome: 'SELECTED' | 'REJECTED'): void {
    const drive = this.selectedDrive();
    if (!drive) return;
    this.startSave();
    this.placementService.setFinalOutcome(drive.id, candidate.applicationId, outcome).subscribe({
      next: updated => {
        this.applications.update(items => items.map(item => item.id === updated.id ? { ...item, status: updated.status } : item));
        this.candidates.update(items => items.map(item => item.applicationId === candidate.applicationId ? { ...item, applicationStatus: updated.status, result: outcome === 'SELECTED' ? 'PASSED' : 'FAILED' } : item));
        this.finishSave(`Application marked ${outcome === 'SELECTED' ? 'selected' : 'not selected'}.`);
      },
      error: error => this.failSave(error.error?.detail ?? error.error?.message
        ?? 'The final outcome could not be saved. Confirm the drive is not cancelled, the final round is published, and the student passed all previous rounds.')
    });
  }

  rejectApplication(application: StaffApplication): void {
    if (!confirm(`Reject ${application.studentName}'s application for ${application.companyName}?`)) return;
    this.startSave();
    this.placementService.rejectApplication(application.driveId, application.id).subscribe({
      next: updated => {
        this.applications.update(items => items.map(item => item.id === updated.id ? { ...item, status: updated.status } : item));
        this.finishSave('Application rejected. It will not enter upcoming rounds.');
      },
      error: error => this.failSave(this.apiError(error, 'The application could not be rejected.'))
    });
  }

  submitCoordinator(): void {
    if (!this.coordinatorForm.fullName.trim() || !this.coordinatorForm.universityEmail.trim() || !this.coordinatorForm.departmentId || !this.coordinatorForm.initialPassword) {
      this.errorMessage.set('Complete the coordinator name, email, department, and initial password.');
      return;
    }
    this.startSave();
    this.placementService.createCoordinator(this.coordinatorForm).subscribe({
      next: coordinator => {
        this.coordinators.update(items => [...items, coordinator].sort((a, b) => a.fullName.localeCompare(b.fullName)));
        this.coordinatorForm = this.emptyCoordinator();
        this.showCoordinatorForm.set(false);
        this.finishSave('Department coordinator created successfully.');
      },
      error: error => this.failSave(this.apiError(error, 'Coordinator could not be created. Check the email, department, and password.'))
    });
  }

  private startSave(): void { this.saving.set(true); this.errorMessage.set(null); this.successMessage.set(null); }
  private finishSave(message: string): void { this.saving.set(false); this.successMessage.set(message); }
  private failSave(message: string): void { this.saving.set(false); this.errorMessage.set(message); }
  minimumDeadline(): string {
    const today = new Date();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${today.getFullYear()}-${month}-${day}`;
  }
  private toInstant(date: string): string { return new Date(`${date}T23:59:59`).toISOString(); }
  private apiError(error: { error?: { detail?: string; message?: string }; message?: string }, fallback: string): string {
    return error.error?.detail ?? error.error?.message ?? error.message ?? fallback;
  }
  private emptyCompany(): CompanyRequest { return { name: '', description: '', website: '', hrContactName: '', hrContactEmail: '', hrContactPhone: '', employmentType: '', positionType: '', defaultLocation: '', ctcAmount: null, ctcCurrency: 'INR', stipend: '', otherBenefits: '', bondDetails: '' }; }
  private emptyDrive(): DriveRequest { return { companyId: 0, departmentId: null, title: '', jobDescription: '', jobType: 'ONSITE', location: '', applicationDeadline: '', minimumCgpa: null, graduationYear: null, maximumActiveBacklogs: null, eligibleProgramIds: [], employmentType: '', positionType: '', ctcAmount: null, ctcCurrency: 'INR', stipend: '', otherBenefits: '', bondDetails: '' }; }
  private emptyCoordinator(): CoordinatorRequest { return { fullName: '', universityEmail: '', departmentId: null, initialPassword: '' }; }
  logout(): void { this.authService.logout(); }
}
