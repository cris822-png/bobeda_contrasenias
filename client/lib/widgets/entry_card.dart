import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../models/vault_entry.dart';

/// Card displaying a single vault entry.
/// Supports reveal/hide password and copy-to-clipboard.
class EntryCard extends StatefulWidget {
  final VaultEntry entry;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const EntryCard({
    super.key,
    required this.entry,
    required this.onEdit,
    required this.onDelete,
  });

  @override
  State<EntryCard> createState() => _EntryCardState();
}

class _EntryCardState extends State<EntryCard>
    with SingleTickerProviderStateMixin {
  bool _passwordVisible = false;
  bool _copied = false;

  late final AnimationController _slideCtrl;
  late final Animation<Offset> _slideAnim;

  @override
  void initState() {
    super.initState();
    _slideCtrl = AnimationController(
        vsync: this, duration: const Duration(milliseconds: 300));
    _slideAnim = Tween<Offset>(
      begin: const Offset(0, 0.1),
      end: Offset.zero,
    ).animate(CurvedAnimation(parent: _slideCtrl, curve: Curves.easeOut));
    _slideCtrl.forward();
  }

  @override
  void dispose() {
    _slideCtrl.dispose();
    super.dispose();
  }

  Future<void> _copyPassword() async {
    await Clipboard.setData(ClipboardData(text: widget.entry.password));
    setState(() => _copied = true);
    await Future.delayed(const Duration(seconds: 2));
    if (mounted) setState(() => _copied = false);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final primary = theme.colorScheme.primary;

    return SlideTransition(
      position: _slideAnim,
      child: Card(
        margin: const EdgeInsets.only(bottom: 12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // ─── Header row ─────────────────────────────────────────
              Row(
                children: [
                  // Site initial avatar
                  CircleAvatar(
                    radius: 20,
                    backgroundColor: primary.withValues(alpha: 0.15),
                    child: Text(
                      widget.entry.title.isNotEmpty
                          ? widget.entry.title[0].toUpperCase()
                          : '?',
                      style: TextStyle(
                          color: primary, fontWeight: FontWeight.bold),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.entry.title,
                          style: const TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 16,
                              color: Colors.white),
                        ),
                        if (widget.entry.username != null) ...[
                          const SizedBox(height: 2),
                          Text(
                            widget.entry.username!,
                            style: const TextStyle(
                                color: Color(0xFF94A3B8), fontSize: 13),
                          ),
                        ],
                      ],
                    ),
                  ),
                  // Actions
                  IconButton(
                    tooltip: 'Editar',
                    icon: const Icon(Icons.edit_outlined,
                        size: 20, color: Color(0xFF94A3B8)),
                    onPressed: widget.onEdit,
                  ),
                  IconButton(
                    tooltip: 'Eliminar',
                    icon: const Icon(Icons.delete_outline,
                        size: 20, color: Color(0xFFEF4444)),
                    onPressed: widget.onDelete,
                  ),
                ],
              ),

              const SizedBox(height: 12),
              const Divider(color: Color(0xFF2D3748), height: 1),
              const SizedBox(height: 12),

              // ─── Password row ────────────────────────────────────────
              Row(
                children: [
                  Expanded(
                    child: Text(
                      _passwordVisible
                          ? widget.entry.password
                          : '•' * widget.entry.password.length.clamp(8, 20),
                      style: TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 14,
                        color: _passwordVisible
                            ? const Color(0xFFE2E8F0)
                            : const Color(0xFF64748B),
                        letterSpacing: _passwordVisible ? 1.0 : 2.0,
                      ),
                    ),
                  ),
                  // Show / hide
                  IconButton(
                    tooltip: _passwordVisible ? 'Ocultar' : 'Revelar',
                    icon: Icon(
                      _passwordVisible
                          ? Icons.visibility_off_outlined
                          : Icons.visibility_outlined,
                      size: 20,
                      color: const Color(0xFF94A3B8),
                    ),
                    onPressed: () =>
                        setState(() => _passwordVisible = !_passwordVisible),
                  ),
                  // Copy
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 200),
                    child: _copied
                        ? const Padding(
                            padding: EdgeInsets.all(8.0),
                            child: Icon(Icons.check_circle_rounded,
                                size: 20, color: Color(0xFF34D399)),
                          )
                        : IconButton(
                            key: const ValueKey('copy'),
                            tooltip: 'Copiar contraseña',
                            icon: const Icon(Icons.copy_outlined,
                                size: 20, color: Color(0xFF94A3B8)),
                            onPressed: _copyPassword,
                          ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
