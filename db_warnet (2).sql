-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Waktu pembuatan: 07 Bulan Mei 2026 pada 15.31
-- Versi server: 10.4.32-MariaDB
-- Versi PHP: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `db_warnet`
--

DELIMITER $$
--
-- Prosedur
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `BuatTransaksi` (IN `p_nama` VARCHAR(100), IN `p_pc_type` VARCHAR(50), IN `p_durasi` INT, IN `p_total` DOUBLE, IN `p_pc_id` INT, IN `p_payment` VARCHAR(20))   BEGIN
  INSERT INTO transactions(customer_name, pc_type, duration, total_amount, pc_id, status, payment_method)
  VALUES (p_nama, p_pc_type, p_durasi, p_total, p_pc_id, 'UNUSED', p_payment);

  SELECT LAST_INSERT_ID() AS id_transaksi_baru;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `CariTransaksi` (IN `p_nama` VARCHAR(100))   BEGIN
  SELECT
    id, customer_name, pc_type, duration,
    total_amount, payment_method, status, created_at
  FROM transactions
  WHERE customer_name LIKE CONCAT('%', p_nama, '%')
  ORDER BY created_at DESC;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `LaporanPerTipePC` ()   BEGIN
  SELECT
    pc_type                  AS Tipe_PC,
    COUNT(*)                 AS Jumlah_Transaksi,
    SUM(total_amount)        AS Total_Pendapatan,
    AVG(total_amount)        AS Rata_Rata,
    MAX(total_amount)        AS Tertinggi,
    MIN(total_amount)        AS Terendah
  FROM transactions
  WHERE status = 'SELESAI'
  GROUP BY pc_type
  ORDER BY Total_Pendapatan DESC;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `RekapPelanggan` (IN `p_nama` VARCHAR(100))   BEGIN
  SELECT
    t.customer_name,
    COUNT(DISTINCT t.id)           AS Total_Transaksi,
    SUM(t.total_amount)            AS Total_Bayar_Sewa,
    COALESCE(SUM(o.subtotal), 0)   AS Total_Belanja_Makanan,
    SUM(t.total_amount) + COALESCE(SUM(o.subtotal), 0) AS Grand_Total
  FROM transactions t
  LEFT JOIN orders o ON t.id = o.transaction_id
  WHERE t.customer_name = p_nama AND t.status = 'SELESAI'
  GROUP BY t.customer_name;
END$$

CREATE DEFINER=`root`@`localhost` PROCEDURE `TambahOrder` (IN `p_transaction_id` INT, IN `p_customer_name` VARCHAR(100), IN `p_item_name` VARCHAR(100), IN `p_qty` INT, IN `p_subtotal` DOUBLE)   BEGIN
  INSERT INTO orders(transaction_id, customer_name, item_name, qty, subtotal)
  VALUES (p_transaction_id, p_customer_name, p_item_name, p_qty, p_subtotal);
END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Struktur dari tabel `komputer`
--

