import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  JobResponse,
  FullJobRequest,
  JobHistoryResponse,
  DeadLetterJobResponse,
  JobStatsResponse
} from '../models';

@Injectable({ providedIn: 'root' })
export class JobService {
  private readonly apiUrl = `${environment.apiUrl}/jobs`;

  constructor(private http: HttpClient) {}

  getAll(): Observable<JobResponse[]> {
    return this.http.get<JobResponse[]>(this.apiUrl);
  }

  getById(id: number): Observable<JobResponse> {
    return this.http.get<JobResponse>(`${this.apiUrl}/${id}`);
  }

  createFull(job: FullJobRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/full`, job);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  pause(id: number): Observable<JobResponse> {
    return this.http.put<JobResponse>(`${this.apiUrl}/${id}/pause`, {});
  }

  resume(id: number): Observable<JobResponse> {
    return this.http.put<JobResponse>(`${this.apiUrl}/${id}/resume`, {});
  }

  getHistory(jobId: number): Observable<JobHistoryResponse[]> {
    return this.http.get<JobHistoryResponse[]>(`${this.apiUrl}/${jobId}/history`);
  }

  getDeadLetterJobs(): Observable<DeadLetterJobResponse[]> {
    return this.http.get<DeadLetterJobResponse[]>(`${this.apiUrl}/dead-letter`);
  }

  replayDeadLetter(id: number): Observable<JobResponse> {
    return this.http.post<JobResponse>(`${this.apiUrl}/dead-letter/${id}/replay`, {});
  }

  getStats(): Observable<JobStatsResponse> {
    return this.http.get<JobStatsResponse>(`${this.apiUrl}/stats`);
  }
}
