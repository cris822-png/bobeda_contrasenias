import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// Optional persistent storage for the userId (NOT the token).
///
/// On Android, uses the Android Keystore. On Linux/Windows, uses the system
/// credential store when available.
///
/// SECURITY NOTE: The JWT token is NEVER stored here — it lives only in
/// ApiClient._token (in-memory). Only the userId is persisted so the user
/// doesn't have to type it every time they unlock.
class SecureStorageService {
  static const _storage = FlutterSecureStorage(
    aOptions: AndroidOptions(encryptedSharedPreferences: true),
  );

  static const _keyUserId = 'vault_user_id';

  Future<void> saveUserId(String userId) =>
      _storage.write(key: _keyUserId, value: userId);

  Future<String?> loadUserId() =>
      _storage.read(key: _keyUserId);

  Future<void> clearUserId() =>
      _storage.delete(key: _keyUserId);
}
