/*-
 * #%L
 * com.mobi.web
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2016 - 2019 iNovex Information Systems, Inc.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
import * as angular from 'angular';
import { NgModule } from '@angular/core';
import { downgradeComponent } from '@angular/upgrade/static';
import { MatTabsModule } from '@angular/material';

import { SharedModule } from '../shared/shared.module';

import { GroupTabComponent } from './components/groupTab/groupTab.component';
import { PasswordTabComponent } from './components/passwordTab/passwordTab.component';
import { ProfileTabComponent } from './components/profileTab/profileTab.component';
import { SettingsPageComponent } from './components/settingsPage/settingsPage.component';
import { PreferencesTabComponent } from './components/preferencesTab/preferencesTab.component';
import { PreferenceGroupComponent } from './components/preferenceGroup/preferenceGroup.component';
import { PreferenceFormComponent } from './components/preferenceForm/preferenceForm.component';
import { PreferenceFormFieldComponent } from './components/preferenceFormField/preferenceFormField.component';

@NgModule({
    imports: [
        SharedModule,
        MatTabsModule
    ],
    declarations: [
        SettingsPageComponent,
        ProfileTabComponent,
        GroupTabComponent,
        PasswordTabComponent,
        PreferencesTabComponent,
        PreferenceGroupComponent,
        PreferenceFormComponent,
        PreferenceFormFieldComponent
    ],
    entryComponents: [
        SettingsPageComponent
    ]
})
export class SettingsModule {}

/**
 * @namespace settings
 *
 * The `settings` module provides components that make up the Settings module in the Mobi application.
 */
angular.module('settings', [])
    .directive('groupTab', downgradeComponent({component: GroupTabComponent}) as angular.IDirectiveFactory)
    .component('passwordTab', downgradeComponent({component: PasswordTabComponent}) as angular.IDirectiveFactory)
    .directive('profileTab', downgradeComponent({component: ProfileTabComponent}) as angular.IDirectiveFactory)
    .directive('settingsPage', downgradeComponent({component: SettingsPageComponent}) as angular.IDirectiveFactory)
    .directive('preferencesTab', downgradeComponent({component: PreferencesTabComponent}) as angular.IDirectiveFactory)
    .directive('preferenceGroup', downgradeComponent({component: PreferenceGroupComponent}) as angular.IDirectiveFactory)
    .directive('preferenceForm', downgradeComponent({component: PreferenceFormComponent}) as angular.IDirectiveFactory)
    .directive('preferenceFormField', downgradeComponent({component: PreferenceFormFieldComponent}) as angular.IDirectiveFactory);
