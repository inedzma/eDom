-- =====================================================================
--  E-Dom: početna baza podataka (MySQL 8)
--
--  Sadrži strukturu svih tabela, šifarnike (uloge, statuse prijave,
--  vrste kriterija i dokumenata, socijalne statuse, spratove i sobe)
--  i jedan administratorski nalog. Ne sadrži nikakve podatke o studentima.
--
--  Upotreba:
--    1. Napravite praznu bazu, npr.:
--         CREATE DATABASE edom CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
--    2. Pokrenite ovaj fajl nad tom bazom:
--         mysql -u root -p edom < edom-baza.sql
--       (ili u MySQL Workbenchu: odaberite bazu, File > Open SQL Script, Execute)
--
--  Početni administrator:
--    korisničko ime:  admin
--    lozinka:         Admin.2026!
--  Odmah nakon prve prijave promijenite lozinku i e-mail adresu administratora.
-- =====================================================================

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- =====================================================================
--  1. STRUKTURA
-- =====================================================================

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
  `isdostavljen` tinyint(1) DEFAULT NULL,
  `dokumentb64` longtext,
  PRIMARY KEY (`id_dokument`),
  KEY `prijavaid_prijava` (`prijavaid_prijava`),
  KEY `vrsta_dokumentaid_vrsta` (`vrsta_dokumentaid_vrsta`),
  CONSTRAINT `dokument_ibfk_1` FOREIGN KEY (`prijavaid_prijava`) REFERENCES `prijava` (`id_prijava`),
  CONSTRAINT `dokument_ibfk_2` FOREIGN KEY (`vrsta_dokumentaid_vrsta`) REFERENCES `vrsta_dokumenta` (`id_vrsta`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  PRIMARY KEY (`id_prijava_kriterij`),
  UNIQUE KEY `uq_prijava_kriterij` (`prijavaid_prijava`,`vrsta_kriterijaid`),
  KEY `pk_vrsta_fk` (`vrsta_kriterijaid`),
  CONSTRAINT `pk_prijava_fk` FOREIGN KEY (`prijavaid_prijava`) REFERENCES `prijava` (`id_prijava`) ON DELETE CASCADE,
  CONSTRAINT `pk_vrsta_fk` FOREIGN KEY (`vrsta_kriterijaid`) REFERENCES `vrsta_kriterija` (`id_vrsta_kriterija`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  CONSTRAINT `fk_smjestaj_prijava` FOREIGN KEY (`prijava_id`) REFERENCES `prijava` (`id_prijava`),
  CONSTRAINT `fk_smjestaj_soba` FOREIGN KEY (`soba_id`) REFERENCES `soba` (`id_soba`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `za_studente` tinyint(1) NOT NULL DEFAULT '1',
  `spol` varchar(1) DEFAULT NULL,
  `broj_kljuceva` int NOT NULL DEFAULT '1',
  `aktivna` tinyint(1) NOT NULL DEFAULT '1',
  `napomena` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_soba`),
  UNIQUE KEY `broj` (`broj`),
  KEY `fk_soba_sprat` (`sprat_id`),
  CONSTRAINT `fk_soba_sprat` FOREIGN KEY (`sprat_id`) REFERENCES `sprat` (`id_sprat`),
  CONSTRAINT `ck_soba_kapacitet` CHECK ((`kapacitet` between 1 and 4))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  PRIMARY KEY (`id_sprat`),
  UNIQUE KEY `naziv` (`naziv`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `broj_indeksa` varchar(30) DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
  `datum_izmjene` datetime DEFAULT NULL,
  PRIMARY KEY (`id_zelja`),
  UNIQUE KEY `prijava_id` (`prijava_id`),
  KEY `fk_zelja_soba` (`soba_id`),
  CONSTRAINT `fk_zelja_prijava` FOREIGN KEY (`prijava_id`) REFERENCES `prijava` (`id_prijava`),
  CONSTRAINT `fk_zelja_soba` FOREIGN KEY (`soba_id`) REFERENCES `soba` (`id_soba`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

-- =====================================================================
--  2. ŠIFARNICI
-- =====================================================================

--
-- Dumping data for table `soba`
--

LOCK TABLES `soba` WRITE;
/*!40000 ALTER TABLE `soba` DISABLE KEYS */;
INSERT INTO `soba` VALUES (1,1,1,2,0,NULL,2,1,NULL),(2,2,1,2,0,NULL,2,1,NULL),(3,3,1,2,0,NULL,2,1,NULL),(4,4,1,2,0,NULL,2,1,NULL),(5,5,1,2,1,NULL,2,1,NULL),(6,6,1,2,1,NULL,2,1,NULL),(7,7,1,3,1,NULL,3,1,NULL),(8,8,1,3,1,NULL,3,1,NULL),(9,9,1,3,1,NULL,3,1,NULL),(10,10,1,3,1,NULL,3,1,NULL),(11,11,1,3,1,NULL,3,1,NULL),(12,12,1,3,1,NULL,3,1,NULL),(13,13,1,3,1,NULL,3,1,NULL),(14,14,1,3,1,NULL,3,1,NULL),(15,15,1,3,1,NULL,3,1,NULL),(16,16,1,2,1,NULL,2,1,NULL),(17,17,1,2,1,NULL,2,1,NULL),(18,18,1,2,1,NULL,2,1,NULL),(19,19,2,3,1,NULL,3,1,NULL),(20,20,2,3,1,NULL,3,1,NULL),(21,21,2,3,1,NULL,3,1,NULL),(22,22,2,3,1,NULL,3,1,NULL),(23,23,2,3,1,NULL,3,1,NULL),(24,24,2,3,1,NULL,3,1,NULL),(25,25,2,2,1,NULL,2,1,NULL),(26,26,2,2,1,NULL,2,1,NULL),(27,27,2,2,1,NULL,2,1,NULL),(28,28,2,2,1,NULL,2,1,NULL),(29,29,2,2,1,NULL,2,1,NULL),(30,30,2,2,1,NULL,2,1,NULL),(31,31,2,3,1,NULL,3,1,NULL),(32,32,2,3,1,NULL,3,1,NULL),(33,33,2,3,1,NULL,3,1,NULL),(34,34,2,3,1,NULL,3,1,NULL),(35,35,2,3,1,NULL,3,1,NULL),(36,36,2,3,1,NULL,3,1,NULL),(37,37,2,3,1,NULL,3,1,NULL),(38,38,2,3,1,NULL,3,1,NULL),(39,39,2,3,1,NULL,3,1,NULL),(40,40,3,2,1,NULL,2,1,NULL),(41,41,3,2,1,NULL,2,1,NULL),(42,42,3,2,1,NULL,2,1,NULL),(43,43,4,3,1,NULL,3,1,NULL),(44,44,4,3,1,NULL,3,1,NULL),(45,45,4,3,1,NULL,3,1,NULL),(46,46,4,3,1,NULL,3,1,NULL),(47,47,4,3,1,NULL,3,1,NULL),(48,48,4,3,1,NULL,3,1,NULL),(49,49,4,2,1,NULL,2,1,NULL),(50,50,4,2,1,NULL,2,1,NULL),(51,51,4,2,1,NULL,2,1,NULL),(52,52,4,2,1,NULL,2,1,NULL),(53,53,4,2,1,NULL,2,1,NULL),(54,54,4,2,1,NULL,2,1,NULL),(55,55,4,3,1,NULL,3,1,NULL),(56,56,4,3,1,NULL,3,1,NULL),(57,57,4,3,1,NULL,3,1,NULL),(58,58,4,3,1,NULL,3,1,NULL),(59,59,4,3,1,NULL,3,1,NULL),(60,60,4,3,1,NULL,3,1,NULL),(61,61,4,3,1,NULL,3,1,NULL),(62,62,4,3,1,NULL,3,1,NULL),(63,63,4,3,1,NULL,3,1,NULL),(64,64,5,2,1,NULL,2,1,NULL),(65,65,5,2,1,NULL,2,1,NULL),(66,66,5,2,1,NULL,2,1,NULL),(67,67,6,3,1,NULL,3,1,NULL),(68,68,6,3,1,NULL,3,1,NULL),(69,69,6,3,1,NULL,3,1,NULL),(70,70,6,3,1,NULL,3,1,NULL),(71,71,6,3,1,NULL,3,1,NULL),(72,72,6,3,1,NULL,3,1,NULL),(73,73,6,2,1,NULL,2,1,NULL),(74,74,6,2,1,NULL,2,1,NULL),(75,75,6,2,1,NULL,2,1,NULL),(76,76,6,2,1,NULL,2,1,NULL),(77,77,6,2,1,NULL,2,1,NULL),(78,78,6,2,1,NULL,2,1,NULL),(79,79,6,3,1,NULL,3,1,NULL),(80,80,6,3,1,NULL,3,1,NULL),(81,81,6,3,1,NULL,3,1,NULL),(82,82,6,3,1,NULL,3,1,NULL),(83,83,6,3,1,NULL,3,1,NULL),(84,84,6,3,1,NULL,3,1,NULL),(85,85,6,3,1,NULL,3,1,NULL),(86,86,6,3,1,NULL,3,1,NULL),(87,87,6,3,1,NULL,3,1,NULL);
/*!40000 ALTER TABLE `soba` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `socijalni_status`
--

LOCK TABLES `socijalni_status` WRITE;
/*!40000 ALTER TABLE `socijalni_status` DISABLE KEYS */;
INSERT INTO `socijalni_status` VALUES (1,'Bez oba roditelja'),(4,'Dijete šehida'),(6,'Izbjeglica'),(2,'Jedan roditelj nezaposlen'),(5,'Korisnik socijalne pomoći'),(3,'Oba roditelja nezaposlena'),(7,'Ostalo');
/*!40000 ALTER TABLE `socijalni_status` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `sprat`
--

LOCK TABLES `sprat` WRITE;
/*!40000 ALTER TABLE `sprat` DISABLE KEYS */;
INSERT INTO `sprat` VALUES (1,'I sprat',1),(2,'II sprat',2),(3,'II međusprat',3),(4,'III sprat',4),(5,'III međusprat',5),(6,'IV sprat',6);
/*!40000 ALTER TABLE `sprat` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `status_prijave`
--

LOCK TABLES `status_prijave` WRITE;
/*!40000 ALTER TABLE `status_prijave` DISABLE KEYS */;
INSERT INTO `status_prijave` VALUES (1,'na pregledu'),(3,'zaključeno'),(4,'odobreno'),(5,'odbijeno'),(6,'u izradi'),(7,'na čekanju'),(8,'nepotpuna'),(9,'iseljen');
/*!40000 ALTER TABLE `status_prijave` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `uloga`
--

LOCK TABLES `uloga` WRITE;
/*!40000 ALTER TABLE `uloga` DISABLE KEYS */;
INSERT INTO `uloga` VALUES (2,'ADMIN'),(3,'Student');
/*!40000 ALTER TABLE `uloga` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `vrsta_dokumenta`
--

LOCK TABLES `vrsta_dokumenta` WRITE;
/*!40000 ALTER TABLE `vrsta_dokumenta` DISABLE KEYS */;
INSERT INTO `vrsta_dokumenta` VALUES (1,'Uplatnica','Osnovni dokumenti','UPLATNICA'),(5,'Potvrda o primanjima','Domaćinstvo','CLAN_PRIMANJA'),(6,'CIPS','Osnovni dokumenti','CIPS'),(7,'Svjedodžba o završetku srednje škole','Uspjeh','SVJEDODZBA'),(8,'Uvjerenje o položenim ispitima','Uspjeh','UVJERENJE_ISPITI'),(9,'Ovjerena kopija indeksa sa ocjenama','Uspjeh','KOPIJA_INDEKSA'),(10,'Dokaz o osvojenim nagradama','Uspjeh','NAGRADE'),(11,'Lice na birou rada: potvrda sa biroa','Domaćinstvo','CLAN_BIRO'),(12,'Lice koje nije zaposleno: ovjerena kopija zdravstvene knjižice','Domaćinstvo','CLAN_NEZAPOSLEN'),(13,'Lice koje pohađa školu ili fakultet: potvrda iz škole ili fakulteta','Domaćinstvo','CLAN_SKOLA'),(14,'Malodobno dijete do polaska u školu: ovjerena kopija zdravstvene knjižice','Domaćinstvo','CLAN_DIJETE'),(15,'Lice koje je u radnom odnosu: tri zadnje platne liste','Domaćinstvo','CLAN_RADNI'),(16,'Lice koje je u penziji: tri zadnja platna čeka','Domaćinstvo','CLAN_PENZIJA'),(17,'Prijavni obrazac','Osnovni dokumenti','PRIJAVNI_OBRAZAC'),(18,'Uvjerenje o zajedničkom domaćinstvu','Domaćinstvo','KUCNA_LISTA'),(19,'Potvrda o stepenu invalidnosti','Dodatni kriteriji','INVALIDNOST'),(20,'Potvrda o poginulim, nestalim i umrlim članovima uže porodice','Dodatni kriteriji','POGINULI'),(21,'Potvrda o logoraškom statusu','Dodatni kriteriji','LOGORAS'),(22,'Potvrda o učešću roditelja u OS RBiH','Dodatni kriteriji','OSRBIH'),(23,'Potvrda o izbjegličkom statusu','Dodatni kriteriji','IZBJEGLICA'),(24,'Ovjerena kopija ID i PDV broja kompanije','Domaćinstvo','GARANT_FIRMA'),(25,'Ovjerena garancija','Osnovni dokumenti','GARANCIJA'),(26,'Garant zaposlen: potvrda o stalnom radnom odnosu i tri posljednje platne liste','Osnovni dokumenti','GARANT_RADNI'),(27,'Garant penzioner: tri posljednja čeka od penzije i rješenje o penzionisanju','Osnovni dokumenti','GARANT_PENZIJA'),(28,'Uvjerenje o redovnom studiranju','Osnovni dokumenti','UVJERENJE_STUDIRANJE');
/*!40000 ALTER TABLE `vrsta_dokumenta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `vrsta_kriterija`
--

LOCK TABLES `vrsta_kriterija` WRITE;
/*!40000 ALTER TABLE `vrsta_kriterija` DISABLE KEYS */;
INSERT INTO `vrsta_kriterija` VALUES (1,'POGINULI_BRANILAC','Dijete šehida ili poginulog branioca',50.00,0,NULL,'BRANIOCI',1,20,0),(2,'CLAN_POR_SEHIDA','Član uže porodice šehida (brat ili sestra)',20.00,0,NULL,'BRANIOCI',1,20,0),(3,'BEZ_OBA_RODITELJA','Bez oba roditelja',20.00,0,NULL,'BRANIOCI',1,NULL,0),(4,'BEZ_STARATELJA','Bez roditeljskog staranja',20.00,0,NULL,'BRANIOCI',1,NULL,0),(5,'BEZ_JEDNOG_RODIT','Bez jednog roditelja',15.00,0,NULL,'BRANIOCI',1,NULL,0),(6,'INVALIDNOST','Ostala invalidnost studenta',10.00,0,NULL,'BRANIOCI',1,19,0),(7,'NOSILAC_PRIZNANJA','Dijete nosioca ratnog priznanja',10.00,1,'Broj ratnih priznanja','BRANIOCI',1,NULL,0),(8,'STUDENT_LOGORAS','Student sa logoraškim statusom',10.00,0,NULL,'BRANIOCI',1,21,0),(9,'SOCIJALNA_POMOC','Korisnik socijalne pomoći',10.00,0,NULL,'BRANIOCI',1,NULL,0),(10,'RODITELJ_LOGORAS','Roditelj sa logoraškim statusom',5.00,0,NULL,'BRANIOCI',1,21,0),(11,'STUDENT_RVI','Student RVI',NULL,1,'Postotak invalidnosti (%)','BRANIOCI',1,19,0),(12,'INVALID_RODITELJA','Invalidnost roditelja ili staratelja',NULL,1,'Postotak invalidnosti (%)','BRANIOCI',1,19,0),(13,'DIJETE_OSRBIH','Dijete učesnika OS RBiH',NULL,1,'Broj punih mjeseci učešća (oba roditelja zajedno)','BRANIOCI',1,22,1),(14,'IZBJEGLICA','Izbjegli i raseljeni student',3.00,0,NULL,'DODATNI',1,23,0),(15,'BRAT_SESTRA','Brat ili sestra studiraju',2.00,0,NULL,'DODATNI',1,NULL,0),(16,'NAGRADE','Osvojene nagrade u toku školovanja',3.00,0,NULL,'DODATNI',1,10,0);
/*!40000 ALTER TABLE `vrsta_kriterija` ENABLE KEYS */;
UNLOCK TABLES;

-- =====================================================================
--  3. POČETNI ADMINISTRATOR  (lozinka: Admin.2026!  -> promijeniti!)
-- =====================================================================

INSERT INTO `korisnik`
  (`ime`, `prezime`, `username`, `password_hash`, `ulogaid_uloga`, `email`, `reset_pokusaji`)
VALUES
  ('Administrator', 'Sistema', 'admin',
   '$2a$10$7AD0aDE4XWuYugqK07c/fuRXlb990XbKQ0UHKOA6LTzOGGO14OUXO',
   (SELECT `id_uloga` FROM `uloga` WHERE `naziv` = 'ADMIN'),
   'admin@example.com', 0);

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
