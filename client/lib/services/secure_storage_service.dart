import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Optional persistent storage for the username (NOT the token).
///
/// On Android, uses the Android Keystore. On Linux/Windows, uses the system
/// credential store when available.
///
/// SECURITY NOTE: The JWT token is NEVER stored here — it lives only in
/// ApiClient._token (in-memory). Only the username is persisted so the user
/// doesn't have to type it every time they unlock.
class SecureStorageService {
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(),
  );

  static const _keyUsername = 'vault_username';

  Future<void> saveUsername(String username) =>
      _storage.write(key: _keyUsername, value: username);

  Future<String?> loadUsername() =>
      _storage.read(key: _keyUsername);

  Future<void> clearUsername() =>
      _storage.delete(key: _keyUsername);
}
