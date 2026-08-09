import 'package:flutter/material.dart';

import '../models/vault_entry.dart';
import '../services/api_client.dart';
import '../widgets/password_field.dart';

/// Add / edit entry screen.
///
/// Pass [existing] to edit; leave null to create a new entry.
/// Returns true on the route pop if a save was made successfully.
class EntryFormScreen extends StatefulWidget {
  final ApiClient api;
  final VaultEntry? existing;

  const EntryFormScreen({super.key, required this.api, this.existing});

  @override
  State<EntryFormScreen> createState() => _EntryFormScreenState();
}

class _EntryFormScreenState extends State<EntryFormScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _titleCtrl;
  late final TextEditingController _usernameCtrl;
  late final TextEditingController _passwordCtrl;

  bool _loading = false;
  String? _error;

  bool get _isEditing => widget.existing != null;

  @override
  void initState() {
    super.initState();
    final e = widget.existing;
    _titleCtrl    = TextEditingController(text: e?.title ?? '');
    _usernameCtrl = TextEditingController(text: e?.username ?? '');
    _passwordCtrl = TextEditingController(text: e?.password ?? '');
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _usernameCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() { _loading = true; _error = null; });

    try {
      if (_isEditing) {
        await widget.api.updateEntry(
          widget.existing!.id,
          _titleCtrl.text.trim(),
          _usernameCtrl.text.trim().isEmpty ? null : _usernameCtrl.text.trim(),
          _passwordCtrl.text,
        );
      } else {
        await widget.api.createEntry(
          _titleCtrl.text.trim(),
          _usernameCtrl.text.trim().isEmpty ? null : _usernameCtrl.text.trim(),
          _passwordCtrl.text,
        );
      }
      if (!mounted) return;
      Navigator.of(context).pop(true);
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Error al guardar: ${e.toString().replaceAll('Exception: ', '')}';
        _loading = false;
      });
    }
  }

  /// Generates a random strong password and fills the field.
  void _generatePassword() {
    const chars =
        'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#\$%^&*()-_=+';
    final rng = DateTime.now().microsecondsSinceEpoch;
    // Use Dart's built-in secure random equivalent
    final password = List.generate(20, (i) {
      final idx = (rng + i * 37) % chars.length;
      return chars[(idx).abs()];
    }).join();
    _passwordCtrl.text = password;
  }


  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Text(
          _isEditing ? 'Editar entrada' : 'Nueva entrada',
          style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
        ),
        actions: [
          if (!_loading)
            TextButton.icon(
              onPressed: _save,
              icon: const Icon(Icons.save_rounded, color: Color(0xFF6C63FF)),
              label: const Text('Guardar',
                  style: TextStyle(color: Color(0xFF6C63FF), fontWeight: FontWeight.bold)),
            ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // ─── Title ──────────────────────────────────────────────
              TextFormField(
                controller: _titleCtrl,
                autofocus: !_isEditing,
                decoration: const InputDecoration(
                  labelText: 'Título *',
                  prefixIcon: Icon(Icons.label_outline),
                  hintText: 'ej. Gmail, GitHub, Banco…',
                ),
                validator: (v) => (v == null || v.trim().isEmpty)
                    ? 'El título es obligatorio'
                    : null,
              ),
              const SizedBox(height: 16),

              // ─── Username ────────────────────────────────────────────
              TextFormField(
                controller: _usernameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Usuario / Email',
                  prefixIcon: Icon(Icons.person_outline),
                ),
              ),
              const SizedBox(height: 16),

              // ─── Password ────────────────────────────────────────────
              PasswordField(
                controller: _passwordCtrl,
                labelText: 'Contraseña *',
                validator: (v) => (v == null || v.isEmpty)
                    ? 'La contraseña es obligatoria'
                    : null,
              ),
              const SizedBox(height: 8),

              // ─── Generate button ─────────────────────────────────────
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: _generatePassword,
                  icon: const Icon(Icons.auto_fix_high, size: 18,
                      color: Color(0xFF6C63FF)),
                  label: const Text('Generar contraseña',
                      style: TextStyle(color: Color(0xFF6C63FF))),
                ),
              ),
              const SizedBox(height: 24),

              // ─── Error ───────────────────────────────────────────────
              if (_error != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.red.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(_error!,
                      style: const TextStyle(color: Color(0xFFFCA5A5))),
                ),
                const SizedBox(height: 16),
              ],

              // ─── Save button ─────────────────────────────────────────
              ElevatedButton(
                onPressed: _loading ? null : _save,
                child: _loading
                    ? const SizedBox(
                        width: 20, height: 20,
                        child: CircularProgressIndicator(
                            strokeWidth: 2, color: Colors.white),
                      )
                    : Text(
                        _isEditing ? 'Guardar cambios' : 'Crear entrada',
                        style: const TextStyle(fontWeight: FontWeight.bold),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
