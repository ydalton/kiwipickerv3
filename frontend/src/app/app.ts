import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { StationForm } from './components/station-form/station-form';
import Station from './models/station';
import { StationService } from './services/station-service';
import { StationView } from './components/station-view/station-view';
import { Button } from './components/button/button';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-root',
  imports: [StationForm, StationView, Button],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  stations: Station[] = [];
  @ViewChild('databaseFile') fileInput!: ElementRef;

  constructor(private stationService: StationService, private http: HttpClient) { }

  getStations() {
    const stations$ = this.stationService.getStations();
    stations$.subscribe({
      next: (stations) => {
        this.stations = stations;
      },
      error: (error) => {
        alert("Failed to get stations: " + error.message)
      }
    })
  }

  ngOnInit(): void {
    this.getStations();
  }

  onRefreshRequested() {
    this.getStations();
  }

  onExportButtonClicked() {
    document.location = "/stations.db";
  }

  onImportButtonClicked() {
    this.fileInput.nativeElement.click();
  }

  onUpload(event: any) {
    let file = event.target.files[0];
    let formData = new FormData;
    formData.append("file", file);
    this.http.post<void>('/stations.db', formData).subscribe({
      next: () => {
        alert("Database successfully imported.");
        // refresh
        this.getStations();
      },
      error: (e: HttpErrorResponse) => {
        alert('Server responded: ' + e.error);
      }
    });
  }
}
