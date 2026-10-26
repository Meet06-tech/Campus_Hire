import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin, of } from 'rxjs';

import { AuthService } from '../../../core/services/auth.service';
import { PlacementService } from '../../../core/services/placement.service';
import { MeResponse, StaffRole } from '../../../core/models/auth.models';
import { Company, CompanyRequest, Coordinator, CoordinatorRequest, DashboardSummary, DepartmentInfo, DriveRequest, PlacementDrive, RecruitmentRound, RoundCandidate, RoundRequest, StaffApplication, StaffStudent } from '../../../core/models/placement.models';

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
      error: () => this.failSave('Company could not be created. Check the details and try again.')
    });
  }

  submitDrive(): void {
    if (!this.driveForm.companyId || !this.driveForm.title.trim() || !this.driveForm.applicationDeadline || !this.driveForm.jobDescription.trim() || !this.driveForm.location.trim() || !this.driveForm.packageSalary.trim()) {
      this.errorMessage.set('Complete the required drive fields before saving.');
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
      error: () => this.failSave('Placement drive could not be created. Confirm the active academic year and required fields.')
    });
  }

  publishDrive(drive: PlacementDrive): void {
    this.startSave();
    this.placementService.publishDrive(drive.id).subscribe({
      next: published => {
        this.drives.update(items => items.map(item => item.id === published.id ? published : item));
        this.finishSave('Placement drive published successfully.');
      },
      error: () => this.failSave('This drive could not be published. It may no longer be a draft or may fail backend validation.')
    });
  }

  openRounds(drive: PlacementDrive): void {
    this.selectedDrive.set(drive);
    this.rounds.set([]);
    this.candidates.set([]);
    this.selectedRound.set(null);
    this.nav('rounds');
    this.placementService.listRounds(drive.id).subscribe({ next: rounds => { this.rounds.set(rounds); if (rounds[0]) this.selectRound(rounds[0]); }, error: () => this.errorMessage.set('Could not load drive rounds.') });
  }
  selectRound(round: RecruitmentRound): void {
    const drive = this.selectedDrive();
    if (!drive) return;
    this.selectedRound.set(round);
    this.placementService.listRoundCandidates(drive.id, round.id).subscribe({ next: candidates => this.candidates.set(candidates), error: () => this.errorMessage.set('Could not load round candidates.') });
  }
  submitRound(): void {
    const drive = this.selectedDrive();
    if (!drive || !this.roundForm.name.trim() || this.roundForm.order < 1) { this.errorMessage.set('Enter a round name and positive order.'); return; }
    this.startSave();
    this.placementService.createRound(drive.id, { ...this.roundForm, name: this.roundForm.name.trim() }).subscribe({
      next: round => { this.rounds.update(items => [...items, round].sort((a, b) => a.order - b.order)); this.roundForm = { name: '', order: this.rounds().length + 1, finalRound: false }; this.finishSave('Round added successfully.'); },
      error: () => this.failSave('Round could not be added. Structural edits are blocked after results exist.')
    });
  }
  removeRound(round: RecruitmentRound): void {
    const drive = this.selectedDrive();
    if (!drive || !confirm(`Remove ${round.name}?`)) return;
    this.startSave();
    this.placementService.deleteRound(drive.id, round.id).subscribe({ next: () => { this.rounds.update(items => items.filter(item => item.id !== round.id)); this.finishSave('Round removed.'); }, error: () => this.failSave('This round cannot be removed after results have been recorded.') });
  }
  saveResult(candidate: RoundCandidate, result: 'PASSED' | 'FAILED'): void {
    const drive = this.selectedDrive(); const round = this.selectedRound();
    if (!drive || !round) return;
    this.placementService.saveRoundResult(drive.id, round.id, candidate.applicationId, result).subscribe({ next: () => { this.candidates.update(items => items.map(item => item.applicationId === candidate.applicationId ? { ...item, result } : item)); this.successMessage.set('Result saved.'); }, error: () => this.errorMessage.set('Result could not be saved.') });
  }
  publishRoundResults(): void {
    const drive = this.selectedDrive(); const round = this.selectedRound();
    if (!drive || !round) return;
    this.startSave();
    this.placementService.publishRound(drive.id, round.id).subscribe({ next: updated => { this.rounds.update(items => items.map(item => item.id === updated.id ? updated : item)); this.selectedRound.set(updated); this.finishSave('Round results published.'); }, error: () => this.failSave('Round results could not be published.') });
  }
  setFinalOutcome(candidate: RoundCandidate, outcome: 'SELECTED' | 'REJECTED'): void {
    const drive = this.selectedDrive();
    if (!drive) return;
    this.startSave();
    this.placementService.setFinalOutcome(drive.id, candidate.applicationId, outcome).subscribe({
      next: () => this.finishSave(`Application marked ${outcome === 'SELECTED' ? 'selected' : 'not selected'}.`),
      error: () => this.failSave('The final outcome could not be saved. Confirm the final round is published and the student passed it.')
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
      error: () => this.failSave('Coordinator could not be created. Check the email, department, and password.')
    });
  }

  private startSave(): void { this.saving.set(true); this.errorMessage.set(null); this.successMessage.set(null); }
  private finishSave(message: string): void { this.saving.set(false); this.successMessage.set(message); }
  private failSave(message: string): void { this.saving.set(false); this.errorMessage.set(message); }
  private toInstant(date: string): string { return new Date(`${date}T23:59:59`).toISOString(); }
  private emptyCompany(): CompanyRequest { return { name: '', description: '', website: '', hrContactName: '', hrContactEmail: '', hrContactPhone: '' }; }
  private emptyDrive(): DriveRequest { return { companyId: 0, departmentId: null, title: '', jobDescription: '', jobType: 'ONSITE', location: '', packageSalary: '', applicationDeadline: '', minimumCgpa: null, graduationYear: null, maximumActiveBacklogs: null, eligibleProgramIds: [] }; }
  private emptyCoordinator(): CoordinatorRequest { return { fullName: '', universityEmail: '', departmentId: null, initialPassword: '' }; }
  logout(): void { this.authService.logout(); }
}
