import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';

import { environment } from '../../../environments/environment';
import { Company, CompanyRequest, Coordinator, CoordinatorRequest, DashboardSummary, DepartmentInfo, DriveRequest, PlacementDrive, RecruitmentRound, RoundCandidate, RoundRequest, StaffApplicationPage, StaffStudentPage } from '../models/placement.models';

@Injectable({ providedIn: 'root' })
export class PlacementService {
  private readonly apiUrl = environment.apiBaseUrl;

  constructor(private http: HttpClient) {}

  listCompanies(): Observable<Company[]> {
    return this.http.get<Company[]>(`${this.apiUrl}/api/placement/companies`);
  }

  createCompany(request: CompanyRequest): Observable<Company> {
    return this.http.post<Company>(`${this.apiUrl}/api/placement/companies`, request);
  }

  listDrives(): Observable<PlacementDrive[]> {
    return this.http.get<PlacementDrive[]>(`${this.apiUrl}/api/placement/drives`);
  }

  createDrive(request: DriveRequest): Observable<PlacementDrive> {
    return this.http.post<PlacementDrive>(`${this.apiUrl}/api/placement/drives`, request);
  }

  publishDrive(driveId: number): Observable<PlacementDrive> {
    return this.http.post<PlacementDrive>(`${this.apiUrl}/api/placement/drives/${driveId}/publish`, {});
  }

  listApplications(page = 0, size = 25, search = '', status = ''): Observable<StaffApplicationPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) params = params.set('search', search.trim());
    if (status) params = params.set('status', status);
    return this.http.get<StaffApplicationPage>(`${this.apiUrl}/api/placement/applications`, { params });
  }

  listStudents(page = 0, size = 25, search = ''): Observable<StaffStudentPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search.trim()) params = params.set('search', search.trim());
    return this.http.get<StaffStudentPage>(`${this.apiUrl}/api/placement/students`, { params });
  }

  dashboardSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.apiUrl}/api/placement/dashboard/summary`);
  }

  listDepartments(): Observable<DepartmentInfo[]> {
    return this.http.get<DepartmentInfo[]>(`${this.apiUrl}/api/placement/departments`);
  }

  listCoordinators(): Observable<Coordinator[]> {
    return this.http.get<Coordinator[]>(`${this.apiUrl}/api/placement/coordinators`);
  }

  createCoordinator(request: CoordinatorRequest): Observable<Coordinator> {
    return this.http.post<Coordinator>(`${this.apiUrl}/api/placement/coordinators`, request);
  }

  listRounds(driveId: number): Observable<RecruitmentRound[]> {
    return this.http.get<RecruitmentRound[]>(`${this.apiUrl}/api/placement/drives/${driveId}/rounds`);
  }
  createRound(driveId: number, request: RoundRequest): Observable<RecruitmentRound> {
    return this.http.post<RecruitmentRound>(`${this.apiUrl}/api/placement/drives/${driveId}/rounds`, request);
  }
  updateRound(driveId: number, roundId: number, request: RoundRequest): Observable<RecruitmentRound> {
    return this.http.put<RecruitmentRound>(`${this.apiUrl}/api/placement/drives/${driveId}/rounds/${roundId}`, request);
  }
  deleteRound(driveId: number, roundId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/placement/drives/${driveId}/rounds/${roundId}`);
  }
  listRoundCandidates(driveId: number, roundId: number): Observable<RoundCandidate[]> {
    return this.http.get<RoundCandidate[]>(`${this.apiUrl}/api/placement/drives/${driveId}/rounds/${roundId}/candidates`);
  }
  saveRoundResult(driveId: number, roundId: number, applicationId: number, result: string): Observable<unknown> {
    return this.http.put(`${this.apiUrl}/api/placement/drives/${driveId}/rounds/${roundId}/results/${applicationId}`, { result });
  }
  publishRound(driveId: number, roundId: number): Observable<RecruitmentRound> {
    return this.http.post<RecruitmentRound>(`${this.apiUrl}/api/placement/drives/${driveId}/rounds/${roundId}/publish`, {});
  }
  setFinalOutcome(driveId: number, applicationId: number, outcome: 'SELECTED' | 'REJECTED'): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/api/placement/drives/${driveId}/applications/${applicationId}/outcome`, { outcome });
  }
}
