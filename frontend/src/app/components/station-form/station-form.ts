import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import Station from '../../models/station';
import { StationService } from '../../services/station-service';

@Component({
  selector: 'station-form',
  imports: [FormsModule],
  templateUrl: './station-form.html',
  styleUrl: './station-form.css'
})
export class StationForm {
  @Output() refreshRequested = new EventEmitter<void>();
  station: Station = {
    id: 0,
    name: "",
    url: ""
  };

  constructor(private stationService: StationService) {}

  reset() {
    this.station.name = "";
    this.station.url = "";
  }

  onSubmit() {
    this.stationService.postStation(this.station).subscribe({
      next: () => {
        this.reset();
        this.refreshRequested.emit();
      },
      error: (err: any) => {
        alert(err.message);
      }
    });

  }
}
