/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2026 LSPosed Contributors
 */
package org.lsposed.manager.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import org.lsposed.lspd.models.Application;
import org.lsposed.manager.ConfigManager;
import org.lsposed.manager.IExternalModuleManager;
import org.lsposed.manager.adapters.ScopeAdapter;
import org.lsposed.manager.util.ModuleUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ExternalModuleManagerService extends Service {
    private static final String TAG = "ExternalModuleManager";

    private final IExternalModuleManager.Stub binder = new IExternalModuleManager.Stub() {
        @Override
        public boolean setModuleEnabled(String modulePackage, boolean enabled) {
            if (!isReady()) return false;
            return ModuleUtil.getInstance().setModuleEnabled(modulePackage, enabled);
        }

        @Override
        public boolean setModuleScope(String modulePackage, int moduleUserId, List<Application> scope) {
            if (!isReady()) return false;
            return updateScope(modulePackage, moduleUserId, scope);
        }

        @Override
        public boolean setModuleScopeByPackageNames(String modulePackage, int moduleUserId, List<String> packageNames, int targetUserId) {
            if (!isReady()) return false;
            if (packageNames == null) return false;
            Set<ScopeAdapter.ApplicationWithEquals> applications = new HashSet<>();
            for (String packageName : packageNames) {
                if (packageName == null || packageName.isEmpty()) continue;
                applications.add(new ScopeAdapter.ApplicationWithEquals(packageName, targetUserId));
            }
            return updateScope(modulePackage, moduleUserId, applications);
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private boolean isReady() {
        if (ConfigManager.isBinderAlive()) return true;
        Log.w(TAG, "Manager binder is not ready");
        return false;
    }

    private boolean updateScope(String modulePackage, int moduleUserId, List<Application> scope) {
        if (modulePackage == null || scope == null) return false;
        Set<ScopeAdapter.ApplicationWithEquals> applications = new HashSet<>();
        for (Application application : scope) {
            if (application == null || application.packageName == null) continue;
            applications.add(new ScopeAdapter.ApplicationWithEquals(application));
        }
        return updateScope(modulePackage, moduleUserId, applications);
    }

    private boolean updateScope(String modulePackage, int moduleUserId, Set<ScopeAdapter.ApplicationWithEquals> applications) {
        boolean legacy = false;
        var moduleUtil = ModuleUtil.getInstance();
        ModuleUtil.InstalledModule module = moduleUtil.getModule(modulePackage, moduleUserId);
        if (module == null) {
            module = moduleUtil.reloadSingleModule(modulePackage, moduleUserId);
        }
        if (module != null) {
            legacy = module.legacy;
        }
        return ConfigManager.setModuleScope(modulePackage, legacy, applications);
    }
}
