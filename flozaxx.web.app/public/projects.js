/*
  Sitedeki içeriğin tamamı bu dosyadadır. Yeni proje eklemek için PROJECTS
  listesine bir nesne kopyalayıp düzenlemek yeterli; sıra, sitedeki sıradır.

  slug      : projenin sayfa adresi (flozaxx.com/<slug>)
  kind      : "app" Uygulama · "tool" Rehber / araç · "web" Web
  status    : "live" Yayında · "dev" Geliştiriliyor · "idea" Planlanıyor
              (henüz başlanmamış projede "Öne çıkanlar" yerine "Planlananlar" yazar)
  icon      : app.js içindeki ICONS anahtarlarından biri; yoksa adın baş harfi
  color     : ikon zemini (CSS rengi ya da gradyan), ink: ikon rengi
  links     : ilk bağlantı satırdaki hızlı "aç" düğmesi olur
  planned   : planlanan özellikler (ayrıntıda "Planlananlar" olarak görünür)
  resources : projeye bağlı ek kaynaklar (ayrıntıda "Ek kaynaklar" olarak görünür)
  changelog : sürüm geçmişi, en yeni en üstte: { version?, date, title?, items: [...] }
              proje sayfasında son 3 kayıt, <slug>/changelog sayfasında tamamı görünür

  SITE.now     : sağ sütundaki "Şu an" kutusu
  SITE.updates : sağ sütundaki "Son güncellemeler", en yeni en üstte; en yeni 4 tanesi görünür
*/

window.SITE = {
  updated: "8 Kasım 2026",
  now: [
    { project: "sticker-studio", text: "İlk sürüm üzerinde çalışıyorum: editör ve paket akışı." },
    { project: "noty", text: "1.12.1 yayında; telefonda ve web’de kullanılabilir." },
  ],
  updates: [
    { date: "8 Kas 2026", project: "flozaxx-web", text: "Site flozaxx.com’a taşındı; her projenin kendi sayfası ve sürüm geçmişi var." },
    { date: "7 Kas 2026", project: "flozaxx-web", text: "Site yenilendi: tüm projeler tek sayfada." },
    { date: "3 Kas 2026", project: "noty", text: "Noty 1.12: e-postayla erişim, erişim isteği, “Notlarıma ekle”." },
    { date: "3 Kas 2026", project: "noty", text: "Noty 1.11: uçtan uca şifreli not paylaşımı, okuma görünümü." },
    { date: "1 Kas 2026", project: "once-bak-sonra-yap", text: "“Önce Bak, Sonra Yap” rehberi yazıldı." },
    { date: "24 Eki 2026", project: "noty", text: "Noty 1.10: iki yönlü sabitleme, grup şeridi." },
  ],
};

