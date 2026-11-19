# NewsApp

Fondasi aplikasi Android native Kotlin, Compose Material 3, MVVM, dan Clean Architecture.

## Unduh APK

Unduh dan instal `NewsApp.apk` dari halaman [Releases](../../releases/latest). APK untuk pengujian ini sudah dikonfigurasi agar sumber dan artikel dapat langsung dicoba.

## Menjalankan

1. Gunakan JDK 17 dan Android SDK Platform 35.
2. Atur `sdk.dir` pada `local.properties` sesuai lokasi SDK. Isi juga `NEWS_API_KEY` dengan kunci NewsAPI Anda. Simpan file ini hanya di mesin lokal.
3. Jalankan `./gradlew assembleDebug testDebugUnitTest` untuk membangun APK debug dan menjalankan unit test. Di Windows, gunakan `gradlew.bat`.
4. APK tersedia di `app/build/outputs/apk/debug/app-debug.apk`.

Versi dikunci di `gradle/libs.versions.toml`; Gradle Wrapper 8.9 memakai verifikasi SHA-256 distribusi.

## Struktur

- `data`: integrasi remote, DTO, PagingSource, dan implementasi repository.
- `domain`: model dan kontrak repository Kotlin tanpa Android atau Retrofit.
- `presentation`: Compose, navigation, dan ViewModel.
- `src/test`: unit test JVM.

Layer domain, data, dan DI menyediakan model, repository, Retrofit NewsAPI, autentikasi header, pemetaan error API, serta PagingSource sources dan artikel. UI Material 3 menghubungkan kategori → sources → artikel → WebView melalui route `@Serializable` dan Navigation Compose bertipe.

Pencarian memakai debounce 400 ms. Query tersimpan di `SavedStateHandle`; Paging di-cache dalam ViewModel. Navigation Compose dan state daftar mempertahankan tujuan serta posisi scroll saat rotasi. WebView menyimpan riwayat pada pause/dispose, memulihkannya saat dibuat kembali, dan mendahulukan riwayat tersebut ketika pengguna menekan Back. Halaman gagal memuat menampilkan aksi muat ulang. URL non-HTTP(S) tidak dibuka.

`NewsRepository.getSources(category, query)` menyediakan sumber berita dengan filter nama/deskripsi lokal. `getArticles(sources, query)` menerima ID source (atau daftar ID dipisahkan koma) dan pencarian server; minimal salah satu harus terisi. Setiap generasi PagingSource sources menyimpan hasil sekali selama masa pakainya; refresh atau perubahan filter membuat generasi baru. Artikel meminta 20 item per halaman dan berhenti maksimal di 100 hasil untuk developer plan.

Error API muncul sebagai `NewsApiException` pada `LoadState.Error`, dengan `httpStatus`, `code`, dan `message`. Kegagalan jaringan tetap berupa exception aslinya dan cancellation coroutine diteruskan. UI dapat memakai retry Paging untuk mengulangi permintaan yang gagal.

Sources menggunakan pagination client dan pencarian lokal karena endpoint mengembalikan semua sources sekaligus. Everything menggunakan pagination server dengan `page` dan `pageSize`.

## Unit test

Jalankan `./gradlew testDebugUnitTest`. Suite menggunakan JUnit4, MockK, Turbine, coroutine virtual time, dan `paging-testing`.

- PagingSource: halaman awal/lanjutan/akhir, pencarian, hasil kosong, IOException, HttpException 401/426/429, error JSON NewsAPI, retry, refresh key, cancellation, dan batas 100 artikel.
- Repository: pemetaan semua field DTO ke domain termasuk nullable, konfigurasi pagination, kategori, dan validasi query.
- ViewModel: kategori resmi, pemulihan query, debounce 400 ms, cache antar-collector, serta state loading/data/empty/error dan retry melalui presenter Paging yang juga digunakan sebagai dasar UI Compose.

Helper test mengganti Main dispatcher, mengendalikan waktu coroutine, dan memock batas Android Log/Bundle. Tidak ada jaringan NewsAPI yang dipanggil oleh unit test. Retry diuji pada presenter Paging, sesuai tombol UI yang memanggil `LazyPagingItems.retry()`.
