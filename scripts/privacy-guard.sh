#!/usr/bin/env bash
# Fails if the app source picks up an API or setting that would leak user data or weaken
# transport security. Run by the pre-commit hook and by CI. Comment lines are ignored.
set -euo pipefail
cd "$(dirname "$0")/.."

src=composeApp/src
shipped=("$src/commonMain" "$src/androidMain" "$src/iosMain" iosApp/iosApp)
failed=0

# forbid <description> <extended regex> <path>...
forbid() {
  local description=$1 pattern=$2
  shift 2
  local hits
  hits=$(grep -rnE --include='*.kt' --include='*.swift' --include='*.xml' --include='*.plist' "$pattern" "$@" 2>/dev/null \
    | grep -vE '^[^:]+:[0-9]+:[[:space:]]*(//|\*|/\*|<!--)' || true)
  if [[ -n $hits ]]; then
    echo "privacy-guard: $description" >&2
    echo "$hits" | sed 's/^/  /' >&2
    failed=1
  fi
}

forbid "The user-assigned device name must not be shared (DeviceName returns the model)" \
  'DEVICE_NAME|"bluetooth_name"|"device_name"|UIDevice\.current(Device)?\.name\b' "${shipped[@]}"
forbid "Hardware, vendor and advertising identifiers are tracking identifiers" \
  'ANDROID_ID|AdvertisingIdClient|getImei|getDeviceId|getSerial\(|Build\.SERIAL|identifierForVendor|ASIdentifierManager|advertisingIdentifier' "${shipped[@]}"
forbid "Logging can leak player names and photos into device logs" \
  '(android\.util\.Log\b|\bLog\.[vdiwe]\(|println\(|printStackTrace\(|NSLog\(|(^|[^A-Za-z_.])(print|debugPrint)\()' "${shipped[@]}"
forbid "Cleartext traffic and ATS exceptions weaken transport security" \
  'usesCleartextTraffic="true"|cleartextTrafficPermitted="true"|NSAllowsArbitraryLoads|NSExceptionAllowsInsecureHTTPLoads' "${shipped[@]}"
forbid "Plain-HTTP URLs send data in cleartext" \
  '"http://' "$src/commonMain/kotlin" "$src/androidMain/kotlin" "$src/iosMain/kotlin"
forbid "World-readable/writable files expose app data to other apps" \
  'MODE_WORLD_READABLE|MODE_WORLD_WRITEABLE' "$src/androidMain"
forbid "Photos and app data must stay in app-private storage" \
  'getExternalStorage|getExternalFilesDir|getExternalCacheDir|MediaStore\.Images\.Media\.insert' "$src/androidMain"

manifest=iosApp/iosApp/PrivacyInfo.xcprivacy
if ! python3 - "$manifest" <<'EOF'
import plistlib, sys
path = sys.argv[1]
try:
    with open(path, "rb") as f:
        manifest = plistlib.load(f)
except (OSError, plistlib.InvalidFileException) as error:
    sys.exit(f"privacy-guard: {path} is missing or not a valid plist ({error})")
if manifest.get("NSPrivacyTracking") is not False:
    sys.exit(f"privacy-guard: {path} must declare NSPrivacyTracking = false")
if not manifest.get("NSPrivacyAccessedAPITypes"):
    sys.exit(f"privacy-guard: {path} must declare the required-reason APIs the app uses")
EOF
then
  failed=1
fi

if [[ $failed -ne 0 ]]; then
  exit 1
fi
echo "privacy-guard: OK"
