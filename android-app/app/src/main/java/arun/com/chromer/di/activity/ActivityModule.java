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

package arun.com.chromer.di.activity;

import android.app.Activity;

import androidx.lifecycle.LifecycleOwner;

import com.bumptech.glide.RequestManager;

import arun.com.chromer.di.scopes.PerActivity;
import arun.com.chromer.util.glide.GlideApp;
import arun.com.chromer.util.lifecycle.ActivityLifecycle;
import dagger.Module;
import dagger.Provides;

@Module
public class ActivityModule {

    @SuppressWarnings("CanBeFinal")
    private Activity activity;

    public ActivityModule(Activity activity) {
        this.activity = activity;
    }

    @Provides
    Activity activity() {
        return activity;
    }

    @Provides
    RequestManager glideRequests() {
        return GlideApp.with(activity);
    }

    @Provides
    @PerActivity
    @ActivityLifecycle
    LifecycleOwner owner(Activity activity) {
        return (LifecycleOwner) activity;
    }
}
