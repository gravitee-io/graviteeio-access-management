/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {HttpClient} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {AuthService} from './auth.service';
import {AppConfig} from '../../config/app.config';
import {Observable} from 'rxjs';
import {map} from 'rxjs/operators';

@Injectable()
export class InstallationService {
  private installationUrl = AppConfig.settings.baseURL + '/platform/installation';

  constructor(private http: HttpClient) {
  }

  get(): Observable<any> {
    return this.http.get<any>(this.installationUrl);
  }
}
