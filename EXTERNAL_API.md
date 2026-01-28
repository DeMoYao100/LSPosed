# External Module Manager API (LSPosed Manager)

This document describes how a third-party app can call the LSPosed Manager external API
to enable a module and select target packages for injection.

## Overview

LSPosed Manager exports a bound Service protected by a signature permission:

- **Service action**: `org.lsposed.manager.action.BIND_EXTERNAL_MANAGER`
- **AIDL interface**: `org.lsposed.manager.IExternalModuleManager`
- **Permission**: `org.lsposed.manager.permission.MANAGE_MODULES` (signature)

Only apps signed with the same key as the Manager can bind, unless you change the
permission protection level in the Manager app.

## Required AIDL files in your app

Copy these AIDL files into your app project under `src/main/aidl/`:

- `app/src/main/aidl/org/lsposed/manager/IExternalModuleManager.aidl`
- `services/manager-service/src/main/aidl/org/lsposed/lspd/models/Application.aidl`

Your app package should have the same AIDL package paths:

```
src/main/aidl/
  org/lsposed/manager/IExternalModuleManager.aidl
  org/lsposed/lspd/models/Application.aidl
```

## Manifest changes in your app

Declare the permission (use the actual Manager package if customized):

```
<uses-permission android:name="org.lsposed.manager.permission.MANAGE_MODULES" />
```

## Binding to the service

Use an explicit package to bind to the Manager app:

```
Intent intent = new Intent("org.lsposed.manager.action.BIND_EXTERNAL_MANAGER");
intent.setPackage("org.lsposed.manager"); // Manager app package name
bindService(intent, connection, Context.BIND_AUTO_CREATE);
```

If you use a custom Manager package name, replace `org.lsposed.manager` accordingly.

## API methods

```
boolean setModuleEnabled(String modulePackage, boolean enabled);
boolean setModuleScope(String modulePackage, int moduleUserId, List<Application> scope);
boolean setModuleScopeByPackageNames(String modulePackage, int moduleUserId,
                                     List<String> packageNames, int targetUserId);
```

Notes:

- `modulePackage`: the module app package name.
- `moduleUserId`: the user id where the module is installed (use `0` for owner).
- `targetUserId`: the user id of target apps (use `0` for owner).
- `setModuleScope` also enables the module at daemon level.

## Java example

```
public class LsposedClient {
    private IExternalModuleManager manager;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            manager = IExternalModuleManager.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            manager = null;
        }
    };

    public void bind(Context context) {
        Intent intent = new Intent("org.lsposed.manager.action.BIND_EXTERNAL_MANAGER");
        intent.setPackage("org.lsposed.manager");
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    public boolean enableAndScope(String modulePkg, List<String> targetPkgs) throws RemoteException {
        if (manager == null) return false;
        int moduleUserId = 0;
        int targetUserId = 0;
        boolean enabled = manager.setModuleEnabled(modulePkg, true);
        boolean scoped = manager.setModuleScopeByPackageNames(modulePkg, moduleUserId, targetPkgs, targetUserId);
        return enabled && scoped;
    }
}
```

## Kotlin example

```
class LsposedClient(private val context: Context) {
    private var manager: IExternalModuleManager? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            manager = IExternalModuleManager.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            manager = null
        }
    }

    fun bind() {
        val intent = Intent("org.lsposed.manager.action.BIND_EXTERNAL_MANAGER").apply {
            setPackage("org.lsposed.manager")
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun enableAndScope(modulePkg: String, targetPkgs: List<String>): Boolean {
        val moduleUserId = 0
        val targetUserId = 0
        val api = manager ?: return false
        val enabled = api.setModuleEnabled(modulePkg, true)
        val scoped = api.setModuleScopeByPackageNames(modulePkg, moduleUserId, targetPkgs, targetUserId)
        return enabled && scoped
    }
}
```

## Troubleshooting

- **Binding fails**: check the Manager app package name and permission.
- **SecurityException**: your app is not signed with the same key as the Manager.
- **Returns false**: Manager binder not ready or module not found; open Manager once.
