/*
 * Lynket
 *
 * Copyright (C) 2019 Arunkumar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package arun.com.chromer

import arun.com.chromer.di.app.AppComponent
import arun.com.chromer.di.app.DaggerTestAppComponent
import arun.com.chromer.di.app.TestAppModule

class ChromerTestApplication : Chromer() {

    override val appComponent: AppComponent by lazy {
        DaggerTestAppComponent.builder()
                .testAppModule(TestAppModule(this))
                .build()
    }

    override fun initFabric() {}
}