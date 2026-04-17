import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface JobTemplate {
  id: number;
  name: string;
  description: string;
  jobType: string;
  payload: string;
  cronExpression: string;
  maxRetries: number;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
  ownerName: string;
}

export interface JobTemplateRequest {
  name: string;
  description: string;
  jobType: string;
  payload: string;
  cronExpression: string;
  maxRetries: number;
  isPublic: boolean;
}

@Injectable({ providedIn: 'root' })
export class JobTemplateService {
  private readonly apiUrl = `${environment.apiUrl}/templates`;

  constructor(private http: HttpClient) {}

  create(template: JobTemplateRequest): Observable<JobTemplate> {
    return this.http.post<JobTemplate>(this.apiUrl, template);
  }

  getAvailable(): Observable<JobTemplate[]> {
    return this.http.get<JobTemplate[]>(this.apiUrl);
  }

  getOwn(): Observable<JobTemplate[]> {
    return this.http.get<JobTemplate[]>(`${this.apiUrl}/own`);
  }

  getById(id: number): Observable<JobTemplate> {
    return this.http.get<JobTemplate>(`${this.apiUrl}/${id}`);
  }

  update(id: number, template: JobTemplateRequest): Observable<JobTemplate> {
    return this.http.put<JobTemplate>(`${this.apiUrl}/${id}`, template);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
