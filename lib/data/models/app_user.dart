/// Oturum açmış kullanıcının arayüzde ihtiyaç duyulan kısmı.
///
/// Firebase türlerini arayüzden ve testlerden uzak tutmak için ayrı model.
class AppUser {
  const AppUser({required this.uid, this.displayName, this.email});

  final String uid;
  final String? displayName;
  final String? email;
}
