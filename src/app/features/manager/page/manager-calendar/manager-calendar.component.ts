import { Component, OnInit, OnDestroy, ViewChild, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { FullCalendarModule, FullCalendarComponent } from '@fullcalendar/angular';
import dayGridPlugin from '@fullcalendar/daygrid';
import multiMonthPlugin from '@fullcalendar/multimonth';
import interactionPlugin from '@fullcalendar/interaction';
import { EventInput } from '@fullcalendar/core';
import { ManagerService } from '../../../../core/services/manager.service';
@Component({
  selector: 'app-manager-calendar',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatFormFieldModule, MatSelectModule, FullCalendarModule
],
  templateUrl: './manager-calendar.component.html',
  styleUrls: ['./manager-calendar.component.scss']
})
export class ManagerCalendarComponent implements OnInit, OnDestroy {
  @ViewChild('calendar') calendarComponent!: FullCalendarComponent;

  selectedYear = new Date().getFullYear();
  selectedMonth = new Date().getMonth();
  years: number[] = [];
  months = ['Janvier', 'Février', 'Mars', 'Avril', 'Mai', 'Juin', 'Juillet', 'Août', 'Septembre', 'Octobre', 'Novembre', 'Décembre'];

  calendarOptions: any = {
    initialView: 'dayGridMonth',
    locale: 'fr',
    plugins: [dayGridPlugin, interactionPlugin, multiMonthPlugin],
    events: [] as EventInput[],
    height: 'auto',
    headerToolbar: {
      left: 'prev,next today',
      center: 'title',
      right: 'dayGridMonth,dayGridWeek,multiMonthYear'
    },
    buttonText: {
      today: "Aujourd'hui",
      month: 'Mois',
      week: 'Semaine',
      multiMonthYear: 'Année'
    }
  };

  private refreshInterval: any;
  private readonly REFRESH_INTERVAL_MS = 15000;

  constructor(
    private managerService: ManagerService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.generateYears();
    this.loadCalendarEvents();
    this.startAutoRefresh();
  }

  ngOnDestroy(): void {
    if (this.refreshInterval) clearInterval(this.refreshInterval);
  }

  generateYears(): void {
    const currentYear = new Date().getFullYear();
    for (let i = currentYear - 3; i <= currentYear + 3; i++) {
      this.years.push(i);
    }
  }

  changeYearMonth(): void {
    if (this.calendarComponent) {
      const calendarApi = this.calendarComponent.getApi();
      const date = new Date(this.selectedYear, this.selectedMonth, 1);
      calendarApi.gotoDate(date);
    }
  }

  loadCalendarEvents(): void {
    this.managerService.getCalendarEvents().subscribe({
      next: (events: EventInput[]) => {
        if (this.calendarComponent) {
          const calendarApi = this.calendarComponent.getApi();
          calendarApi.removeAllEventSources();
          calendarApi.addEventSource(events);
        } else {
          this.calendarOptions = { ...this.calendarOptions, events };
        }
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Erreur chargement calendrier manager', err)
    });
  }

  refreshEvents(): void {
    this.loadCalendarEvents();
  }

  startAutoRefresh(): void {
    this.refreshInterval = setInterval(() => this.refreshEvents(), this.REFRESH_INTERVAL_MS);
  }
}