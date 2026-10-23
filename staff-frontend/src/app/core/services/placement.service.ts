import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';

import { environment } from '../../../environments/environment';
import { Company, CompanyRequest, Coordinator, CoordinatorRequest, DashboardSummary, DepartmentInfo, DriveRequest, PlacementDrive, StaffApplicationPage, StaffStudentPage } from '../models/placement.models';

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
}
