-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: localhost    Database: defaultdb
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `dokument`
--

DROP TABLE IF EXISTS `dokument`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dokument` (
  `id_dokument` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(100) DEFAULT NULL,
  `datum_upload` date DEFAULT NULL,
  `prijavaid_prijava` int DEFAULT NULL,
  `vrsta_dokumentaid_vrsta` int DEFAULT NULL,
  `broj_bodova` double NOT NULL,
  `isdostavljen` tinyint(1) DEFAULT NULL,
  `dokumentb64` longtext,
  PRIMARY KEY (`id_dokument`),
  KEY `prijavaid_prijava` (`prijavaid_prijava`),
  KEY `vrsta_dokumentaid_vrsta` (`vrsta_dokumentaid_vrsta`),
  CONSTRAINT `dokument_ibfk_1` FOREIGN KEY (`prijavaid_prijava`) REFERENCES `prijava` (`id_prijava`),
  CONSTRAINT `dokument_ibfk_2` FOREIGN KEY (`vrsta_dokumentaid_vrsta`) REFERENCES `vrsta_dokumenta` (`id_vrsta`)
) ENGINE=InnoDB AUTO_INCREMENT=524 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `kategorija_sobe`
--

DROP TABLE IF EXISTS `kategorija_sobe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `kategorija_sobe` (
  `id_kategorija` int NOT NULL AUTO_INCREMENT,
  `sifra` varchar(30) NOT NULL,
  `naziv` varchar(60) NOT NULL,
  `boja` varchar(7) DEFAULT NULL,
  `za_studente` tinyint(1) NOT NULL DEFAULT '1',
  `opis` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_kategorija`),
  UNIQUE KEY `sifra` (`sifra`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `korisnik`
--

DROP TABLE IF EXISTS `korisnik`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `korisnik` (
  `id_korisnik` int NOT NULL AUTO_INCREMENT,
  `ime` varchar(50) DEFAULT NULL,
  `prezime` varchar(50) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL,
  `password_hash` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `ulogaid_uloga` int DEFAULT NULL,
  `email` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL,
  `zadnja_prijava` timestamp NULL DEFAULT NULL,
  `studentid_student` int DEFAULT NULL,
  `reset_token_hash` varchar(255) DEFAULT NULL,
  `reset_token_istek` timestamp NULL DEFAULT NULL,
  `reset_pokusaji` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_korisnik`),
  UNIQUE KEY `uq_korisnik_username` (`username`),
  UNIQUE KEY `uq_korisnik_email` (`email`),
  KEY `ulogaid_uloga` (`ulogaid_uloga`),
  KEY `korisnik_student_fk` (`studentid_student`),
  CONSTRAINT `korisnik_ibfk_1` FOREIGN KEY (`ulogaid_uloga`) REFERENCES `uloga` (`id_uloga`),
  CONSTRAINT `korisnik_student_fk` FOREIGN KEY (`studentid_student`) REFERENCES `student` (`id_student`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `oznaka_stanara`
--

DROP TABLE IF EXISTS `oznaka_stanara`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oznaka_stanara` (
  `id_oznaka` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(60) NOT NULL,
  `boja` varchar(7) NOT NULL,
  `opis` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_oznaka`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prijava`
--

DROP TABLE IF EXISTS `prijava`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prijava` (
  `id_prijava` int NOT NULL AUTO_INCREMENT,
  `datum_prijave` date DEFAULT NULL,
  `ukupni_bodovi` double DEFAULT NULL,
  `napomena` varchar(500) DEFAULT NULL,
  `studentid_student2` int DEFAULT NULL,
  `status_prijaveid_status` int DEFAULT NULL,
  `akademska_godina` int DEFAULT NULL,
  `prosjek` decimal(4,2) DEFAULT NULL,
  `godina_studija` int DEFAULT NULL,
  `socijalni_status_id_status` int DEFAULT NULL,
  `udaljenost_km` decimal(6,2) DEFAULT NULL,
  `broj_clanova_domacinstva` int DEFAULT NULL,
  `ukupna_primanja` decimal(10,2) DEFAULT NULL,
  `polozeni_ispiti` int DEFAULT NULL,
  `uspjeh_verifikovan` tinyint(1) NOT NULL DEFAULT '0',
  `primanja_verifikovana` tinyint(1) NOT NULL DEFAULT '0',
  `udaljenost_verifikovana` tinyint(1) NOT NULL DEFAULT '0',
  `napomena_administratora` varchar(500) DEFAULT NULL,
  `upis_godine_put` int DEFAULT NULL COMMENT '1 = prvi put upisuje tekucu godinu, 2 = drugi put...',
  `obnavljao_godinu` tinyint(1) DEFAULT NULL COMMENT 'Pitanje 7: da li je ikada obnavljao godinu',
  `obnovljene_godine` varchar(100) DEFAULT NULL COMMENT 'Pitanje 7: koje akademske godine',
  `dokaz_uslova_at` datetime DEFAULT NULL,
  `dokaz_uslova_evidentirao` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_prijava`),
  UNIQUE KEY `uq_prijava_student_godina` (`studentid_student2`,`akademska_godina`),
  KEY `studentid_student2` (`studentid_student2`),
  KEY `status_prijaveid_status` (`status_prijaveid_status`),
  KEY `prijava_soc_status_fk` (`socijalni_status_id_status`),
  CONSTRAINT `prijava_ibfk_1` FOREIGN KEY (`studentid_student2`) REFERENCES `student` (`id_student`),
  CONSTRAINT `prijava_ibfk_2` FOREIGN KEY (`status_prijaveid_status`) REFERENCES `status_prijave` (`id_status`),
  CONSTRAINT `prijava_soc_status_fk` FOREIGN KEY (`socijalni_status_id_status`) REFERENCES `socijalni_status` (`id_status`),
  CONSTRAINT `chk_clanovi` CHECK (((`broj_clanova_domacinstva` is null) or (`broj_clanova_domacinstva` >= 1)))
) ENGINE=InnoDB AUTO_INCREMENT=198 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `prijava_kriterij`
--

DROP TABLE IF EXISTS `prijava_kriterij`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `prijava_kriterij` (
  `id_prijava_kriterij` int NOT NULL AUTO_INCREMENT,
  `prijavaid_prijava` int NOT NULL,
  `vrsta_kriterijaid` int NOT NULL,
  `parametar` decimal(10,2) DEFAULT NULL,
  `verifikovan` tinyint(1) NOT NULL DEFAULT '0',
  `datum_od` date DEFAULT NULL,
  `datum_do` date DEFAULT NULL,
  PRIMARY KEY (`id_prijava_kriterij`),
  UNIQUE KEY `uq_prijava_kriterij` (`prijavaid_prijava`,`vrsta_kriterijaid`),
  KEY `pk_vrsta_fk` (`vrsta_kriterijaid`),
  CONSTRAINT `pk_prijava_fk` FOREIGN KEY (`prijavaid_prijava`) REFERENCES `prijava` (`id_prijava`) ON DELETE CASCADE,
  CONSTRAINT `pk_vrsta_fk` FOREIGN KEY (`vrsta_kriterijaid`) REFERENCES `vrsta_kriterija` (`id_vrsta_kriterija`)
) ENGINE=InnoDB AUTO_INCREMENT=41 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `smjestaj`
--

DROP TABLE IF EXISTS `smjestaj`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `smjestaj` (
  `id_smjestaj` int NOT NULL AUTO_INCREMENT,
  `soba_id` int NOT NULL,
  `prijava_id` int NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PREDLOZEN',
  `datum_useljenja` date DEFAULT NULL,
  `datum_iseljenja` date DEFAULT NULL,
  `oznaka_id` int DEFAULT NULL,
  `kljucevi_preuzeti` int NOT NULL DEFAULT '0',
  `kljucevi_vraceni` int NOT NULL DEFAULT '0',
  `razlog` varchar(255) DEFAULT NULL,
  `napomena` varchar(255) DEFAULT NULL,
  `kartica_preuzeta` date DEFAULT NULL,
  `kartica_razduzena` date DEFAULT NULL,
  `ljekarsko_predano` date DEFAULT NULL,
  `uvjerenje_fakultet_predano` date DEFAULT NULL,
  PRIMARY KEY (`id_smjestaj`),
  KEY `fk_smjestaj_soba` (`soba_id`),
  KEY `fk_smjestaj_prijava` (`prijava_id`),
  KEY `fk_smjestaj_oznaka` (`oznaka_id`),
  CONSTRAINT `fk_smjestaj_oznaka` FOREIGN KEY (`oznaka_id`) REFERENCES `oznaka_stanara` (`id_oznaka`),
  CONSTRAINT `fk_smjestaj_prijava` FOREIGN KEY (`prijava_id`) REFERENCES `prijava` (`id_prijava`),
  CONSTRAINT `fk_smjestaj_soba` FOREIGN KEY (`soba_id`) REFERENCES `soba` (`id_soba`)
) ENGINE=InnoDB AUTO_INCREMENT=52 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `soba`
--

DROP TABLE IF EXISTS `soba`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `soba` (
  `id_soba` int NOT NULL AUTO_INCREMENT,
  `broj` int NOT NULL,
  `sprat_id` int NOT NULL,
  `kapacitet` int NOT NULL,
  `kategorija_id` int NOT NULL,
  `spol` varchar(1) DEFAULT NULL,
  `broj_kljuceva` int NOT NULL DEFAULT '1',
  `aktivna` tinyint(1) NOT NULL DEFAULT '1',
  `napomena` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_soba`),
  UNIQUE KEY `broj` (`broj`),
  KEY `fk_soba_sprat` (`sprat_id`),
  KEY `fk_soba_kategorija` (`kategorija_id`),
  CONSTRAINT `fk_soba_kategorija` FOREIGN KEY (`kategorija_id`) REFERENCES `kategorija_sobe` (`id_kategorija`),
  CONSTRAINT `fk_soba_sprat` FOREIGN KEY (`sprat_id`) REFERENCES `sprat` (`id_sprat`),
  CONSTRAINT `ck_soba_kapacitet` CHECK ((`kapacitet` between 1 and 4))
) ENGINE=InnoDB AUTO_INCREMENT=128 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `socijalni_status`
--

