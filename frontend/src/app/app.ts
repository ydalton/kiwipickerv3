import { Component, OnInit } from '@angular/core';
import { StationForm } from './components/station-form/station-form';
import Station from './models/station';
import { StationService } from './services/station-service';
import { StationView } from './components/station-view/station-view';

@Component({
  selector: 'app-root',
  imports: [StationForm, StationView],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  stations: Station[] = [];

  constructor(private stationService: StationService) {}

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
    console.log("Refresh")
    this.getStations();
  }
}
