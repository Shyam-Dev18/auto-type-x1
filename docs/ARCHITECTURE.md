# Architecture — AutoType X1

## Overview

AutoType X1 follows **Clean Architecture** with a strict unidirectional dependency rule:

```
presentation  →  domain  ←  data
                  ↑
                core
```

- `presentation` depends on `domain`
- `data` depends on `domain` (implements domain interfaces)
- `core` provides cross-cutting concerns (DI, service, utilities)
- `domain` has no Android dependencies — pure Kotlin

---

## Layer Descriptions

### `presentation/`
Jetpack Compose UI, ViewModels, and Navigation.

```
presentation/
├── about/          AboutScreen
├── adddevice/      AddDeviceScreen, AddDeviceViewModel, AddDeviceContract
├── home/           HomeScreen, HomeViewModel, HomeContract
├── navigation/     AppNavGraph, Routes
├── permissions/    PermissionsScreen
├── privacy/        PrivacyScreen
├── scripteditor/   ScriptEditorScreen, ScriptEditorViewModel, ScriptEditorContract
├── scripts/        ScriptsScreen, ScriptsViewModel, ScriptsContract
├── settings/       SettingsScreen, SettingsViewModel, SettingsContract
├── splash/         SplashScreen
├── theme/          Color, Theme, Type
└── typing/         TypingScreen, TypingViewModel, TypingContract
```

- All screens are pure Compose composables
- ViewModels expose `StateFlow<UiState>` and accept `UiEvent` one-shot events
- Navigation uses Compose Navigation with type-safe `Routes` constants

### `domain/`
Business logic and contracts. No Android framework dependencies.

```
domain/
├── model/          AppSettings, BluetoothAdapterState, ConnectionState,
│                   KnownDevice, ScannedDevice, Script, TypingProfile,
│                   TypingProfileSpec, TypingState
├── repository/     DeviceRepository, HidConnectionRepository,
│                   ScriptRepository, SettingsRepository  (interfaces)
├── typing/         HumanTypingEngine (interface), DefaultHumanTypingEngine,
│                   QwertyAdjacency, TimedKeyEvent
└── usecase/        BluetoothUseCases, DeviceUseCases, ScriptUseCases,
                    SettingsUseCases, TypingUseCases
```

### `data/`
Concrete implementations of domain repository interfaces.

```
data/
├── bluetooth/      BluetoothHidRepositoryImpl (HidConnectionRepository),
│                   HidKeyMapper, HidKeyStroke
├── local/          AppDatabase (Room), ScriptDao, ScriptEntity,
│                   KnownDevicesDataStore, SettingsDataStore
└── repository/     DeviceRepositoryImpl, ScriptRepositoryImpl,
                    SettingsRepositoryImpl
```

### `core/`
DI modules, foreground service, and utilities.

```
core/
├── di/             BluetoothModule, PersistenceModule, RepositoryModule
├── service/        TypingForegroundService
└── util/           PermissionHelper
```

---

## Key Data Flows

### User types a script (main flow)

```
TypingScreen (UI)
  → TypingViewModel.startTyping()
    → TypingUseCases.startTyping
      → DefaultHumanTypingEngine.typeText()
        → generates TimedKeyEvent sequence (with human timing)
        → BluetoothUseCases.sendCharacter(char)
          → BluetoothHidRepositoryImpl.sendCharacter(char)
            → HidKeyMapper.mapChar(char) → HidKeyStroke
            → BluetoothHidDevice.sendReport(host, reportId, bytes)
              → Bluetooth HID host (computer)
```

The foreground service (`TypingForegroundService`) hosts this coroutine, keeping it alive when the app is backgrounded.

### Script CRUD

```
ScriptsScreen / ScriptEditorScreen (UI)
  → ScriptsViewModel / ScriptEditorViewModel
    → ScriptUseCases (create / read / update / delete)
      → ScriptRepositoryImpl
        → ScriptDao (Room)
          → autotype_scripts.db (SQLite)
```

### Bluetooth device scan and connect

```
AddDeviceScreen (UI)
  → AddDeviceViewModel
    → BluetoothUseCases.startScan()
      → BluetoothHidRepositoryImpl.startScan()
        → BluetoothAdapter.startDiscovery()
        → BroadcastReceiver (ACTION_FOUND)
          → _scannedDevices StateFlow (device list updates)

AddDeviceScreen → connect(address)
  → BluetoothHidRepositoryImpl.connect(address)
    → BluetoothAdapter.getProfileProxy(HID_DEVICE)
      → BluetoothHidDevice.registerApp(sdp, qos, callback)
        → BluetoothHidDevice.connect(bondedDevice)
          → hidCallback.onConnectionStateChanged → ConnectionState.Connected
```

---

## Dependency Injection

Hilt is used for DI. All singletons are scoped to `SingletonComponent`:

- `AppDatabase` (Room) — singleton
- `ScriptDao` — derived from AppDatabase
- `KnownDevicesDataStore` — singleton
- `SettingsDataStore` — singleton
- `BluetoothHidRepositoryImpl` — singleton (holds Bluetooth state)
- Repository implementations — singleton

ViewModels use `@HiltViewModel` and receive use cases via constructor injection.

---

## Concurrency Model

- Bluetooth operations: `Dispatchers.IO` (custom `CoroutineScope` in `BluetoothHidRepositoryImpl`)
- Typing engine: `Dispatchers.IO` via `viewModelScope`
- Room: uses `Dispatchers.IO` automatically via `room-ktx`
- DataStore: Flow-based, collected on calling dispatcher
- UI state: `StateFlow` collected with `collectAsStateWithLifecycle` in Compose

---

## Local Storage

| Store | Technology | Contents |
|---|---|---|
| `autotype_scripts.db` | Room / SQLite | User scripts (name, content, timestamps) |
| `settings.preferences_pb` | DataStore Preferences | Theme, typing profile preferences |
| `known_devices.preferences_pb` | DataStore Preferences | Bluetooth device name + MAC address pairs |

No data leaves the device.