DROP TABLE IF EXISTS `socijalni_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `socijalni_status` (
  `id_status` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_status`),
  UNIQUE KEY `naziv` (`naziv`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `sprat`
--

DROP TABLE IF EXISTS `sprat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sprat` (
  `id_sprat` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(40) NOT NULL,
  `redoslijed` int NOT NULL,
  `spol` varchar(1) DEFAULT NULL,
  PRIMARY KEY (`id_sprat`),
  UNIQUE KEY `naziv` (`naziv`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `status_prijave`
--

DROP TABLE IF EXISTS `status_prijave`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `status_prijave` (
  `id_status` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_status`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `student`
--

DROP TABLE IF EXISTS `student`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `student` (
  `id_student` int NOT NULL AUTO_INCREMENT,
  `ime` varchar(50) DEFAULT NULL,
  `prezime` varchar(50) DEFAULT NULL,
  `broj_indeksa` varchar(20) DEFAULT NULL,
  `fakultet` varchar(100) DEFAULT NULL,
  `godina_studija` int DEFAULT NULL,
  `prosjek` decimal(4,2) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `telefon` varchar(100) DEFAULT NULL,
  `socijalni_status_id_status` int DEFAULT NULL,
  `adresa` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `ime_roditelja` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `jmbg` varchar(13) NOT NULL,
  `spol` char(1) DEFAULT NULL,
  `broj_licne_karte` varchar(20) DEFAULT NULL,
  `telefon_roditelja` varchar(30) DEFAULT NULL,
  `kanton` varchar(60) DEFAULT NULL,
  `opcina` varchar(60) DEFAULT NULL,
  `napomena` varchar(1000) DEFAULT NULL,
  `sifra_broj` int DEFAULT NULL,
  PRIMARY KEY (`id_student`),
  UNIQUE KEY `uq_student_jmbg` (`jmbg`),
  UNIQUE KEY `sifra_broj` (`sifra_broj`),
  KEY `socijalni_status_id_status` (`socijalni_status_id_status`),
  KEY `idx_student_broj_indeksa` (`broj_indeksa`),
  CONSTRAINT `student_ibfk_1` FOREIGN KEY (`socijalni_status_id_status`) REFERENCES `socijalni_status` (`id_status`)
) ENGINE=InnoDB AUTO_INCREMENT=181 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `uloga`
--

DROP TABLE IF EXISTS `uloga`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `uloga` (
  `id_uloga` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id_uloga`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vrsta_dokumenta`
--

DROP TABLE IF EXISTS `vrsta_dokumenta`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vrsta_dokumenta` (
  `id_vrsta` int NOT NULL AUTO_INCREMENT,
  `naziv` varchar(100) DEFAULT NULL,
  `kategorija` varchar(30) NOT NULL DEFAULT 'Ostalo',
  `sifra` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id_vrsta`),
  UNIQUE KEY `uq_vrsta_dokumenta_sifra` (`sifra`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `vrsta_kriterija`
--

DROP TABLE IF EXISTS `vrsta_kriterija`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `vrsta_kriterija` (
  `id_vrsta_kriterija` int NOT NULL AUTO_INCREMENT,
  `sifra` varchar(50) NOT NULL,
  `naziv` varchar(200) NOT NULL,
  `bodovi` decimal(6,2) DEFAULT NULL,
  `trazi_parametar` tinyint(1) NOT NULL DEFAULT '0',
  `naziv_parametra` varchar(100) DEFAULT NULL,
  `grupa` varchar(50) NOT NULL,
  `aktivan` tinyint(1) NOT NULL DEFAULT '1',
  `dokaz_vrsta_id` int DEFAULT NULL,
  `trazi_period` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id_vrsta_kriterija`),
  UNIQUE KEY `sifra` (`sifra`),
  KEY `fk_kriterij_dokaz` (`dokaz_vrsta_id`),
  CONSTRAINT `fk_kriterij_dokaz` FOREIGN KEY (`dokaz_vrsta_id`) REFERENCES `vrsta_dokumenta` (`id_vrsta`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zahtjev_promjene_sobe`
--

DROP TABLE IF EXISTS `zahtjev_promjene_sobe`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zahtjev_promjene_sobe` (
  `id_zahtjev` int NOT NULL AUTO_INCREMENT,
  `prijava_id` int NOT NULL,
  `vrsta` varchar(20) NOT NULL,
  `sa_studentom` varchar(100) DEFAULT NULL,
  `zeljena_soba` int DEFAULT NULL,
  `razlog` varchar(500) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'NA_CEKANJU',
  `datum_podnosenja` datetime NOT NULL,
  `datum_odgovora` datetime DEFAULT NULL,
  `odgovor` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_zahtjev`),
  KEY `fk_zahtjev_prijava` (`prijava_id`),
  CONSTRAINT `fk_zahtjev_prijava` FOREIGN KEY (`prijava_id`) REFERENCES `prijava` (`id_prijava`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `zelja_smjestaja`
--

DROP TABLE IF EXISTS `zelja_smjestaja`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `zelja_smjestaja` (
  `id_zelja` int NOT NULL AUTO_INCREMENT,
  `prijava_id` int NOT NULL,
  `zeljeni_kapacitet` int DEFAULT NULL,
  `soba_id` int DEFAULT NULL,
  `cimer1` varchar(100) DEFAULT NULL,
  `cimer2` varchar(100) DEFAULT NULL,
  `napomena` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_zelja`),
  UNIQUE KEY `prijava_id` (`prijava_id`),
  KEY `fk_zelja_soba` (`soba_id`),
  CONSTRAINT `fk_zelja_prijava` FOREIGN KEY (`prijava_id`) REFERENCES `prijava` (`id_prijava`),
  CONSTRAINT `fk_zelja_soba` FOREIGN KEY (`soba_id`) REFERENCES `soba` (`id_soba`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-01 12:54:20
