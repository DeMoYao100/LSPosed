package org.lsposed.manager;

import org.lsposed.lspd.models.Application;

interface IExternalModuleManager {
    boolean setModuleEnabled(String modulePackage, boolean enabled);
    boolean setModuleScope(String modulePackage, int moduleUserId, in List<Application> scope);
    boolean setModuleScopeByPackageNames(String modulePackage, int moduleUserId, in List<String> packageNames, int targetUserId);
}
