import 'package:flutter/foundation.dart';
import 'package:vault_client/services/api_client.dart';

/// Holds the current authentication state and exposes actions.
///
/// The token is stored in memory ONLY — never written to disk by this class.
/// On Android the token may optionally be stored in the Keystore via
/// SecureStorageService, but that layer is separate.
class AuthState extends ChangeNotifier {
  final ApiClient _api = ApiClient();

  String? _userId;
  bool _isAuthenticated = false;
  String? _error;

  bool get isAuthenticated => _isAuthenticated;
  String? get userId => _userId;
  String? get error => _error;

  /// The shared HTTP client that holds the active JWT in memory.
  /// Screens must use this instance — never create their own ApiClient().
  ApiClient get api => _api;

  /// Registers a new user. Returns the userId string on success.
  Future<String?> register(String masterPassword) async {
    _error = null;
    try {
      final id = await _api.register(masterPassword);
      return id;
    } catch (e) {
      _error = e.toString();
      notifyListeners();
      return null;
    }
  }

  /// Unlocks the vault with the master password.
  Future<bool> unlock(String userId, String masterPassword) async {
    _error = null;
    try {
      await _api.unlock(userId, masterPassword);
      _userId = userId;
      _isAuthenticated = true;
      notifyListeners();
      return true;
    } catch (e) {
      _error = _friendlyError(e.toString());
      notifyListeners();
      return false;
    }
  }

  /// Locks the vault: clears token from memory and notifies the server.
  Future<void> lock() async {
    try {
      await _api.lock();
    } catch (_) {
      // Best-effort — we still clear local state
    }
    _api.clearToken();
    _userId = null;
    _isAuthenticated = false;
    _error = null;
    notifyListeners();
  }

  String _friendlyError(String raw) {
    if (raw.contains('401') || raw.contains('credentials')) {
      return 'Invalid user ID or master password.';
    }
    if (raw.contains('SocketException') || raw.contains('Connection')) {
      return 'Cannot reach the server. Is it running?';
    }
    return 'An error occurred. Please try again.';
  }
}
