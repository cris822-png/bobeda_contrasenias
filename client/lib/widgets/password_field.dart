import 'package:flutter/material.dart';

/// Reusable password input field with show/hide toggle.
class PasswordField extends StatefulWidget {
  final TextEditingController controller;
  final String labelText;
  final String? Function(String?)? validator;
  final void Function(String)? onChanged;

  const PasswordField({
    super.key,
    required this.controller,
    this.labelText = 'Contraseña',
    this.validator,
    this.onChanged,
  });

  @override
  State<PasswordField> createState() => _PasswordFieldState();
}

class _PasswordFieldState extends State<PasswordField> {
  bool _visible = false;

  @override
  Widget build(BuildContext context) {
    return TextFormField(
      controller: widget.controller,
      obscureText: !_visible,
      onChanged: widget.onChanged,
      validator: widget.validator,
      decoration: InputDecoration(
        labelText: widget.labelText,
        prefixIcon: const Icon(Icons.lock_outline),
        suffixIcon: IconButton(
          icon: Icon(
            _visible ? Icons.visibility_off_outlined : Icons.visibility_outlined,
            color: const Color(0xFF94A3B8),
          ),
          onPressed: () => setState(() => _visible = !_visible),
        ),
      ),
    );
  }
}
