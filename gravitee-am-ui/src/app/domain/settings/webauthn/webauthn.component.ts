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
import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {DomainService} from '../../../services/domain.service';
import {SnackbarService} from '../../../services/snackbar.service';
import {AuthService} from '../../../services/auth.service';
import {EntrypointService} from '../../../services/entrypoint.service';

@Component({
  selector: 'app-domain-webauthn',
  templateUrl: './webauthn.component.html',
  styleUrls: ['./webauthn.component.scss']
})
export class DomainSettingsWebAuthnComponent implements OnInit {
  @ViewChild('webAuthnForm', { static: true }) form: any;
  private entrypoint: any;
  private baseUrl: string;
  domainId: string;
  domain: any = {};
  formChanged = false;
  readonly = false;
  userVerifications: string[] = ['required', 'preferred', 'discouraged'];
  authenticatorAttachments: string[] = ['cross_platform', 'platform'];
  attestationConveyancePreferences: string[] = ['none', 'indirect', 'direct'];

  constructor(private domainService: DomainService,
              private snackbarService: SnackbarService,
              private authService: AuthService,
              private route: ActivatedRoute,
              private entrypointService: EntrypointService) { }

  ngOnInit() {
    this.domain = this.route.snapshot.data['domain'];
    this.domainId = this.domain.id;
    this.entrypoint = this.route.snapshot.data['entrypoint'];
    this.baseUrl = this.entrypointService.resolveBaseUrl(this.entrypoint, this.domain);
    this.domain.webAuthnSettings = this.domain.webAuthnSettings || {};
    const url = new URL(this.domain.webAuthnSettings.origin || this.baseUrl);
    this.domain.webAuthnSettings.origin = url.origin;
    this.domain.webAuthnSettings.relyingPartyId = this.domain.webAuthnSettings.relyingPartyId || url.hostname;
    this.readonly = !this.authService.hasPermissions(['domain_settings_update']);
  }

  save() {
    this.domainService.patchWebAuthnSettings(this.domainId, this.domain).subscribe(data => {
      this.domain = data;
      this.formChanged = false;
      this.form.reset(this.domain.webAuthnSettings);
      this.snackbarService.open('WebAuthn configuration updated');
    });
  }
}
