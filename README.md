# HerbaMed Jabar

HerbaMed Jabar adalah aplikasi Android yang dirancang untuk membantu pengguna mengidentifikasi tanaman herbal melalui pemindaian gambar. Aplikasi ini juga menyediakan platform bagi pengguna untuk berbagi penemuan mereka dan berdiskusi di forum.

## ✨ Fitur

* **Pemindaian Tanaman**: Identifikasi tanaman herbal dengan memindai gambar menggunakan kamera ponsel.
* **Forum Diskusi**: Bagikan hasil pindaian dan diskusikan tanaman herbal dengan pengguna lain.
* **Profil Pengguna**: Lihat riwayat postingan dan lencana yang diperoleh.
* **Riwayat Pemindaian**: Simpan dan lihat riwayat tanaman yang pernah Anda pindai.
* **Autentikasi Pengguna**: Sistem pendaftaran dan login yang aman.

## 🛠️ Teknologi yang Digunakan

* **Bahasa**: [Kotlin](https://kotlinlang.org/)
* **Arsitektur**: MVVM (Model-View-ViewModel)
* **UI**: XML Layouts
* **Asynchronous**: Coroutines
* **Dependency Injection**: Hilt
* **Jaringan**:
  * [Firebase Firestore](https://firebase.google.com/docs/firestore) - untuk database forum
  * [Firebase Storage](https://firebase.google.com/docs/storage) - untuk penyimpanan gambar
  * [Cloudinary](https://cloudinary.com/) - untuk unggah gambar
* **Database Lokal**: [Room](https://developer.android.com/training/data-storage/room)
* **AI**: [Google Generative AI (Gemini)](https://ai.google.dev/)
* **Lainnya**:
  * [CameraX](https://developer.android.com/training/camerax) - untuk fungsionalitas kamera
  * [Coil](https://coil-kt.github.io/coil/) - untuk memuat gambar
  * [Lottie](https://lottiefiles.com/) - untuk animasi

## ⚙️ Instalasi dan Penggunaan

1. **Clone repositori ini:**

    ```bash
    git clone https://github.com/ebinugraha/herbamed-jabar.git
    ```

2. **Buka proyek di Android Studio.**
3. **Tambahkan file `google-services.json` Anda:**
    * Buat proyek Firebase baru di [Firebase Console](https://console.firebase.google.com/).
    * Tambahkan aplikasi Android ke proyek Firebase Anda.
    * Unduh file `google-services.json` dan letakkan di direktori `app/`.
4. **Tambahkan kunci API Anda:**
    * Buat file `local.properties` di direktori root proyek.
    * Tambahkan kunci API Google Generative AI Anda ke dalam `local.properties`:

        ```properties
        apiKey="KUNCI_API_ANDA"
        ```

5. **Jalankan aplikasi.**

## 🤝 Kontribusi

Kontribusi sangat diterima! Jika Anda ingin berkontribusi pada proyek ini, silakan fork repositori ini dan buat *pull request*.

## 📄 Lisensi

Proyek ini dilisensikan di bawah [Lisensi MIT](LICENSE).
