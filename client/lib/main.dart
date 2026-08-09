import 'package:flutter/material.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:provider/provider.dart';
import 'package:google_fonts/google_fonts.dart';

import 'models/auth_state.dart';
import 'screens/unlock_screen.dart';
import 'screens/vault_list_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await dotenv.load(fileName: 'assets/.env');
  runApp(const VaultApp());
}

class VaultApp extends StatelessWidget {
  const VaultApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ChangeNotifierProvider(
      create: (_) => AuthState(),
      child: MaterialApp(
        title: 'Bóveda de Contraseñas',
        debugShowCheckedModeBanner: false,

        // ─── Dark theme ──────────────────────────────────────────────────
        themeMode: ThemeMode.dark,
        darkTheme: _buildDarkTheme(),
        theme: _buildDarkTheme(),

        // ─── Routing ────────────────────────────────────────────────────
        home: Consumer<AuthState>(
          builder: (context, auth, _) {
            if (auth.isAuthenticated) {
              return const VaultListScreen();
            }
            return const UnlockScreen();
          },
        ),
      ),
    );
  }

  ThemeData _buildDarkTheme() {
    const bg       = Color(0xFF0F1117);   // near-black background
    const surface  = Color(0xFF1A1D27);   // card surface
    const primary  = Color(0xFF6C63FF);   // indigo-violet accent
    const onSurface = Color(0xFFE2E8F0);  // text on surface

    final base = ThemeData.dark(useMaterial3: true);
    return base.copyWith(
      colorScheme: ColorScheme.dark(
        surface: surface,
        primary: primary,
        onPrimary: Colors.white,
        onSurface: onSurface,
        outline: const Color(0xFF2D3748),
      ),
      scaffoldBackgroundColor: bg,
      cardTheme: CardThemeData(
        color: surface,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: const BorderSide(color: Color(0xFF2D3748), width: 1),
        ),
      ),
      textTheme: GoogleFonts.interTextTheme(base.textTheme).apply(
        bodyColor: onSurface,
        displayColor: onSurface,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: const Color(0xFF1A1D27),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF2D3748)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF2D3748)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: primary, width: 2),
        ),
        labelStyle: const TextStyle(color: Color(0xFF94A3B8)),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 32),
          elevation: 0,
        ),
      ),
    );
  }
}
