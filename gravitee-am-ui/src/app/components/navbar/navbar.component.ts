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
import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from "@angular/router";
import {AuthService} from "../../services/auth.service";
import {DomainService} from "../../services/domain.service";
import {Subscription} from "rxjs";
import {NavbarService} from "./navbar.service";
import {SnackbarService} from "../../services/snackbar.service";
import * as _ from 'lodash';
import {SidenavService} from "../sidenav/sidenav.service";
import {EnvironmentService} from "../../services/environment.service";
import {MatSelectChange} from "@angular/material/select";

@Component({
  selector: 'gv-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.scss']
})
export class NavbarComponent implements OnInit, OnDestroy {
  private navbarSubscription: Subscription;
  private sidenavSubscription: Subscription;
  reducedMode = false;
  domains: any[];
  currentResource: any = {};
  navLinks: any = [
    {'href': '/domains/new', 'label': 'Create domain', 'icon': 'add'},
    {'href': '/settings', 'label': 'Global settings', 'icon': 'settings'},
    {'href': '/logout', 'label': 'Sign out', 'icon': 'exit_to_app'},
  ];
  currentEnvironment;
  environments: any[] = [];

  constructor(private authService: AuthService,
              private domainService: DomainService,
              private navbarService: NavbarService,
              private snackbarService: SnackbarService,
              private sidenavService: SidenavService,
              private environmentService: EnvironmentService,
              public router: Router) {

    this.currentEnvironment = this.environmentService.getCurrentEnvironment();
    this.initNavLinks();
    this.initEnvironments();
  }

  ngOnInit() {
    this.navbarSubscription = this.navbarService.currentDomainObs$.subscribe(data => this.currentResource = data);
    this.sidenavSubscription = this.sidenavService.resizeSidenavObservable.subscribe(reducedMode => this.reducedMode = reducedMode);
  }

  ngOnDestroy(): void {
    this.navbarSubscription.unsubscribe();
    this.sidenavSubscription.unsubscribe();
  }

  get user() {
    return this.authService.user() != null ? this.authService.user() : null;
  }

  listDomains() {
    if(this.currentEnvironment) {
      this.domainService.list().subscribe(data => this.domains = data);
    } else {
      this.domains = [];
    }
  }

  goTo(routerLink) {
    // needed to trick reuse route strategy, skipLocationChange to avoid /dummy to go into history
    this.router.navigateByUrl('/dummy', {skipLocationChange: true})
      .then(() => this.router.navigate(routerLink));
  }

  displayBreadcrumb(): boolean {
    return !this.router.url.startsWith('/domains/new') &&
      !this.router.url.startsWith('/login') &&
      !this.router.url.startsWith('/logout') &&
      !this.router.url.startsWith('/404');
  }

  displayEnvironments(): boolean {
    return this.currentEnvironment &&
      !this.router.url.startsWith('/settings') &&
      !this.router.url.startsWith('/login') &&
      !this.router.url.startsWith('/logout') &&
      !this.router.url.startsWith('/404');
  }

  private initNavLinks() {
    if (this.currentEnvironment === undefined || !this.canDisplay(['domain_create'])) {
      _.remove(this.navLinks, {href: '/domains/new'});
    }
    if (!this.canDisplay(['organization_settings_read'])) {
      _.remove(this.navLinks, {href: '/settings'});
    }
  }

  private initEnvironments() {
    this.environmentService.getAllEnvironments().subscribe(environments => {
      this.environments = environments;
    });
  }

  private canDisplay(permissions): boolean {
    return this.authService.hasPermissions(permissions);
  }

  switchEnvironment($event: MatSelectChange) {
    this.environmentService.setCurrentEnvironment($event.value);

    if (this.router.url !== '/') {
      this.router.navigate(["/"]);
    }
  }
}
