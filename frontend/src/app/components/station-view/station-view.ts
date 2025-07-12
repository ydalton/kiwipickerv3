import { Component, EventEmitter, Input, Output } from '@angular/core';
import Station from '../../models/station';
import { StationService } from '../../services/station-service';

@Component({
  selector: 'station-view',
  imports: [],
  templateUrl: './station-view.html',
  styleUrl: './station-view.css'
})
export class StationView {
  @Input() station: Station = new Station
  @Output() refreshRequested = new EventEmitter<void>();

  constructor(private stationService: StationService) {}

  deleteStation() {
    if (this.station.id !== undefined && confirm("Are you sure you want to delete this station?")) {
      this.stationService.deleteStation(this.station.id).subscribe({
        next: () => {
          this.refreshRequested.emit();
        },
        error: () => {
          alert("Failed to delete station.");
        }
      });
    }
  }
}
