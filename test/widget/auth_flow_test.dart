import 'dart:async';

import 'package:flozaxx/app.dart';
import 'package:flozaxx/core/strings_tr.dart';
import 'package:flozaxx/data/auth_repository.dart';
import 'package:flozaxx/data/models/app_user.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

class FakeAuthRepository implements AuthRepository {
  final _controller = StreamController<AppUser?>.broadcast();
  AppUser? _user;
  Object? nextSignInError;
  int signInCalls = 0;

  @override
  Stream<AppUser?> authStateChanges() async* {
    yield _user;
    yield* _controller.stream;
  }

  @override
  Future<void> signInWithGoogle() async {
    signInCalls++;
    final error = nextSignInError;
    if (error != null) throw error;
    _user = const AppUser(uid: 'u1', email: 'test@example.com');
    _controller.add(_user);
  }

  @override
  Future<void> signOut() async {
    _user = null;
    _controller.add(null);
  }
}

Future<FakeAuthRepository> pumpApp(WidgetTester tester) async {
  final repo = FakeAuthRepository();
  await tester.pumpWidget(
    ProviderScope(
      overrides: [authRepositoryProvider.overrideWithValue(repo)],
      child: const FlozaxxApp(),
    ),
  );
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('oturum yokken giriş ekranı gösterilir', (tester) async {
    await pumpApp(tester);
    expect(find.text(Str.signInWithGoogle), findsOneWidget);
  });

  testWidgets('giriş başarılıysa ana ekrana, çıkışta geri döner',
      (tester) async {
    final repo = await pumpApp(tester);

    await tester.tap(find.text(Str.signInWithGoogle));
    await tester.pumpAndSettle();
    expect(repo.signInCalls, 1);
    expect(find.text('test@example.com'), findsOneWidget);

    await tester.tap(find.byTooltip(Str.signOut));
    await tester.pumpAndSettle();
    expect(find.text(Str.signInWithGoogle), findsOneWidget);
  });

  testWidgets('kullanıcı vazgeçerse hata mesajı gösterilmez', (tester) async {
    final repo = await pumpApp(tester);
    repo.nextSignInError = const SignInCancelled();

    await tester.tap(find.text(Str.signInWithGoogle));
    await tester.pumpAndSettle();
    expect(find.text(Str.signInFailed), findsNothing);
    expect(find.text(Str.signInConfigError), findsNothing);
  });

  testWidgets('yapılandırma hatası ve genel hata ayrı mesaj gösterir',
      (tester) async {
    final repo = await pumpApp(tester);

    repo.nextSignInError = const SignInConfigurationError();
    await tester.tap(find.text(Str.signInWithGoogle));
    await tester.pumpAndSettle();
    expect(find.text(Str.signInConfigError), findsOneWidget);

    repo.nextSignInError = Exception('ağ');
    await tester.tap(find.text(Str.signInWithGoogle));
    await tester.pumpAndSettle();
    expect(find.text(Str.signInFailed), findsOneWidget);
  });

  testWidgets('koyu temada da açılır', (tester) async {
    tester.platformDispatcher.platformBrightnessTestValue = Brightness.dark;
    addTearDown(tester.platformDispatcher.clearPlatformBrightnessTestValue);
    await pumpApp(tester);
    final context = tester.element(find.byType(Scaffold));
    expect(Theme.of(context).brightness, Brightness.dark);
  });
}
