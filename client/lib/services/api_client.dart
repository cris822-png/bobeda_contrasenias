import 'package:dio/dio.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import '../models/vault_entry.dart';

/// HTTP client wrapping the Java backend REST API.
///
/// SECURITY INVARIANTS:
///   - The JWT token is stored in memory only (_token field).
///   - clearToken() must be called on lock/logout.
///   - The client is a singleton within AuthState.
class ApiClient {
  late final Dio _dio;
  String? _token;

  ApiClient() {
    final baseUrl = dotenv.env['API_BASE_URL'] ?? 'http://localhost:8443';
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      headers: {'Content-Type': 'application/json'},
    ));

    // Inject JWT on every request that has a token
    _dio.interceptors.add(InterceptorsWrapper(
      onRequest: (options, handler) {
        if (_token != null) {
          options.headers['Authorization'] = 'Bearer $_token';
        }
        handler.next(options);
      },
      onError: (DioException e, handler) {
        handler.next(e);
      },
    ));
  }

  // ─── Auth ────────────────────────────────────────────────────────────────

  /// Registers a new user. Returns the userId UUID string.
  Future<String> register(String masterPassword) async {
    final resp = await _dio.post('/auth/register', data: {
      'masterPassword': masterPassword,
    });
    return resp.data['userId'] as String;
  }

  /// Unlocks the vault. Stores the JWT token in memory on success.
  Future<void> unlock(String userId, String masterPassword) async {
    final resp = await _dio.post('/auth/unlock', data: {
      'userId': userId,
      'masterPassword': masterPassword,
    });
    _token = resp.data['token'] as String;
  }

  /// Calls POST /auth/lock to clear the DEK on the server, then clears the local token.
  Future<void> lock() async {
    await _dio.post('/auth/lock');
  }

  /// Clears the in-memory JWT token. Always call on logout.
  void clearToken() {
    _token = null;
  }

  // ─── Vault entries ───────────────────────────────────────────────────────

  Future<List<VaultEntry>> listEntries() async {
    final resp = await _dio.get('/vault/entries');
    final list = resp.data as List<dynamic>;
    return list.map((e) => VaultEntry.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<VaultEntry> createEntry(String title, String? username, String password) async {
    final resp = await _dio.post('/vault/entries', data: {
      'title': title,
      'username': username,
      'password': password,
    });
    return VaultEntry.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<VaultEntry> updateEntry(
      String id, String title, String? username, String password) async {
    final resp = await _dio.put('/vault/entries/$id', data: {
      'title': title,
      'username': username,
      'password': password,
    });
    return VaultEntry.fromJson(resp.data as Map<String, dynamic>);
  }

  Future<void> deleteEntry(String id) async {
    await _dio.delete('/vault/entries/$id');
  }
}
