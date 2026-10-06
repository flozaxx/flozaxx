/// Uygulamadaki tüm kullanıcıya görünen metinler.
abstract final class Str {
  static const appName = 'Noty';

  // Giriş
  static const signInTitle = 'Şifreli kasanıza hoş geldiniz';
  static const signInSubtitle =
      'Notlarınız ve şifreleriniz cihazınızda şifrelenir; '
      'sunucuya yalnızca şifreli hâlleri gider.';
  static const signInWithGoogle = 'Google ile giriş yap';
  static const signInFailed = 'Giriş yapılamadı. Lütfen tekrar deneyin.';
  static const signInConfigError =
      'Google girişi yapılandırılmamış görünüyor. '
      'Firebase ayarlarını kontrol edin.';
  static const signOut = 'Çıkış yap';

  // Genel
  static const loading = 'Yükleniyor…';
  static const genericError = 'Bir hata oluştu.';
  static const retry = 'Tekrar dene';
}
