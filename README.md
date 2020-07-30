Frekis SDK is android library which will help you to connect with the frekis account and manage your assets with in own app.

## Installation
Frekis sdk latest version is available on JitPackAdd below link in Project Level build.gradle file.
```bash
   allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Include Frekis SDK dependency in to your app build.gradle file
```bash
    dependencies {
        implementation 'com.github.frekisSDK:android-sdk:1.0.15'
    }
```

## Usage

To **initialize** the sdk, Use below code and setListeners to receive the callback.
```java
//  Managed this instance class as a singleton class for further usage.
//  Recommended usages is to keep this instance in application class.
Frekis instance = Frekis.getInstance(context)

//  Set session connection listener
instance.setSessionConnectionListener(this);
//  Set BleConnection Listener
instance.setBleConnectionListener(this);

//Init the sdk with your token
instance.init(token /* Replace your token here, Contact support to generate your app token*/)
```

**SessionConnectionListener** will invoked when client is successfully connected with Frekis SDK or when there is some error.
```java
public interface SessionConnectionListener {

   void onSessionConnectionSuccess();

   void onSessionConnectionError(int code, String error_message);
}
```

**BleConnectionListener** is for status update with ble devices, In Frekis they are know for asset.
```java
public interface BleListener {

  void onBleStatusUpdate(String status);

  void onBleConnected();

  void onBleConnectionError(int code, String message);
}
```

**Unlock** and **lock** the asset - Make sure the frekis sdk is connected and asset is connected before calling this method.

Note, Client has to manually detect the status of lock and call this method once user is manually locking the device.
```java
frekis.unlockAsset(lock_id, location, listener);

frekis.lockAsset(lock_id, location, listener);
```
