import { Component } from '@angular/core';
import { StaffDashboardComponent } from '../../shared/staff-dashboard/staff-dashboard.component';

@Component({
  selector: 'app-utpo-dashboard',
  standalone: true,
  imports: [StaffDashboardComponent],
  template: '<app-staff-dashboard [role]="\'UTPO\'" />'
})
export class UtpoDashboardComponent {}
