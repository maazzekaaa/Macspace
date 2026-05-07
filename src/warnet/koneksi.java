package warnet;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class koneksi {
    
    // Konfigurasi Database
    static final String DB_URL = "jdbc:mysql://localhost:3306/db_warnet";
    static final String USER = "root"; // Username default XAMPP
    static final String PASS = "";     // Password default XAMPP biasanya kosong

    public static void main(String[] args) {
        // Deklarasi object Connection
        Connection conn = null;

        try {
            // Langkah 1: Mendaftarkan JDBC Driver (Opsional untuk versi JDBC terbaru, tapi aman untuk ditulis)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Langkah 2: Membuka koneksi
            System.out.println("Menghubungkan ke database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            
            System.out.println("Koneksi berhasil! Siap mengeksekusi query untuk transaksi atau penjadwalan.");

        } catch (SQLException e) {
            System.err.println("Gagal terhubung ke database. Cek URL, Username, atau Password!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL tidak ditemukan. Pastikan file .jar sudah ditambahkan ke library!");
            e.printStackTrace();
        } finally {
            // Langkah 3: Menutup koneksi (Best Practice)
            try {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                    System.out.println("Koneksi ditutup.");
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}