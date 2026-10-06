import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/strings_tr.dart';
import 'data/auth_repository.dart';
import 'features/auth/sign_in_screen.dart';
import 'features/home/home_screen.dart';

const _seed = Color(0xFF3F51B5);

ThemeData _theme(Brightness brightness) => ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(
        seedColor: _seed,
        brightness: brightness,
      ),
    );

class NotyApp extends StatelessWidget {
  const NotyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: Str.appName,
      debugShowCheckedModeBanner: false,
      theme: _theme(Brightness.light),
      darkTheme: _theme(Brightness.dark),
      themeMode: ThemeMode.system,
      locale: const Locale('tr'),
      supportedLocales: const [Locale('tr')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      home: const AuthGate(),
    );
  }
}

/// Oturum durumuna göre giriş ekranını veya uygulamayı gösterir.
class AuthGate extends ConsumerWidget {
  const AuthGate({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authStateProvider);
    return auth.when(
      data: (user) =>
          user == null ? const SignInScreen() : HomeScreen(user: user),
      loading: () => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      ),
      error: (_, _) => Scaffold(
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(Str.genericError),
              const SizedBox(height: 8),
              TextButton(
                onPressed: () => ref.invalidate(authStateProvider),
                child: const Text(Str.retry),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
