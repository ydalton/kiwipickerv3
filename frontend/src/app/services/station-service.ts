import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import Station from '../models/station';

@Injectable({
  providedIn: 'root'
})
export class StationService {
  private apiUrl = "http://localhost:3000"

  constructor(private http: HttpClient) { }

  getStations(): Observable<Station[]> {
    return this.http.get<Station[]>(this.apiUrl + "/stations");
  }
  postStation(station: Station): Observable<Station> {
    return this.http.post<Station>(this.apiUrl + "/stations", station);
  }
  deleteStation(id: number): Observable<void> {
    return this.http.delete<void>(this.apiUrl + `/stations/${id}`, {}).pipe()
  }
}
