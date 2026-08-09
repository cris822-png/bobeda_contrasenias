import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/auth_state.dart';
import '../services/secure_storage_service.dart';
import 'register_screen.dart';

/// The lock/unlock screen — first screen the user sees.
/// Collects the user ID and master password, then calls AuthState.unlock().
class UnlockScreen extends StatefulWidget {
  const UnlockScreen({super.key});

  @override
  State<UnlockScreen> createState() => _UnlockScreenState();
}

class _UnlockScreenState extends State<UnlockScreen>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _userIdCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _storage = SecureStorageService();

  bool _loading = false;
  bool _showPassword = false;

  late final AnimationController _fadeCtrl;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _fadeCtrl = AnimationController(
        vsync: this, duration: const Duration(milliseconds: 600));
    _fadeAnim = CurvedAnimation(parent: _fadeCtrl, curve: Curves.easeOut);
    _fadeCtrl.forward();
    _loadSavedUserId();
  }

  Future<void> _loadSavedUserId() async {
    final saved = await _storage.loadUserId();
    if (saved != null && mounted) {
      _userIdCtrl.text = saved;
    }
  }

  @override
  void dispose() {
    _fadeCtrl.dispose();
    _userIdCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);

    final auth = context.read<AuthState>();
    final success = await auth.unlock(
      _userIdCtrl.text.trim(),
      _passwordCtrl.text,
    );

    if (!mounted) return;
    setState(() => _loading = false);

    if (success) {
      await _storage.saveUserId(_userIdCtrl.text.trim());
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final error = context.watch<AuthState>().error;

    return Scaffold(
      body: Center(
        child: FadeTransition(
          opacity: _fadeAnim,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(32),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Form(
                key: _formKey,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // ─── Logo / Title ──────────────────────────────────
                    Icon(Icons.lock_rounded,
                        size: 64, color: theme.colorScheme.primary),
                    const SizedBox(height: 24),
                    Text(
                      'Bóveda de Contraseñas',
                      style: theme.textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Introduce tu ID de usuario y contraseña maestra',
                      style: theme.textTheme.bodyMedium?.copyWith(
                          color: const Color(0xFF94A3B8)),
                      textAlign: TextAlign.center,
                    ),
                    const SizedBox(height: 40),

                    // ─── User ID ────────────────────────────────────────
                    TextFormField(
                      controller: _userIdCtrl,
                      decoration: const InputDecoration(
                        labelText: 'ID de usuario (UUID)',
                        prefixIcon: Icon(Icons.person_outline),
                      ),
                      validator: (v) =>
                          (v == null || v.trim().isEmpty) ? 'Requerido' : null,
                    ),
                    const SizedBox(height: 16),

                    // ─── Master password ────────────────────────────────
                    TextFormField(
                      controller: _passwordCtrl,
                      obscureText: !_showPassword,
                      decoration: InputDecoration(
                        labelText: 'Contraseña maestra',
                        prefixIcon: const Icon(Icons.key_outlined),
                        suffixIcon: IconButton(
                          icon: Icon(_showPassword
                              ? Icons.visibility_off
                              : Icons.visibility),
                          onPressed: () =>
                              setState(() => _showPassword = !_showPassword),
                        ),
                      ),
                      validator: (v) =>
                          (v == null || v.isEmpty) ? 'Requerido' : null,
                      onFieldSubmitted: (_) => _submit(),
                    ),
                    const SizedBox(height: 24),

                    // ─── Error ──────────────────────────────────────────
                    if (error != null) ...[
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.red.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: Colors.red.shade800),
                        ),
                        child: Text(error,
                            style: const TextStyle(color: Color(0xFFFCA5A5))),
                      ),
                      const SizedBox(height: 16),
                    ],

                    // ─── Unlock button ──────────────────────────────────
                    ElevatedButton(
                      onPressed: _loading ? null : _submit,
                      child: _loading
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white),
                            )
                          : const Text('Desbloquear',
                              style: TextStyle(fontWeight: FontWeight.bold)),
                    ),
                    const SizedBox(height: 16),

                    // ─── Register link ──────────────────────────────────
                    TextButton(
                      onPressed: () => Navigator.of(context).push(
                        MaterialPageRoute(
                            builder: (_) => const RegisterScreen()),
                      ),
                      child: const Text('¿Primer uso? Crear cuenta'),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
