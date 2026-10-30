import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { PlacementService } from '../../../core/services/placement.service';
import { MeResponse, StaffRole } from '../../../core/models/auth.models';
import { ApplicationStatus, Company, CompanyRequest, Coordinator, CoordinatorRequest, DashboardSummary, DepartmentInfo, DriveRequest, PlacementDrive, RecruitmentRound, RoundCandidate, RoundRequest, StaffApplication, StaffStudent } from '../../../core/models/placement.models';

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
  selectedApplicationIds = signal<number[]>([]);

  companyForm: CompanyRequest = this.emptyCompany();
  driveForm: DriveRequest = this.emptyDrive();
  coordinatorForm: CoordinatorRequest = this.emptyCoordinator();
  roundForm: RoundRequest = { name: '', order: 1, finalRound: false };

  get navItems(): DashboardNavItem[] {
    return this.role === 'UTPO'
      ? [...this.baseNavItems, { id: 'coordinators', label: 'Department coordinators', icon: '♙' }]
      : [...this.baseNavItems, { id: 'rounds', label: 'Round management', icon: '◉' }];
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

  allVisibleApplicationsSelected(): boolean {
    const visible = this.filteredApplications();
    return visible.length > 0 && visible.every(application => this.selectedApplicationIds().includes(application.id));
  }

  toggleApplicationSelection(applicationId: number): void {
    this.selectedApplicationIds.update(ids => ids.includes(applicationId) ? ids.filter(id => id !== applicationId) : [...ids, applicationId]);
  }

  toggleAllVisibleApplications(): void {
    const visibleIds = this.filteredApplications().map(application => application.id);
    if (this.allVisibleApplicationsSelected()) {
      this.selectedApplicationIds.update(ids => ids.filter(id => !visibleIds.includes(id)));
    } else {
      this.selectedApplicationIds.update(ids => Array.from(new Set([...ids, ...visibleIds])));
    }
  }

  clearApplicationSelection(): void { this.selectedApplicationIds.set([]); }

  loadFilteredApplications(): void {
    const companyFilter = this.companyFilter();
    const selectedCompany = typeof companyFilter === 'number' ? companyFilter : undefined;
    const applicationStatuses: ApplicationStatus[] = ['APPLIED', 'SHORTLISTED', 'SELECTED', 'REJECTED', 'WITHDRAWN'];
    const selectedStatus = this.statusFilter();
    const status = applicationStatuses.includes(selectedStatus as ApplicationStatus)
      ? selectedStatus as ApplicationStatus
      : '';
    this.placementService.listApplications(0, 100, '', status, selectedCompany, this.minCgpaFilter() ?? undefined, this.maxCgpaFilter() ?? undefined).subscribe({
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

  submitCompany(): void {
    if (!this.companyForm.name.trim()) {
      this.errorMessage.set('Company name is required.');
      return;
    }
    this.startSave();
    this.placementService.createCompany(this.companyForm).subscribe({
      next: company => {
        this.companies.update(items => [...items, company]);
        this.companyForm = this.emptyCompany();
        this.showCompanyForm.set(false);
        this.finishSave('Company created successfully.');
      },
      error: error => this.failSave(this.apiError(error, 'Company could not be created. Check the details and try again.'))
    });
  }

  submitDrive(): void {
    if (!this.driveForm.companyId || !this.driveForm.title.trim() || !this.driveForm.applicationDeadline || !this.driveForm.jobDescription.trim() || !this.driveForm.location.trim() || !this.driveForm.packageSalary.trim()) {
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
        this.driveForm = this.emptyDrive();
        this.showDriveForm.set(false);
        this.finishSave('Placement drive created as a draft.');
      },
      error: error => this.failSave(this.apiError(error, 'Placement drive could not be created. Confirm the active academic year and required fields.'))
    });
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

  setApplicationOutcome(application: StaffApplication, outcome: 'SELECTED' | 'REJECTED'): void {
    if (!confirm(`${outcome === 'SELECTED' ? 'Select' : 'Reject'} ${application.studentName}'s application?`)) return;
    this.startSave();
    this.placementService.setFinalOutcome(application.driveId, application.id, outcome).subscribe({
      next: updated => {
        this.applications.update(items => items.map(item => item.id === updated.id ? { ...item, status: updated.status } : item));
        this.candidates.update(items => items.map(item => item.applicationId === updated.id ? { ...item, applicationStatus: updated.status, result: outcome === 'SELECTED' ? 'PASSED' : 'FAILED' } : item));
        this.finishSave(`Application marked ${outcome === 'SELECTED' ? 'selected' : 'not selected'}.`);
      },
      error: error => this.failSave(error.error?.detail ?? error.error?.message
        ?? 'The drive must not be cancelled, the final round must be published, and the student must have passed all previous rounds before recording an outcome.')
    });
  }

  bulkApplicationOutcome(outcome: 'SELECTED' | 'REJECTED'): void {
    const ids = this.selectedApplicationIds();
    if (!ids.length || !confirm(`${outcome === 'SELECTED' ? 'Select' : 'Reject'} ${ids.length} application(s)?`)) return;
    const selected = this.applications().filter(application => ids.includes(application.id));
    this.startSave();
    forkJoin(selected.map(application => this.placementService.setFinalOutcome(application.driveId, application.id, outcome))).subscribe({
      next: updated => {
        const updatedById = new Map(updated.map(application => [application.id, application.status]));
        this.applications.update(items => items.map(item => updatedById.has(item.id) ? { ...item, status: updatedById.get(item.id)! } : item));
        this.candidates.update(items => items.map(item => updatedById.has(item.applicationId) ? { ...item, applicationStatus: updatedById.get(item.applicationId)!, result: outcome === 'SELECTED' ? 'PASSED' : 'FAILED' } : item));
        this.clearApplicationSelection();
        this.finishSave(`${updated.length} application(s) updated.`);
      },
      error: error => this.failSave(error.error?.detail ?? error.error?.message ?? 'One or more applications could not be updated. Confirm the final-round rules for the selected students.')
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
  private emptyCompany(): CompanyRequest { return { name: '', description: '', website: '', hrContactName: '', hrContactEmail: '', hrContactPhone: '' }; }
  private emptyDrive(): DriveRequest { return { companyId: 0, departmentId: null, title: '', jobDescription: '', jobType: 'ONSITE', location: '', packageSalary: '', applicationDeadline: '', minimumCgpa: null, graduationYear: null, maximumActiveBacklogs: null, eligibleProgramIds: [] }; }
  private emptyCoordinator(): CoordinatorRequest { return { fullName: '', universityEmail: '', departmentId: null, initialPassword: '' }; }
  logout(): void { this.authService.logout(); }
}