window.PROJECTS = [
  {
    id: "noty",
    slug: "noty",
    name: "Noty",
    tagline: "Şifreli not ve şifre kasası",
    kind: "app",
    status: "live",
    icon: "noty",
    color: "linear-gradient(135deg, #2F8F65, #1E6446)",
    platforms: ["Web", "Android"],
    tech: ["Flutter", "Dart", "Firebase", "Riverpod"],
    version: "1.12.1",
    started: "Ekim 2026",
    summary:
      "Notlarımı ve şifrelerimi tek yerde, güvenle tutmak için yaptım. Başlıklar dahil her şey cihazda, hesaba özel bir anahtarla şifrelenir; sunucuya yalnızca şifreli veri gider. Anahtarın sunucuda kopyası yoktur.",
    features: [
      "Zengin metin notlar: başlıklar, listeler, yapılacaklar, alıntı, kod ve görsel; satır başında “/” ile komut menüsü",
      "Şifre kasası: güçlü şifre üretici, zayıf ve tekrar kullanılan şifre uyarıları; kopyalanan şifre 30 sn sonra panodan silinir",
      "Uçtan uca şifreli not paylaşımı: bağlantıyla ya da e-postayla, görüntüleme veya düzenleme yetkisiyle",
      "Gruplar, sabitleme, 30 günlük çöp kutusu; bilgisayarda Ctrl+K komut paleti",
      "Şifreli .noty yedeği; Chrome, Firefox, Bitwarden (CSV) ve Google Keep’ten içe aktarma",
      "PIN, desen ya da şifre kilidi; Android’de ekran görüntüsü koruması",
      "Koyu ve açık tema; telefonda sade arayüz, geniş ekranda panel düzeni",
    ],
    security: "AES-256-GCM şifreleme · Argon2id ile korunan yedekler · sıkı Firestore kuralları",
    links: [{ label: "Web uygulamasını aç", url: "https://noty.tr" }],
    changelog: [
      { version: "1.12.1", date: "3 Kasım 2026", title: "Beyaz ekran düzeltmesi", items: [
        "Bazı cihazlarda girişten sonra uygulamanın boş, soluk bir ekranda kalması düzeltildi (telefon ve web).",
        "Yeni eklenen ayarların okunamaması bu hataya yol açıyordu; artık her yeni ayar otomatik testle denetleniyor.",
      ] },
      { version: "1.12.0", date: "3 Kasım 2026", title: "E-postayla erişim ve erişim isteği", items: [
        "Bir notu e-postasını yazdığın kişiyle paylaşabilirsin; not onun “Benimle paylaşılanlar” bölümüne düşer, bildirim gelir, bağlantı gerekmez.",
        "Kişi henüz Noty kullanmıyorsa erişim bekler; Noty’ye katıldığında kendiliğinden tamamlanır.",
        "Yalnızca davetlilere açık bir bağlantıyı açan biri “Erişim iste” diyebilir; istek sana bildirim olarak gelir, Ayarlar’dan onaylar ya da reddedersin.",
        "“Notlarıma ekle”: seninle paylaşılan not, Notlar sekmesinde “ad tarafından paylaşıldı” dipnotuyla durur.",
        "Android’de sistem bildirimleri.",
      ] },
      { version: "1.11.0", date: "3 Kasım 2026", title: "Uçtan uca şifreli not paylaşımı", items: [
        "Paylaş ikonu bir bağlantı oluşturur; anahtar yalnızca bağlantının içindedir, sunucu notu okuyamaz.",
        "Bağlantıya sahip herkes görüntüleyebilir ya da düzenleyebilir; istersen yalnızca davet ettiğin kişiler.",
        "Not önce okuma görünümünde açılır; kalemle düzenlemeye geçilir. Yapılacak kutucukları okurken de işaretlenir.",
        "“Aa” paneli: başlık seviyeleri, yazı boyutu ve hizalama.",
        "Android’de ekran koruması ayarı; kapatmak kilit doğrulaması ister.",
      ] },
      { version: "1.10.0", date: "24 Ekim 2026", title: "İki yönlü sabitleme", items: [
        "Bir kaydı listenin en üstüne ya da ana sayfaya sabitleyebilirsin.",
        "Kayıt ekranında menü yerine raptiye, kalem ve sil ikonları.",
        "Bilgisayarda kopyala ikonu yalnızca fareyle satırın üstüne gelince görünür.",
        "CSV içe aktarmada “www.instagram.com” gibi adlar okunur ada (“Instagram”) çevrilebilir.",
      ] },
      { version: "1.9", date: "23 Ekim 2026", title: "Yeni görünüm: çam yeşili ve Figtree", items: [
        "Vurgu rengi çam yeşili; uygulama ikonu yeşile geçti, yazı tipi Figtree.",
        "Telefonda gruplu yuvarlak kartlar, hap biçimli düğmeler ve arama kutusu.",
        "1.9.1: logo yazısının oranları düzeltildi.",
        "1.9.2: yüzlerce kayıtta listeler akıcı; “Geri al” bildirimi 5 sn sonra kapanıyor.",
      ] },
      { version: "1.8.0", date: "23 Ekim 2026", title: "Yedek ve içe aktarma", items: [
        "Şifreli .noty yedeği: kendi belirlediğin yedek şifresiyle korunur, başka hesaba da yüklenebilir.",
        "Chrome, Firefox ve Bitwarden şifrelerini (CSV) ve Google Keep notlarını içe aktarma.",
        "Her işlemden önce ne ekleneceği önizlenir; dosyalar cihazda okunur.",
      ] },
      { version: "1.7.0", date: "23 Ekim 2026", title: "Çöp kutusu", items: [
        "Silinen kayıt önce çöp kutusuna gider; “Geri al” ile hemen döner.",
        "30 günü geçenler kendiliğinden kalıcı silinir; sunucu hangi kaydın çöpte olduğunu göremez.",
      ] },
      { version: "1.6.0", date: "23 Ekim 2026", title: "“Kaldığın yerden” ana sayfası", items: [
        "Ana sayfada son düzenlenen not, sabitlenenler ve son değişenler.",
        "Ctrl+K komut paleti; yazı alanında değilken N yeni not açar.",
      ] },
      { version: "1.4 – 1.5", date: "23 Ekim 2026", title: "Gruplar ve zengin şifre notları", items: [
        "Benzer kayıtları gruplama, seçim modu ve grup düzenleyici.",
        "Şifre kaydının notu da zengin metin; “/” komutları orada da çalışır.",
        "1.5.1: panel pencereleri sürüklenirken akıcı.",
      ] },
      { version: "1.1 – 1.3", date: "22 Ekim 2026", title: "Yeni arayüz ve zengin not düzenleyici", items: [
        "Zengin not düzenleyici ve yeni arayüz (1.1.0).",
        "Geniş ekranda panel pencereleri, şifrede adres önerileri (1.2.0).",
        "Arayüz ölçeği ayarı (1.3.0) ve yeni “n” logosu (1.3.1).",
      ] },
      { version: "1.0", date: "7 Ekim 2026", title: "İlk sürüm", items: [
        "Google ile giriş; her şey cihazda şifrelenir, web ve Android’de çalışır.",
        "PIN, desen ya da şifre kilidi; ekran görüntüsü koruması.",
      ] },
    ],
  },
  {
    id: "sticker-studio",
    slug: "stickerstudio",
    name: "Sticker Studio",
    tagline: "Fotoğraf ve videolardan WhatsApp çıkartma paketleri",
    kind: "app",
    status: "dev",
    icon: "sticker",
    color: "#4FF07F",
    ink: "#003914",
    platforms: ["Android"],
    tech: ["Kotlin", "Jetpack Compose", "Material 3"],
    started: "Ekim 2026",
    summary:
      "Telefondaki fotoğraf ve videolardan birkaç dokunuşla WhatsApp çıkartma paketi hazırlayan Android uygulaması. Paket, WhatsApp’a eklenmeden önce orada nasıl görüneceğiyle önizlenir. Henüz mağazada değil.",
    features: [
      "Fotoğraftan statik, videodan animasyonlu çıkartma",
      "Kontur (beyaz çerçeve) stilleri; seçim önizlemeyle yapılır",
      "Akıcı, Dengeli ve Net kalite seçenekleri",
      "Adım rehberi: biten, sıradaki ve isteğe bağlı adımlar tek bakışta",
      "“WhatsApp’ta böyle görünecek” paket önizlemesi",
      "Hızlı paket ön ayarları ve Paketlerim listesi",
      "Türkçe ve İngilizce arayüz",
    ],
    links: [],
    changelog: [
      { date: "1 Kasım 2026", title: "Editör yenilendi", items: [
        "Kaydet düğmesi üst çubukta, paneller arası yumuşak geçiş.",
        "Paneller tek bileşende birleşti; ekranda en fazla iki katman.",
        "Daha sakin yüzeyler; iç bölümlerde geri oku.",
        "Kod temizliği ve hata düzeltmeleri.",
      ] },
      { date: "Ekim 2026", title: "İlk çalışan sürüm", items: [
        "Fotoğraf ve videodan çıkartma, paket oluşturma.",
      ] },
    ],
  },
  {
    id: "pc-kontrol",
    slug: "pckontrol",
    name: "PC Kontrol",
    tagline: "Evdeki bilgisayarı telefondan yönetmek",
    kind: "app",
    status: "dev",
    icon: "power",
    color: "#E5484D",
    platforms: ["Android", "Windows"],
    tech: ["Kotlin", "Jetpack Compose", "PowerShell"],
    started: "Ağustos 2026",
    summary:
      "Bilgisayarımı telefondan kilitlemek, uyutmak, kapatmak ya da ekranına bakmak için yaptığım uygulama. Bilgisayarda arka planda çalışan küçük bir PowerShell sunucusu, telefonda tek ekranlık bir Compose uygulaması var; her komut gizli bir anahtarla doğrulanır. Sıradaki büyük adım kapalı bilgisayarı Wake-on-LAN ile uzaktan açmak.",
    features: [
      "Güç: kilitle, uyku, hazırda beklet, kapat ve kapatmayı iptal et",
      "Uzaktan ekran görüntüsü; istenirse telefonun galerisine kendiliğinden kaydedilir",
      "Dosya açma ve program başlatma, yalnızca izin verilenler listesinden",
      "Dosya ve program listeleri; bilgisayarın açık olup olmadığını gösteren durum",
      "Ev dışından erişim: bilgisayar bir tünel üzerinden dışarıya açılır; sunucu pencere açmadan arka planda çalışır",
    ],
    planned: [
      "Wake-on-LAN: kapalı bilgisayarı telefondan açmak. Bilgisayar kapalıyken kendi sunucusu da kapalı olduğundan, sihirli paketi evde sürekli açık bir cihaz (modem, Raspberry Pi ya da eski bir telefon) gönderecek",
      "Yeniden başlatma komutu",
      "Ayrıntılı durum: işlemci, bellek, açık kalma süresi",
      "“Eve geliyorum” modu: konuma göre bilgisayarı kendiliğinden açmak",
      "Ana ekran widget'ı ve kısayollar",
    ],
    links: [],
  },
  {
    id: "rubik",
    slug: "rubik",
    name: "Rubik Küpü Rehberi",
    tagline: "Hiç çözmemişlikten körlemeye, etkileşimli küp rehberi",
    kind: "tool",
    status: "live",
    icon: "cube",
    color: "#F08619",
    platforms: ["Web"],
    tech: ["HTML/CSS/JS", "three.js"],
    started: "Ekim 2026",
    summary:
      "Küpü sıfırdan öğrenmek ya da hızlanmak için beş yol. Her yöntem adım adım anlatılır, formüller 3B küp üzerinde oynatılır.",
    features: [
      "Çırak: 8 durakta, kat kat ilk çözüm",
      "CFOP (Cross · F2L · OLL · PLL) ve Roux (iki blok · CMLL · LSE)",
      "ZB ile son katı tek formülde bitirme",
      "Körleme: ezberle, gözü kapalı çöz",
      "Formül kütüphanesi: F2L, OLL, PLL, ZBLL, CMLL",
      "Serbest küp: sürükleyip çevir, karıştır, süre tut; kendi küpünü kamerayla tara, beş yöntemi yarıştır",
    ],
    links: [{ label: "Rehberi aç", url: "/p/rubik/" }],
    resources: [
      {
        name: "BLD Harf Küpü",
        url: "/p/bld/",
        text: "Körleme harf şemasını (sarı üst, kırmızı ön; büyük harf köşe, küçük harf kenar) 3B küpte öğreten ve sınayan araç. Keşfet, harf sor, yer bul; sık yanılınan harfler daha sık sorulur.",
      },
    ],
  },
  {
    id: "once-bak-sonra-yap",
    slug: "oncebak",
    name: "Önce Bak, Sonra Yap",
    tagline: "AI destekli yazılımda profesyonel standartlar",
    kind: "tool",
    status: "live",
    icon: "compass",
    color: "#7A5AF8",
    platforms: ["Web"],
    tech: [],
    started: "Kasım 2026",
    summary:
      "Yeni bir uygulama ya da ekrana başlarken izlediğim yöntem: kod yazmadan önce iyi örnekleri incele, ortak olanı kural kabul et, farkı bilinçli yerlerde yarat. Kaynaklı tablolar ve A, B, C diye sıralanmış adımlarla.",
    features: [
      "Referans araştırma şablonu: 3–5 örnek uygulama, ortak kalıplar ve kararlar",
      "AI ile yazılmış yazılımların büyük sorunları ve sektördeki karşılıkları",
      "Arayüz: profesyonel editörlerin ortak düzeni, Sticker Studio editörü için öneriler",
      "Optimizasyon: kare bütçesi, vekil önizleme, arka plan işleri",
      "Kod sadeliği: AI kodunu ele veren izler, yorum kuralları, anlama borcu",
      "Adım adım iş akışı: A planlama, B geliştirme, C yayın öncesi",
    ],
    links: [{ label: "Rehberi oku", url: "/p/once-bak-sonra-yap/" }],
  },
  {
    id: "flozaxx-web",
    slug: "flozaxx",
    name: "flozaxx.com",
    tagline: "Bu site: tüm projelerimin özeti",
    kind: "web",
    status: "live",
    icon: "globe",
    color: "linear-gradient(135deg, #1D3D53, #191819)",
    platforms: ["Web"],
    tech: ["HTML/CSS/JS", "Firebase"],
    started: "2025",
    summary:
      "Projelerimi tek sayfada toplayan kişisel site. Çerçevesiz, sade HTML, CSS ve JavaScript; içeriğin tamamı tek bir veri dosyasından gelir.",
    features: [
      "Kategori ve teknoloji süzgeci; aynı anda tek proje açık kalır",
      "Her projenin paylaşılabilir bağlantısı (#kimlik)",
      "Geniş ekranda sayfa içi gezinme, güncel durum ve son güncellemeler",
      "Koyu ve açık tema; varsayılan olarak cihaz ayarını izler",
    ],
    links: [{ label: "Kaynak kodu", url: "https://github.com/flozaxx/flozaxx/tree/main/flozaxx.web.app" }],
    changelog: [
      { date: "8 Kasım 2026", title: "flozaxx.com ve proje sayfaları", items: [
        "Site flozaxx.com alan adına taşındı.",
        "Her projenin kendi sayfası; Noty, Sticker Studio ve site için sürüm geçmişi.",
        "Ana sayfada sade kartlar; telefonda sol üstten açılan menü.",
      ] },
      { date: "7 Kasım 2026", title: "Site baştan yazıldı", items: [
        "Tüm projeler tek sayfada; kategori ve teknoloji süzgeci, yan sütunlar.",
        "“Önce Bak, Sonra Yap” rehber sayfası.",
      ] },
    ],
  },
];
