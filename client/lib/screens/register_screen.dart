import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/auth_state.dart';

/// Registration screen — creates a new vault user.
/// On success, shows the new UUID and navigates back to the unlock screen.
class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  State<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _passwordCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();

  bool _loading = false;
  bool _showPassword = false;
  String? _newUserId;
  String? _error;

  @override
  void dispose() {
    _passwordCtrl.dispose();
    _confirmCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() { _loading = true; _error = null; });

    final auth = context.read<AuthState>();
    final userId = await auth.register(_passwordCtrl.text);
    if (!mounted) return;

    if (userId != null) {
      setState(() { _newUserId = userId; _loading = false; });
    } else {
      setState(() {
        _error = auth.error ?? 'Error al registrar';
        _loading = false;
      });
    }
  }

  /// Simple password strength (0–4)
  int _strength(String p) {
    if (p.isEmpty) return 0;
    int score = 0;
    if (p.length >= 12) score++;
    if (RegExp(r'[A-Z]').hasMatch(p)) score++;
    if (RegExp(r'[0-9]').hasMatch(p)) score++;
    if (RegExp(r'[!@#\$%^&*(),.?":{}|<>]').hasMatch(p)) score++;
    return score;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final primary = theme.colorScheme.primary;

    return Scaffold(
      appBar: AppBar(
        title: const Text('Crear cuenta'),
        backgroundColor: Colors.transparent,
        elevation: 0,
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(32),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: _newUserId != null
                ? _SuccessCard(userId: _newUserId!, onDone: () =>
                    Navigator.of(context).pop())
                : Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        Icon(Icons.add_moderator_rounded,
                            size: 56, color: primary),
                        const SizedBox(height: 24),
                        Text(
                          'Elige una contraseña maestra',
                          style: theme.textTheme.titleLarge?.copyWith(
                              fontWeight: FontWeight.bold, color: Colors.white),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          'Mínimo 12 caracteres. Esta contraseña NO se puede recuperar.',
                          style: theme.textTheme.bodySmall
                              ?.copyWith(color: const Color(0xFF94A3B8)),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 32),

                        // ─── Password ──────────────────────────────────
                        TextFormField(
                          controller: _passwordCtrl,
                          obscureText: !_showPassword,
                          onChanged: (_) => setState(() {}),
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
                          validator: (v) {
                            if (v == null || v.isEmpty) return 'Requerido';
                            if (v.length < 12) return 'Mínimo 12 caracteres';
                            return null;
                          },
                        ),
                        const SizedBox(height: 8),
                        _StrengthBar(score: _strength(_passwordCtrl.text)),
                        const SizedBox(height: 16),

                        // ─── Confirm ───────────────────────────────────
                        TextFormField(
                          controller: _confirmCtrl,
                          obscureText: !_showPassword,
                          decoration: const InputDecoration(
                            labelText: 'Confirmar contraseña',
                            prefixIcon: Icon(Icons.lock_outline),
                          ),
                          validator: (v) {
                            if (v != _passwordCtrl.text) {
                              return 'Las contraseñas no coinciden';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 24),

                        if (_error != null) ...[
                          Container(
                            padding: const EdgeInsets.all(12),
                            decoration: BoxDecoration(
                              color: Colors.red.withValues(alpha: 0.15),
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Text(_error!,
                                style: const TextStyle(
                                    color: Color(0xFFFCA5A5))),
                          ),
                          const SizedBox(height: 16),
                        ],

                        ElevatedButton(
                          onPressed: _loading ? null : _submit,
                          child: _loading
                              ? const SizedBox(
                                  width: 20, height: 20,
                                  child: CircularProgressIndicator(
                                      strokeWidth: 2, color: Colors.white),
                                )
                              : const Text('Crear cuenta',
                                  style: TextStyle(fontWeight: FontWeight.bold)),
                        ),
                      ],
                    ),
                  ),
          ),
        ),
      ),
    );
  }
}

// ─── Success card ────────────────────────────────────────────────────────────

class _SuccessCard extends StatelessWidget {
  final String userId;
  final VoidCallback onDone;
  const _SuccessCard({required this.userId, required this.onDone});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Icon(Icons.check_circle_rounded, size: 72, color: Color(0xFF34D399)),
        const SizedBox(height: 24),
        const Text('¡Cuenta creada!',
            style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Colors.white),
            textAlign: TextAlign.center),
        const SizedBox(height: 12),
        const Text(
          'Guarda tu ID de usuario en un lugar seguro. Lo necesitarás para desbloquear la bóveda.',
          textAlign: TextAlign.center,
          style: TextStyle(color: Color(0xFF94A3B8)),
        ),
        const SizedBox(height: 24),
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFF1A1D27),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: const Color(0xFF2D3748)),
          ),
          child: SelectableText(
            userId,
            style: const TextStyle(
                fontFamily: 'monospace', fontSize: 13, color: Color(0xFF6C63FF)),
            textAlign: TextAlign.center,
          ),
        ),
        const SizedBox(height: 32),
        ElevatedButton(
          onPressed: onDone,
          child: const Text('Ir al inicio de sesión',
              style: TextStyle(fontWeight: FontWeight.bold)),
        ),
      ],
    );
  }
}

// ─── Strength bar ────────────────────────────────────────────────────────────

class _StrengthBar extends StatelessWidget {
  final int score; // 0–4
  const _StrengthBar({required this.score});

  @override
  Widget build(BuildContext context) {
    final colors = [
      Colors.transparent,
      const Color(0xFFEF4444),
      const Color(0xFFF97316),
      const Color(0xFFEAB308),
      const Color(0xFF22C55E),
    ];
    final labels = ['', 'Muy débil', 'Débil', 'Media', 'Fuerte'];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: List.generate(4, (i) => Expanded(
            child: Container(
              height: 4,
              margin: const EdgeInsets.symmetric(horizontal: 2),
              decoration: BoxDecoration(
                color: i < score ? colors[score] : const Color(0xFF2D3748),
                borderRadius: BorderRadius.circular(4),
              ),
            ),
          )),
        ),
        if (score > 0) ...[
          const SizedBox(height: 4),
          Text(labels[score],
              style: TextStyle(color: colors[score], fontSize: 12)),
        ],
      ],
    );
  }
}
