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

package arun.com.chromer.shared.base;

import androidx.annotation.NonNull;

import arun.com.chromer.di.activity.ActivityComponent;

/**
 * Created by arunk on 12-11-2017.
 * Marker interface to define a contract to let {@code {@link androidx.core.app.Fragment}} retrieve
 * the {@link ActivityComponent} without explicitly depending on any sub class of {@link android.app.Activity}
 */
public interface ProvidesActivityComponent {
    /**
     * The Activity component for which the fragment component will be sub component of.
     *
     * @return Instantiated {@link ActivityComponent}
     */
    @NonNull
    ActivityComponent getActivityComponent();

    /**
     * Delegates inject calls to component definitions.
     *
     * @param activityComponent Current component.
     */
    void inject(@NonNull ActivityComponent activityComponent);
}