CREATE TABLE `komputer` (
  `id_pc` int(11) NOT NULL,
  `tipe_pc` varchar(50) NOT NULL,
  `biaya_tambahan_per_jam` decimal(10,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `komputer`
--

INSERT INTO `komputer` (`id_pc`, `tipe_pc`, `biaya_tambahan_per_jam`) VALUES
(1, 'PC Standar (Browsing)', 0.00),
(2, 'PC VIP (Desain/Coding)', 2000.00),
(3, 'PC Gaming (Esports)', 5000.00);

-- --------------------------------------------------------

--
-- Struktur dari tabel `menu`
--

CREATE TABLE `menu` (
  `id` int(11) NOT NULL,
  `name` varchar(100) DEFAULT NULL,
  `type` varchar(50) DEFAULT NULL,
  `price` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `menu`
--

INSERT INTO `menu` (`id`, `name`, `type`, `price`) VALUES
(1, 'Indomie Goreng', 'Makanan', 12000),
(2, 'Nasi Goreng Gila', 'Makanan', 18000),
(3, 'Roti Bakar Coklat Keju', 'Makanan', 15000),
(4, 'Es Kopi Susu Aren', 'Minuman', 15000),
(5, 'Ice Lemon Tea', 'Minuman', 8000),
(6, 'Mineral Water', 'Minuman', 5000);

-- --------------------------------------------------------

--
-- Struktur dari tabel `orders`
--

CREATE TABLE `orders` (
  `id` int(11) NOT NULL,
  `transaction_id` int(11) DEFAULT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `item_name` varchar(100) DEFAULT NULL,
  `qty` int(11) DEFAULT NULL,
  `subtotal` double DEFAULT NULL,
  `status` varchar(20) DEFAULT 'BELUM DIANTAR'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `orders`
--

INSERT INTO `orders` (`id`, `transaction_id`, `customer_name`, `item_name`, `qty`, `subtotal`, `status`) VALUES
(60, 86, 'rezz', 'Indomie Goreng', 1, 12000, 'DIANTAR'),
(61, 86, 'rezz', 'Roti Bakar Coklat Keju', 1, 15000, 'DIANTAR'),
(62, 86, 'rezz', 'Roti Bakar Coklat Keju', 1, 15000, 'DIANTAR');

-- --------------------------------------------------------

--
-- Struktur dari tabel `pcs`
--

CREATE TABLE `pcs` (
  `id` int(11) NOT NULL,
  `status` varchar(20) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `pcs`
--

INSERT INTO `pcs` (`id`, `status`) VALUES
(1, 'TERSEDIA'),
(2, 'PERBAIKAN'),
(3, 'TERSEDIA'),
(4, 'TERSEDIA'),
(5, 'TERSEDIA'),
(6, 'TERSEDIA'),
(7, 'TERSEDIA'),
(8, 'TERSEDIA'),
(9, 'TERSEDIA'),
(10, 'TERSEDIA'),
(11, 'TERSEDIA'),
(12, 'TERSEDIA'),
(13, 'TERSEDIA'),
(14, 'TERSEDIA'),
(15, 'TERSEDIA'),
(16, 'TERSEDIA'),
(17, 'TERSEDIA'),
(18, 'AKTIF'),
(19, 'TERSEDIA'),
(20, 'TERSEDIA'),
(21, 'TERSEDIA'),
(22, 'DIPESAN'),
(23, 'TERSEDIA'),
(24, 'TERSEDIA'),
(25, 'TERSEDIA'),
(26, 'TERSEDIA'),
(27, 'TERSEDIA'),
(28, 'TERSEDIA'),
(29, 'TERSEDIA'),
(30, 'TERSEDIA');

-- --------------------------------------------------------

--
-- Struktur dari tabel `transactions`
--

CREATE TABLE `transactions` (
  `id` int(11) NOT NULL,
  `customer_name` varchar(100) DEFAULT NULL,
  `pc_type` varchar(50) DEFAULT NULL,
  `duration` int(11) DEFAULT NULL,
  `total_amount` double DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `pc_id` int(11) DEFAULT 0,
  `activation_code` varchar(20) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'UNUSED',
  `activated_at` timestamp NULL DEFAULT NULL,
  `payment_method` varchar(20) DEFAULT 'Tunai',
  `booking_date` date DEFAULT NULL,
  `booking_datetime` datetime DEFAULT NULL,
  `is_activated` int(11) DEFAULT 0,
  `duration_minutes` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `transactions`
--

INSERT INTO `transactions` (`id`, `customer_name`, `pc_type`, `duration`, `total_amount`, `created_at`, `pc_id`, `activation_code`, `status`, `activated_at`, `payment_method`, `booking_date`, `booking_datetime`, `is_activated`, `duration_minutes`) VALUES
(66, 'user', 'VIP', 1, 25000, '2026-05-07 19:08:59', 13, 'LANGSUNG', 'SELESAI', '2026-05-07 10:07:59', 'QRIS', NULL, NULL, 1, 60),
(67, 'rezz', 'VVIP', 1, 35000, '2026-05-07 18:14:32', 24, 'VV-63213', 'SELESAI', '2026-05-07 11:16:00', 'Tunai', NULL, '2026-05-07 18:16:00', 0, 60),
(68, 'rezz', 'VVIP', 0, 0, '2026-05-07 18:17:31', 24, 'LANGSUNG', 'SELESAI', '2026-05-07 11:17:31', 'Tunai', NULL, NULL, 1, 58),
(69, 'rezz', 'VVIP', 1, 35000, '2026-05-07 18:19:17', 25, 'VV-80079', 'BELUM_DIPAKAI', NULL, 'Tunai', NULL, '2026-05-14 00:00:00', 0, 60),
(70, 'rezz', 'VVIP', 1, 35000, '2026-05-07 18:19:41', 0, 'VV-11124', 'BELUM_DIPAKAI', NULL, 'Tunai', NULL, NULL, 0, 60),
(71, 'rezz', 'VVIP', 10, 350000, '2026-05-07 18:20:10', 0, 'VV-50770', 'BELUM_DIPAKAI', NULL, 'QRIS', NULL, NULL, 0, 600),
(72, 'user', 'VVIP', 10, 350000, '2026-05-07 18:21:52', 30, 'VV-35168', 'EXPIRED', '2026-05-07 11:30:00', 'QRIS', NULL, '2026-05-07 18:30:00', 0, 600),
(73, 'user', 'VVIP', 1, 35000, '2026-05-07 18:22:57', 29, 'VV-86222', 'EXPIRED', '2026-05-07 11:40:00', 'Tunai', NULL, '2026-05-07 18:40:00', 0, 60),
(74, 'user', 'VVIP', 4, 140000, '2026-05-07 18:23:33', 29, 'LANGSUNG', 'AKTIF', '2026-05-07 11:23:33', 'QRIS', NULL, NULL, 1, 240),
(75, 'mid22', 'User Baru', 0, 50000, '2026-05-07 18:27:01', 0, NULL, 'SELESAI', NULL, 'QRIS', NULL, NULL, 1, 0),
(76, 'mid22', 'VVIP', 100, 3500000, '2026-05-07 18:27:52', 0, 'VV-97116', 'BELUM_DIPAKAI', NULL, 'QRIS', NULL, NULL, 0, 6000),
(77, 'mid22', 'Reguler', 1, 15000, '2026-05-07 18:28:53', 6, 'LANGSUNG', 'SELESAI', '2026-05-07 11:28:53', 'Tunai', NULL, NULL, 1, 60),
(78, 'rezz', 'VVIP', 0, 0, '2026-05-07 18:40:51', 23, 'LANGSUNG', 'SELESAI', '2026-05-07 11:40:51', 'Tunai', NULL, NULL, 1, 54),
(79, 'rezz', 'Reguler', 0, 0, '2026-05-07 18:41:11', 5, 'LANGSUNG', 'SELESAI', '2026-05-07 11:41:11', 'Tunai', NULL, NULL, 1, 53),
(80, 'rezz', 'Reguler', 1, 15000, '2026-05-07 18:41:28', 6, 'LANGSUNG', 'SELESAI', '2026-05-07 11:41:28', 'Tunai', NULL, NULL, 1, 60),
(81, 'rezz', 'VVIP', 0, 0, '2026-05-07 18:41:51', 21, 'LANGSUNG', 'SELESAI', '2026-05-07 11:41:51', 'Tunai', NULL, NULL, 1, 59),
(82, 'rezz', 'VVIP', 1, 35000, '2026-05-07 18:42:18', 23, 'TKN-MBR-VV-19234', 'SELESAI', '2026-05-07 11:42:58', 'QRIS', NULL, NULL, 1, 60),
(83, 'user', 'VVIP', 1, 35000, '2026-05-07 18:46:33', 26, 'BKG-USR-VV-89226', 'EXPIRED', '2026-05-07 11:47:00', 'Tunai', NULL, '2026-05-07 18:47:00', 0, 60),
(84, 'rezz', 'VIP', 1, 25000, '2026-05-07 18:47:41', 16, 'BKG-MBR-V-70469', 'SELESAI', '2026-05-07 11:48:00', 'Tunai', NULL, '2026-05-07 18:48:00', 0, 60),
(85, 'rezz', 'VVIP', 3, 0, '2026-05-07 18:49:32', 27, 'LANGSUNG', 'SELESAI', '2026-05-07 11:49:32', 'Tunai', NULL, NULL, 1, 210),
(86, 'rezz', 'VVIP', 3, 0, '2026-05-07 18:54:50', 23, 'LANGSUNG', 'SELESAI', '2026-05-07 11:54:50', 'Tunai', NULL, NULL, 1, 204),
(87, 'rezz', 'VIP', 1, 25000, '2026-05-07 19:03:29', 17, 'BKG-MBR-V-79755', 'SELESAI', '2026-05-07 12:04:00', 'Tunai', NULL, '2026-05-07 19:05:00', 1, 60),
(88, 'rezz', 'VIP', 1, 25000, '2026-05-07 19:04:35', 11, 'BKG-MBR-V-51228', 'SELESAI', '2026-05-07 12:06:18', 'Tunai', NULL, '2026-05-07 19:06:00', 1, 60),
(89, 'rezz', 'VIP', 1, 25000, '2026-05-07 19:06:44', 19, 'BKG-MBR-V-31832', 'SELESAI', '2026-05-07 12:07:43', 'Tunai', NULL, '2026-05-07 19:07:00', 1, 60),
(90, 'user', 'VVIP', 1, 35000, '2026-05-07 19:09:04', 27, 'BKG-USR-VV-53419', 'BELUM_DIPAKAI', NULL, 'Tunai', NULL, '2026-05-08 19:08:00', 0, 60),
(91, 'rezz', 'VIP', 10, 250000, '2026-05-07 19:39:17', 18, 'LANGSUNG', 'AKTIF', '2026-05-07 12:39:17', 'Tunai', NULL, NULL, 1, 600),
(92, 'vian', 'User Baru', 0, 50000, '2026-05-07 20:18:06', 0, NULL, 'SELESAI', NULL, 'QRIS', NULL, NULL, 1, 0);

-- --------------------------------------------------------

--
-- Struktur dari tabel `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password` varchar(50) DEFAULT NULL,
  `role` varchar(20) DEFAULT NULL,
  `saved_time_minutes` int(11) DEFAULT 0,
  `saved_time_reguler` int(11) DEFAULT 0,
  `saved_time_vip` int(11) DEFAULT 0,
  `saved_time_vvip` int(11) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data untuk tabel `users`
--

INSERT INTO `users` (`id`, `username`, `password`, `role`, `saved_time_minutes`, `saved_time_reguler`, `saved_time_vip`, `saved_time_vvip`) VALUES
(1, 'admin', 'admin123', 'ADMIN', 0, 0, 0, 0),
(2, 'user', 'user123', 'USER', 0, 0, 0, 0),
(16, 'flavv', 'flavv123', 'USER', 0, 0, 0, 0),
(17, 'flav', 'flavv123', 'USER', 0, 0, 0, 0),
(18, 'rezz', 'rez123', 'MEMBER', 0, 0, 0, 158),
(19, 'mid22', 'mid22', 'MEMBER', 59, 0, 0, 0),
(20, 'vian', 'vian', 'MEMBER', 0, 0, 0, 0);

-- --------------------------------------------------------

--
-- Stand-in struktur untuk tampilan `v_laporan_transaksi`
-- (Lihat di bawah untuk tampilan aktual)
--
CREATE TABLE `v_laporan_transaksi` (
`id` int(11)
,`customer_name` varchar(100)
,`pc_type` varchar(50)
,`pc_id` int(11)
,`duration` int(11)
,`biaya_sewa` double
,`payment_method` varchar(20)
,`status` varchar(20)
,`created_at` datetime
,`total_belanja_makanan` double
,`grand_total` double
);

-- --------------------------------------------------------

--
-- Stand-in struktur untuk tampilan `v_menu_makanan`
-- (Lihat di bawah untuk tampilan aktual)
--
CREATE TABLE `v_menu_makanan` (
`id` int(11)
,`name` varchar(100)
,`price` double
);

-- --------------------------------------------------------

--
-- Stand-in struktur untuk tampilan `v_menu_minuman`
-- (Lihat di bawah untuk tampilan aktual)
--
CREATE TABLE `v_menu_minuman` (
`id` int(11)
,`name` varchar(100)
,`price` double
);

-- --------------------------------------------------------

--
-- Stand-in struktur untuk tampilan `v_pc_tersedia`
-- (Lihat di bawah untuk tampilan aktual)
--
CREATE TABLE `v_pc_tersedia` (
`id` int(11)
,`status` varchar(20)
);

-- --------------------------------------------------------

--
-- Stand-in struktur untuk tampilan `v_pendapatan_per_payment`
-- (Lihat di bawah untuk tampilan aktual)
--
CREATE TABLE `v_pendapatan_per_payment` (
`payment_method` varchar(20)
,`jumlah_transaksi` bigint(21)
,`total_pendapatan` double
);

-- --------------------------------------------------------

--
-- Stand-in struktur untuk tampilan `v_rekap_pelanggan`
-- (Lihat di bawah untuk tampilan aktual)
--
CREATE TABLE `v_rekap_pelanggan` (
`customer_name` varchar(100)
,`total_transaksi` bigint(21)
,`total_bayar` double
,`total_jam_main` decimal(32,0)
,`transaksi_terakhir` datetime
);

-- --------------------------------------------------------

--
-- Struktur untuk view `v_laporan_transaksi`
--
DROP TABLE IF EXISTS `v_laporan_transaksi`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_laporan_transaksi`  AS SELECT `t`.`id` AS `id`, `t`.`customer_name` AS `customer_name`, `t`.`pc_type` AS `pc_type`, `t`.`pc_id` AS `pc_id`, `t`.`duration` AS `duration`, `t`.`total_amount` AS `biaya_sewa`, `t`.`payment_method` AS `payment_method`, `t`.`status` AS `status`, `t`.`created_at` AS `created_at`, coalesce(sum(`o`.`subtotal`),0) AS `total_belanja_makanan`, `t`.`total_amount`+ coalesce(sum(`o`.`subtotal`),0) AS `grand_total` FROM (`transactions` `t` left join `orders` `o` on(`t`.`id` = `o`.`transaction_id`)) GROUP BY `t`.`id` ;

-- --------------------------------------------------------

--
-- Struktur untuk view `v_menu_makanan`
--
DROP TABLE IF EXISTS `v_menu_makanan`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_menu_makanan`  AS SELECT `menu`.`id` AS `id`, `menu`.`name` AS `name`, `menu`.`price` AS `price` FROM `menu` WHERE `menu`.`type` = 'Makanan' ORDER BY `menu`.`price` ASC ;

-- --------------------------------------------------------

--
-- Struktur untuk view `v_menu_minuman`
--
DROP TABLE IF EXISTS `v_menu_minuman`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_menu_minuman`  AS SELECT `menu`.`id` AS `id`, `menu`.`name` AS `name`, `menu`.`price` AS `price` FROM `menu` WHERE `menu`.`type` = 'Minuman' ORDER BY `menu`.`price` ASC ;

-- --------------------------------------------------------

--
-- Struktur untuk view `v_pc_tersedia`
--
DROP TABLE IF EXISTS `v_pc_tersedia`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_pc_tersedia`  AS SELECT `pcs`.`id` AS `id`, `pcs`.`status` AS `status` FROM `pcs` WHERE `pcs`.`status` = 'TERSEDIA' ORDER BY `pcs`.`id` ASC ;

-- --------------------------------------------------------

--
-- Struktur untuk view `v_pendapatan_per_payment`
--
DROP TABLE IF EXISTS `v_pendapatan_per_payment`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_pendapatan_per_payment`  AS SELECT `transactions`.`payment_method` AS `payment_method`, count(0) AS `jumlah_transaksi`, sum(`transactions`.`total_amount`) AS `total_pendapatan` FROM `transactions` WHERE `transactions`.`status` = 'SELESAI' GROUP BY `transactions`.`payment_method` ORDER BY sum(`transactions`.`total_amount`) DESC ;

-- --------------------------------------------------------

--
-- Struktur untuk view `v_rekap_pelanggan`
--
DROP TABLE IF EXISTS `v_rekap_pelanggan`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `v_rekap_pelanggan`  AS SELECT `transactions`.`customer_name` AS `customer_name`, count(0) AS `total_transaksi`, sum(`transactions`.`total_amount`) AS `total_bayar`, sum(`transactions`.`duration`) AS `total_jam_main`, max(`transactions`.`created_at`) AS `transaksi_terakhir` FROM `transactions` WHERE `transactions`.`status` = 'SELESAI' GROUP BY `transactions`.`customer_name` ORDER BY sum(`transactions`.`total_amount`) DESC ;

--
-- Indexes for dumped tables
--

--
-- Indeks untuk tabel `komputer`
--
ALTER TABLE `komputer`
  ADD PRIMARY KEY (`id_pc`);

--
-- Indeks untuk tabel `menu`
--
ALTER TABLE `menu`
  ADD PRIMARY KEY (`id`);

--
-- Indeks untuk tabel `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`id`),
  ADD KEY `transaction_id` (`transaction_id`);

--
-- Indeks untuk tabel `pcs`
--
ALTER TABLE `pcs`
  ADD PRIMARY KEY (`id`);

--
-- Indeks untuk tabel `transactions`
--
ALTER TABLE `transactions`
  ADD PRIMARY KEY (`id`);

--
-- Indeks untuk tabel `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT untuk tabel yang dibuang
--

--
-- AUTO_INCREMENT untuk tabel `komputer`
--
ALTER TABLE `komputer`
  MODIFY `id_pc` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT untuk tabel `menu`
--
ALTER TABLE `menu`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT untuk tabel `orders`
--
ALTER TABLE `orders`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=63;

--
-- AUTO_INCREMENT untuk tabel `transactions`
--
ALTER TABLE `transactions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=93;

--
-- AUTO_INCREMENT untuk tabel `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- Ketidakleluasaan untuk tabel pelimpahan (Dumped Tables)
--

--
-- Ketidakleluasaan untuk tabel `orders`
--
ALTER TABLE `orders`
  ADD CONSTRAINT `orders_ibfk_1` FOREIGN KEY (`transaction_id`) REFERENCES `transactions` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
