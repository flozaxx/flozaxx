import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_sign_in/google_sign_in.dart';

import 'models/app_user.dart';

/// Kullanıcının kendi isteğiyle iptal ettiği giriş denemesi.
class SignInCancelled implements Exception {
  const SignInCancelled();
}

/// Google girişinin bu derlemede yapılandırılmamış olması
/// (ör. SHA-1 parmak izi veya web client ID eksik).
class SignInConfigurationError implements Exception {
  const SignInConfigurationError();
}

abstract interface class AuthRepository {
  Stream<AppUser?> authStateChanges();

  /// Başarısızlıkta [SignInCancelled], [SignInConfigurationError]
  /// veya başka bir istisna fırlatır.
  Future<void> signInWithGoogle();

  Future<void> signOut();
}

class FirebaseAuthRepository implements AuthRepository {
  FirebaseAuthRepository({FirebaseAuth? auth})
      : _auth = auth ?? FirebaseAuth.instance;

  final FirebaseAuth _auth;
  Future<void>? _googleInit;

  @override
  Stream<AppUser?> authStateChanges() => _auth.authStateChanges().map(
        (u) => u == null
            ? null
            : AppUser(uid: u.uid, displayName: u.displayName, email: u.email),
      );

  @override
  Future<void> signInWithGoogle() async {
    if (kIsWeb) {
      await _signInWeb();
    } else {
      await _signInNative();
    }
  }

  Future<void> _signInWeb() async {
    try {
      await _auth.signInWithPopup(GoogleAuthProvider());
    } on FirebaseAuthException catch (e) {
      switch (e.code) {
        case 'popup-closed-by-user':
        case 'cancelled-popup-request':
          throw const SignInCancelled();
        case 'operation-not-allowed':
        case 'unauthorized-domain':
          throw const SignInConfigurationError();
        default:
          rethrow;
      }
    }
  }

  Future<void> _signInNative() async {
    // serverClientId verilmez: Android eklentisi google-services.json'dan
    // üretilen `default_web_client_id` kaynağını kullanır.
    await (_googleInit ??= GoogleSignIn.instance.initialize());
    final GoogleSignInAccount account;
    try {
      account = await GoogleSignIn.instance.authenticate();
    } on GoogleSignInException catch (e) {
      switch (e.code) {
        case GoogleSignInExceptionCode.canceled:
        case GoogleSignInExceptionCode.interrupted:
          throw const SignInCancelled();
        case GoogleSignInExceptionCode.clientConfigurationError:
        case GoogleSignInExceptionCode.providerConfigurationError:
          throw const SignInConfigurationError();
        default:
          rethrow;
      }
    }
    final idToken = account.authentication.idToken;
    if (idToken == null) throw const SignInConfigurationError();
    await _auth.signInWithCredential(
      GoogleAuthProvider.credential(idToken: idToken),
    );
  }

  @override
  Future<void> signOut() async {
    if (!kIsWeb) {
      // Bir sonraki girişte hesap seçiciyi yeniden göstermek için.
      await (_googleInit ??= GoogleSignIn.instance.initialize());
      await GoogleSignIn.instance.signOut();
    }
    await _auth.signOut();
  }
}

final authRepositoryProvider = Provider<AuthRepository>(
  (ref) => FirebaseAuthRepository(),
);

final authStateProvider = StreamProvider<AppUser?>(
  (ref) => ref.watch(authRepositoryProvider).authStateChanges(),
);
