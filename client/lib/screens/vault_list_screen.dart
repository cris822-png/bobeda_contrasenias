import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/auth_state.dart';
import '../models/vault_entry.dart';
import '../services/api_client.dart';
import '../widgets/entry_card.dart';
import 'entry_form_screen.dart';

/// Main screen showing the list of vault entries.
/// Supports search, pull-to-refresh, add, edit, and delete.
class VaultListScreen extends StatefulWidget {
  const VaultListScreen({super.key});

  @override
  State<VaultListScreen> createState() => _VaultListScreenState();
}

class _VaultListScreenState extends State<VaultListScreen> {
  final _api = ApiClient();
  final _searchCtrl = TextEditingController();

  List<VaultEntry> _entries = [];
  List<VaultEntry> _filtered = [];
  bool _loading = true;
  String? _error;

  @override
  void initState() {
    super.initState();
    _searchCtrl.addListener(_applyFilter);
    _loadEntries();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadEntries() async {
    setState(() { _loading = true; _error = null; });
    try {
      final entries = await _api.listEntries();
      if (!mounted) return;
      setState(() {
        _entries = entries;
        _applyFilter();
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _error = 'Error al cargar entradas: $e';
        _loading = false;
      });
    }
  }

  void _applyFilter() {
    final q = _searchCtrl.text.toLowerCase().trim();
    setState(() {
      _filtered = q.isEmpty
          ? List.of(_entries)
          : _entries
              .where((e) =>
                  e.title.toLowerCase().contains(q) ||
                  (e.username?.toLowerCase().contains(q) ?? false))
              .toList();
    });
  }

  Future<void> _deleteEntry(VaultEntry entry) async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Eliminar entrada'),
        content: Text('¿Eliminar "${entry.title}"? Esta acción no se puede deshacer.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx, false),
              child: const Text('Cancelar')),
          TextButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Eliminar', style: TextStyle(color: Color(0xFFEF4444))),
          ),
        ],
      ),
    );
    if (confirmed != true || !mounted) return;
    try {
      await _api.deleteEntry(entry.id);
      await _loadEntries();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error: $e'), backgroundColor: Colors.red.shade900),
      );
    }
  }

  Future<void> _openForm({VaultEntry? entry}) async {
    final result = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => EntryFormScreen(
          api: _api,
          existing: entry,
        ),
      ),
    );
    if (result == true) await _loadEntries();
  }

  Future<void> _lock() async {
    // Re-use the same ApiClient instance to call /auth/lock
    await context.read<AuthState>().lock();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final primary = theme.colorScheme.primary;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: const Text(
          'Mi Bóveda',
          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
        ),
        actions: [
          IconButton(
            tooltip: 'Bloquear bóveda',
            icon: const Icon(Icons.lock_rounded, color: Color(0xFF94A3B8)),
            onPressed: _lock,
          ),
        ],
      ),

      // ─── FAB ───────────────────────────────────────────────────────────
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _openForm(),
        icon: const Icon(Icons.add),
        label: const Text('Nueva entrada'),
        backgroundColor: primary,
        foregroundColor: Colors.white,
      ),

      body: Column(
        children: [
          // ─── Search bar ───────────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: TextField(
              controller: _searchCtrl,
              decoration: InputDecoration(
                hintText: 'Buscar por título o usuario…',
                hintStyle: const TextStyle(color: Color(0xFF64748B)),
                prefixIcon: const Icon(Icons.search, color: Color(0xFF64748B)),
                suffixIcon: _searchCtrl.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear, color: Color(0xFF64748B)),
                        onPressed: () => _searchCtrl.clear(),
                      )
                    : null,
              ),
            ),
          ),

          // ─── Content ──────────────────────────────────────────────────
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.error_outline, color: Color(0xFFEF4444), size: 48),
            const SizedBox(height: 12),
            Text(_error!, textAlign: TextAlign.center,
                style: const TextStyle(color: Color(0xFF94A3B8))),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _loadEntries,
              icon: const Icon(Icons.refresh),
              label: const Text('Reintentar'),
            ),
          ],
        ),
      );
    }

    if (_filtered.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.shield_outlined, size: 64,
                color: Theme.of(context).colorScheme.primary.withValues(alpha: 0.4)),
            const SizedBox(height: 16),
            Text(
              _searchCtrl.text.isNotEmpty
                  ? 'Sin resultados para "${_searchCtrl.text}"'
                  : 'Tu bóveda está vacía.\nPulsa + para añadir una entrada.',
              textAlign: TextAlign.center,
              style: const TextStyle(color: Color(0xFF94A3B8)),
            ),
          ],
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: _loadEntries,
      child: ListView.builder(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 100),
        itemCount: _filtered.length,
        itemBuilder: (ctx, i) {
          final entry = _filtered[i];
          return EntryCard(
            entry: entry,
            onEdit: () => _openForm(entry: entry),
            onDelete: () => _deleteEntry(entry),
          );
        },
      ),
    );
  }
}
