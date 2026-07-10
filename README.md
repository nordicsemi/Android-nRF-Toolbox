# nRF Toolbox

The nRF Toolbox is a container app that stores your Nordic Semiconductor apps for Bluetooth Low Energy in one location.

It contains applications demonstrating standard Bluetooth LE profiles:
* **Cycling Speed and Cadence**
* **Running Speed and Cadence**
* **Heart Rate Monitor**
* **Blood Pressure Monitor**
* **Health Thermometer Monitor**
* **Glucose Monitor**
* **Continuous Glucose Monitor**
* **Universal Asynchronous Receiver/Transmitter (UART)**
* **Throughput**
* [**nRF Distance Measurement**](https://docs.nordicsemi.com/bundle/ncs-latest/page/nrfxlib/nrf_dm/README.html)
* **Blinky (LBS) Service**
* **Channel Sounding Service**

> [!Note]
> The Proximity profile is not included in this version of the app.
> If you need it, please download the previous [version](https://github.com/nordicsemi/Android-nRF-Toolbox/releases/tag/3.3.1).

### How to import to Android Studio

The production version of nRF Toolbox depends on
[Kotlin BLE Library](https://github.com/nordicsemi/Kotlin-BLE-Library) for Bluetooth LE connectivity, and [Nordic Android Common Library](https://github.com/nordicsemi/Android-Common-Libraries),
which contains utility classes such as Bluetooth LE permission, Bluetooth LE scanner, etc.

### BLE Profile Service

The `ProfileService` is responsible for managing Bluetooth Low Energy (BLE) connections in a centralized way.
It runs as a foreground service, maintaining connections to multiple devices, handling lifecycle events, and exposing device data to bound components (e.g., activities or fragments).

#### Overview
The service establishes and maintains BLE connections via the `CentralManager`. Connections are kept alive independently of Activity lifecycle. When the UI component unbinds from the service, the connections remain active, ensuring uninterrupted data flow.

All connected devices are tracked, and their states (connection, services, disconnections) are published using a `StateFlow`.

#### Key Features

- Single Service for All Devices: 
Instead of running a separate service per device, one `ProfileService` manages all active connections.

- Connection Lifecycle: Starts a connection when `onStartCommand` is triggered with a device address. Cleans up and disconnects gracefully when no devices remain connected. Publishes disconnection events with reasons for UI handling.

- Device State Management: 
 Keeps a map of devices and their `DeviceData` (address, state, services). Tracks missing services and notifies observers if required services are unavailable.

- Service Discovery & Management:
Discovers services after connection. New profiles can be added by implementing a `ServiceManager` subclass.

- Logging: 
Integrates with *nRF Logger* to provide per-device logs. Each device connection initializes its own logger instance.

### Nordic UART Service

The UART profile allows for fast prototyping of devices. The service itself is very simple, having
just 2 characteristics, one for sending data and one for receiving. A UART preset configuration can be created as a separate profile.
Each of them, when pressed, will send the stored command to the device. The UART profile is included in the profile list and is managed in the same way as other profiles.

> [!Note]
> The Device Firmware Update (DFU) feature has been removed from this application and is now offered in [nRF DFU](https://github.com/nordicsemi/Android-DFU-Library) (for devices with Legacy and Secure DFU from nRF5 SDK) or [nRF Connect Device Manager](https://github.com/nordicsemi/Android-nRF-Connect-Device-Manager) (for devices based on nRF Connect SDK or Zephyr).

### Dependencies

nRF Toolbox depends on [Kotlin BLE Library](https://github.com/nordicsemi/Kotlin-BLE-Library)
which may be cloned into the same root folder as this app. 

The nRF Toolbox also uses the [nRF Logger API library](https://github.com/NordicSemiconductor/nRF-Logger-API). 
This library allows the app to create log entries in
[nRF Logger](https://play.google.com/store/apps/details?id=no.nordicsemi.android.log).
Please, read the library documentation on GitHub for more information about the usage and permissions.

The [Kotlin-Utils-Library](https://github.com/nordicsemi/Kotlin-Util-Library.git) is used for different `ByteArray` operations during profile data parsing.

The graph in the HRM profile is created using the [MPAndroidChart v3.1.0](https://github.com/PhilJay/MPAndroidChart)
contribution based on the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0).

### Note
- Android 6.0 or newer is required. Android 5.0 was supported until version 4.1.4.
- Compatible with nRF5 devices running samples from the Nordic SDK and other devices implementing
  standard profiles.
- Development kits: https://www.nordicsemi.com/Software-and-tools/Development-Kits.
- The nRF Connect SDK documentation is available at https://docs.nordicsemi.com/bundle/ncs-latest/page/nrf/index.html.
