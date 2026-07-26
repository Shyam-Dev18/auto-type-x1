# ─── AutoType X1 — ProGuard / R8 Rules ────────────────────────────────────────

# Keep source file names and line numbers in release stack traces for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Note: Hilt, Room, Compose, and Kotlin Coroutines provide their own consumer
# ProGuard rules automatically bundled in their respective AAR libraries.

# ─── Bluetooth HID Framework Callbacks ──────────────────────────────────────
# System framework invokes BluetoothHidDevice.Callback methods via IPC/reflection.
# Keep callback classes and methods to prevent R8 from stripping framework overrides.
-keep class * extends android.bluetooth.BluetoothHidDevice$Callback {
    public <methods>;
}