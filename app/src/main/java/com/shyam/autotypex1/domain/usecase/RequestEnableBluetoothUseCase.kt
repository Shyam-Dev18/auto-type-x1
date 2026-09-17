package com.shyam.autotypex1.domain.usecase

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import javax.inject.Inject

/**
 * Creates the system intent to enable Bluetooth.
 *
 * This is the **only** mechanism to enable Bluetooth per the project plan:
 * - Uses `ACTION_REQUEST_ENABLE` (system dialog)
 * - No Settings redirect
 * - No repeated dialog loop
 *
 * Returns an [Intent] that the Activity/ViewModel should launch via
 * `ActivityResultLauncher`. The domain layer does not hold Activity references.
 *
 * Note: This use case necessarily imports an Android class ([BluetoothAdapter])
 * because the intent action constant lives there. This is an acceptable compromise —
 * the use case itself does no Android platform work; it just builds an Intent.
 */
class RequestEnableBluetoothUseCase @Inject constructor() {

    /**
     * @return An intent for `ACTION_REQUEST_ENABLE`, ready to be launched.
     */
    operator fun invoke(): Intent {
        return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
    }
}
