import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

import 'package:vault_client/main.dart';

void main() {
  setUpAll(() async {
    // Load a minimal .env for tests
    await dotenv.load(mergeWith: {'API_BASE_URL': 'http://localhost:8443'});
  });

  testWidgets('App renders UnlockScreen when not authenticated', (tester) async {
    await tester.pumpWidget(const VaultApp());
    expect(find.text('Bóveda de Contraseñas'), findsOneWidget);
  });
}
