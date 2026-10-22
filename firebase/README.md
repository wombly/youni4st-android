# Username registry

Publish `firestore.rules` to the same Firebase project's **(default)** Cloud Firestore database as the Android app. The Android REST client authenticates with Firebase user ID tokens; the rules enforce ownership and atomic reservation. No Admin SDK credential or service-account key belongs in the APK.

Schema:

- `handles/{firebaseUid}`: `{ username: "wombly" }` — readable only by its owner.
- `usernames/{canonicalUsername}`: `{ uid: "firebaseUid" }` — authenticated single-document reads; no collection listing.

Android creates both records in one `documents:commit` with `exists: false`. A rename creates the new reservation, updates the owner pointer using its `updateTime`, and deletes the old reservation in the same commit. The rules reject incomplete operations, overwrites, impersonation and duplicate names.

## Local security tests

Requires Node.js 22+ and Java 21+. No Firebase login is needed. Only the demo project and local emulator are used.

```sh
npm install
npm test
```

The suite covers 11 scenarios, including concurrent duplicate registration and the exact REST commit shape used by Android. Production deployment is separate from these tests.

The application does not yet implement account deletion. When adding it, include a trusted backend cleanup of both records after verified account deletion; do not loosen client rules to permit unilateral removal of an active reservation.
