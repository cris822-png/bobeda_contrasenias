import 'package:flutter/foundation.dart';
import 'package:vault_client/services/api_client.dart';

/// Holds the current authentication state and exposes actions.
///
/// The token is stored in memory ONLY — never written to disk by this class.
/// On Android the token may optionally be stored in the Keystore via
/// SecureStorageService, but that layer is separate.
class AuthState extends ChangeNotifier {
  final ApiClient _api = ApiClient();

  String? _username;
  bool _isAuthenticated = false;
  String? _error;

  bool get isAuthenticated => _isAuthenticated;
  String? get username => _username;
  String? get error => _error;

  /// The shared HTTP client that holds the active JWT in memory.
  /// Screens must use this instance — never create their own ApiClient().
  ApiClient get api => _api;

  /// Registers a new user. Returns an error message or null on success.
  Future<String?> register(String username, String masterPassword) async {
    _error = null;
    try {
      await _api.register(username, masterPassword);
      return null;
    } catch (e) {
      if (e.toString().contains('409') || e.toString().contains('conflict')) {
        _error = 'El nombre de usuario ya está en uso.';
      } else {
        _error = _friendlyError(e.toString());
      }
      notifyListeners();
      return _error;
    }
  }

  /// Unlocks the vault with the master password.
  Future<bool> unlock(String username, String masterPassword) async {
    _error = null;
    try {
      await _api.unlock(username, masterPassword);
      _username = username;
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
    _username = null;
    _isAuthenticated = false;
    _error = null;
    notifyListeners();
  }

  String _friendlyError(String raw) {
    if (raw.contains('401') || raw.contains('credentials') || raw.contains('Invalid credentials')) {
      return 'Usuario o contraseña incorrectos.';
    }
    if (raw.contains('SocketException') || raw.contains('Connection')) {
      return 'Cannot reach the server. Is it running?';
    }
    return 'An error occurred. Please try again.';
  }
}
