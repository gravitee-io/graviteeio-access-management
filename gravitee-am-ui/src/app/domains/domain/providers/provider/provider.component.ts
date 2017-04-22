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
import { Component, OnInit } from '@angular/core';
import { ProviderService } from "../../../../services/provider.service";
import { ActivatedRoute } from "@angular/router";
import { PlatformService } from "../../../../services/platform.service";
import { SnackbarService } from "../../../../services/snackbar.service";

@Component({
  selector: 'app-provider',
  templateUrl: './provider.component.html',
  styleUrls: ['./provider.component.scss']
})
export class ProviderComponent implements OnInit {
  private domainId: string;
  private configurationIsValid: boolean = true;
  private configurationPristine: boolean = true;
  provider: any;
  providerSchema: any;
  providerConfiguration: any;

  constructor(private providerService: ProviderService, private platformService: PlatformService,
              private snackbarService: SnackbarService, private route: ActivatedRoute) { }

  ngOnInit() {
    this.domainId = this.route.snapshot.parent.params['domainId'];
    let providerId = this.route.snapshot.params['providerId'];
    this.providerService.get(this.domainId, providerId).map(res => res.json()).subscribe(data => {
      this.provider = data;
      this.providerConfiguration = JSON.parse(this.provider.configuration);
      this.platformService.identitySchema(this.provider.type).map(res => res.json()).subscribe(data => this.providerSchema = data);
    });
  }

  update() {
    this.provider.configuration = JSON.stringify(this.provider.configuration);
    this.providerService.update(this.domainId, this.provider.id, this.provider).map(res => res.json()).subscribe(data => {
      this.snackbarService.open("Provider updated");
    })
  }

  enableProviderUpdate(configurationWrapper) {
    this.configurationPristine = false;
    this.configurationIsValid = configurationWrapper.isValid;
    this.provider.configuration = configurationWrapper.configuration;
  }
}
