import { Component } from '@angular/core';
import { StaffDashboardComponent } from '../../shared/staff-dashboard/staff-dashboard.component';

@Component({
  selector: 'app-dtpo-dashboard',
  standalone: true,
  imports: [StaffDashboardComponent],
  template: '<app-staff-dashboard [role]="\'DTPO\'" />'
})
export class DtpoDashboardComponent {}
