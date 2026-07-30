# Cordova FileChooser Plugin

Android-only file chooser plugin for Cordova.

## Requirements

- Cordova Android `14.x` or newer (verified with `14.x` and `15.x`)
- Node.js `>=20.5.0`
- JDK `17`

## Install with Cordova CLI
	$ cordova plugin add https://github.com/tgptom/cordova-plugin-filechooser.git

## API

```javascript
fileChooser.open(filter, successCallback, failureCallback); // with mime filter

fileChooser.open(successCallback, failureCallback); // without mime filter
```

### Filter (optional)

```javascript
{ "mime": "application/pdf" }  // text/plain, image/png, image/jpeg, audio/wav etc
```

The success callback gets the `content://` URI of the selected file.

```javascript
fileChooser.open(function(uri) {
  alert(uri);
});
```

The URI is provided by Android's Storage Access Framework. The plugin requests read access and attempts to persist URI permissions where supported. Some providers may not grant persistable access, so long-term availability is provider-dependent.

## Screenshot

![Screenshot](filechooser.png "Screenshot")

## Supported Platforms

- Android
