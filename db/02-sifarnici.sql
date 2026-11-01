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
-- Dumping data for table `kategorija_sobe`
--

LOCK TABLES `kategorija_sobe` WRITE;
/*!40000 ALTER TABLE `kategorija_sobe` DISABLE KEYS */;
INSERT INTO `kategorija_sobe` VALUES (1,'STUDENTSKA','Studentska soba',NULL,1,NULL),(2,'GOSTINJSKA','Soba za goste','#FF0000',0,'Crveni brojevi 1–4 u tabeli; u 2023/24 upisani GOSTI.'),(3,'LJUBICASTA','Ljubičasta oznaka (provjeriti značenje)','#7030A0',1,'Ljubičasti brojevi 25–30 i čajna kuhinja II sprata.');
/*!40000 ALTER TABLE `kategorija_sobe` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `oznaka_stanara`
--

LOCK TABLES `oznaka_stanara` WRITE;
/*!40000 ALTER TABLE `oznaka_stanara` DISABLE KEYS */;
INSERT INTO `oznaka_stanara` VALUES (1,'Zelena (provjeriti značenje)','#92D050','Najčešća boja imena u 2024/25.'),(2,'Žuta (provjeriti značenje)','#FFFF00',NULL),(3,'Roza (provjeriti značenje)','#FF0066',NULL),(4,'Ljubičasta (provjeriti značenje)','#7030A0',NULL);
/*!40000 ALTER TABLE `oznaka_stanara` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `soba`
--

LOCK TABLES `soba` WRITE;
/*!40000 ALTER TABLE `soba` DISABLE KEYS */;
INSERT INTO `soba` VALUES (1,1,1,2,2,NULL,2,1,NULL),(2,2,1,2,2,NULL,2,1,NULL),(3,3,1,2,2,NULL,2,1,NULL),(4,4,1,2,2,NULL,2,1,NULL),(5,5,1,2,1,NULL,2,1,NULL),(6,6,1,2,1,NULL,2,1,NULL),(7,7,1,3,1,NULL,3,1,NULL),(8,8,1,3,1,NULL,3,1,NULL),(9,9,1,3,1,NULL,3,1,NULL),(10,10,1,3,1,NULL,3,1,NULL),(11,11,1,3,1,NULL,3,1,NULL),(12,12,1,3,1,NULL,3,1,NULL),(13,13,1,3,1,NULL,3,1,NULL),(14,14,1,3,1,NULL,3,1,NULL),(15,15,1,3,1,NULL,3,1,NULL),(16,16,1,2,1,NULL,2,1,NULL),(17,17,1,2,1,NULL,2,1,NULL),(18,18,1,2,1,NULL,2,1,NULL),(19,19,2,3,1,NULL,3,1,NULL),(20,20,2,3,1,NULL,3,1,NULL),(21,21,2,3,1,NULL,3,1,NULL),(22,22,2,3,1,NULL,3,1,NULL),(23,23,2,3,1,NULL,3,1,NULL),(24,24,2,3,1,NULL,3,1,NULL),(25,25,2,2,3,NULL,2,1,NULL),(26,26,2,2,3,NULL,2,1,NULL),(27,27,2,2,3,NULL,2,1,NULL),(28,28,2,2,3,NULL,2,1,NULL),(29,29,2,2,3,NULL,2,1,NULL),(30,30,2,2,3,NULL,2,1,NULL),(31,31,2,3,1,NULL,3,1,NULL),(32,32,2,3,1,NULL,3,1,NULL),(33,33,2,3,1,NULL,3,1,NULL),(34,34,2,3,1,NULL,3,1,NULL),(35,35,2,3,1,NULL,3,1,NULL),(36,36,2,3,1,NULL,3,1,NULL),(37,37,2,3,1,NULL,3,1,NULL),(38,38,2,3,1,NULL,3,1,NULL),(39,39,2,3,1,NULL,3,1,NULL),(40,40,3,2,1,NULL,2,1,NULL),(41,41,3,2,1,NULL,2,1,NULL),(42,42,3,2,1,NULL,2,1,NULL),(43,43,4,3,1,NULL,3,1,NULL),(44,44,4,3,1,NULL,3,1,NULL),(45,45,4,3,1,NULL,3,1,NULL),(46,46,4,3,1,NULL,3,1,NULL),(47,47,4,3,1,NULL,3,1,NULL),(48,48,4,3,1,NULL,3,1,NULL),(49,49,4,2,1,NULL,2,1,NULL),(50,50,4,2,1,NULL,2,1,NULL),(51,51,4,2,1,NULL,2,1,NULL),(52,52,4,2,1,NULL,2,1,NULL),(53,53,4,2,1,NULL,2,1,NULL),(54,54,4,2,1,NULL,2,1,NULL),(55,55,4,3,1,NULL,3,1,NULL),(56,56,4,3,1,NULL,3,1,NULL),(57,57,4,3,1,NULL,3,1,NULL),(58,58,4,3,1,NULL,3,1,NULL),(59,59,4,3,1,NULL,3,1,NULL),(60,60,4,3,1,NULL,3,1,NULL),(61,61,4,3,1,NULL,3,1,NULL),(62,62,4,3,1,NULL,3,1,NULL),(63,63,4,3,1,NULL,3,1,NULL),(64,64,5,2,1,NULL,2,1,NULL),(65,65,5,2,1,NULL,2,1,NULL),(66,66,5,2,1,NULL,2,1,NULL),(67,67,6,3,1,NULL,3,1,NULL),(68,68,6,3,1,NULL,3,1,NULL),(69,69,6,3,1,NULL,3,1,NULL),(70,70,6,3,1,NULL,3,1,NULL),(71,71,6,3,1,NULL,3,1,NULL),(72,72,6,3,1,NULL,3,1,NULL),(73,73,6,2,1,NULL,2,1,NULL),(74,74,6,2,1,NULL,2,1,NULL),(75,75,6,2,1,NULL,2,1,NULL),(76,76,6,2,1,NULL,2,1,NULL),(77,77,6,2,1,NULL,2,1,NULL),(78,78,6,2,1,NULL,2,1,NULL),(79,79,6,3,1,NULL,3,1,NULL),(80,80,6,3,1,NULL,3,1,NULL),(81,81,6,3,1,NULL,3,1,NULL),(82,82,6,3,1,NULL,3,1,NULL),(83,83,6,3,1,NULL,3,1,NULL),(84,84,6,3,1,NULL,3,1,NULL),(85,85,6,3,1,NULL,3,1,NULL),(86,86,6,3,1,NULL,3,1,NULL),(87,87,6,3,1,NULL,3,1,NULL);
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
INSERT INTO `sprat` VALUES (1,'I sprat',1,'M'),(2,'II sprat',2,'M'),(3,'II međusprat',3,'M'),(4,'III sprat',4,'Z'),(5,'III međusprat',5,'Z'),(6,'IV sprat',6,'Z');
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
INSERT INTO `vrsta_dokumenta` VALUES (1,'uplatnica','Osnovni dokumenti','UPLATNICA'),(5,'Potvrda o primanjima','Domaćinstvo','CLAN_PRIMANJA'),(6,'CIPS','Osnovni dokumenti','CIPS'),(7,'Svjedodzba o zavrsetku srednje skole','Uspjeh','SVJEDODZBA'),(8,'Uvjerenje o položenim ispitima','Uspjeh','UVJERENJE_ISPITI'),(9,'Ovjerena kopija indeksa sa ocjenama','Uspjeh','KOPIJA_INDEKSA'),(10,'Dokaz o osvojenim nagradama','Uspjeh','NAGRADE'),(11,'Lice na birou rada: potvrda sa biroa','Domaćinstvo','CLAN_BIRO'),(12,'Lice koje nije zaposleno: ovjerena kopija zdravstvene knjižice','Domaćinstvo','CLAN_NEZAPOSLEN'),(13,'Lice koje pohađa školu ili fakultet: potvrda iz škole ili fakulteta','Domaćinstvo','CLAN_SKOLA'),(14,'Malodobno dijete do polaska u školu: ovjerena kopija zdravstvene knjižice','Domaćinstvo','CLAN_DIJETE'),(15,'Lice koje je u radnom odnosu: tri zadnje platne liste','Domaćinstvo','CLAN_RADNI'),(16,'Lice koje je u penziji: tri zadnja platna čeka','Domaćinstvo','CLAN_PENZIJA'),(17,'Prijavni obrazac','Osnovni dokumenti','PRIJAVNI_OBRAZAC'),(18,'Uvjerenje o zajedničkom domaćinstvu','Domaćinstvo','KUCNA_LISTA'),(19,'Potvrda o stepenu invalidnosti','Dodatni kriteriji','INVALIDNOST'),(20,'Potvrda o poginulim, nestalim i umrlim članovima uže porodice','Dodatni kriteriji','POGINULI'),(21,'Potvrda o logoraškom statusu','Dodatni kriteriji','LOGORAS'),(22,'Potvrda o učešću roditelja u OS RBiH','Dodatni kriteriji','OSRBIH'),(23,'Potvrda o izbjegličkom statusu','Dodatni kriteriji','IZBJEGLICA'),(24,'Ovjerena kopija ID i PDV broja kompanije','Domaćinstvo','GARANT_FIRMA'),(25,'Ovjerena garancija','Osnovni dokumenti','GARANCIJA'),(26,'Garant zaposlen: potvrda o stalnom radnom odnosu i tri posljednje platne liste','Osnovni dokumenti','GARANT_RADNI'),(27,'Garant penzioner: tri posljednja čeka od penzije i rješenje o penzionisanju','Osnovni dokumenti','GARANT_PENZIJA'),(28,'Uvjerenje o redovnom studiranju','Osnovni dokumenti','UVJERENJE_STUDIRANJE');
/*!40000 ALTER TABLE `vrsta_dokumenta` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `vrsta_kriterija`
--

LOCK TABLES `vrsta_kriterija` WRITE;
/*!40000 ALTER TABLE `vrsta_kriterija` DISABLE KEYS */;
INSERT INTO `vrsta_kriterija` VALUES (1,'POGINULI_BRANILAC','Dijete šehida ili poginulog branioca',50.00,0,NULL,'BRANIOCI',1,20,0),(2,'CLAN_POR_SEHIDA','Član uže porodice šehida (brat ili sestra)',20.00,0,NULL,'BRANIOCI',1,20,0),(3,'BEZ_OBA_RODITELJA','Bez oba roditelja',20.00,0,NULL,'BRANIOCI',1,NULL,0),(4,'BEZ_STARATELJA','Bez roditeljskog staranja',20.00,0,NULL,'BRANIOCI',1,NULL,0),(5,'BEZ_JEDNOG_RODIT','Bez jednog roditelja',15.00,0,NULL,'BRANIOCI',1,NULL,0),(6,'INVALIDNOST','Ostala invalidnost studenta',10.00,0,NULL,'BRANIOCI',1,19,0),(7,'NOSILAC_PRIZNANJA','Dijete nosioca ratnog priznanja',10.00,1,'Broj ratnih priznanja','BRANIOCI',1,NULL,0),(8,'STUDENT_LOGORAS','Student sa logoraškim statusom',10.00,0,NULL,'BRANIOCI',1,21,0),(9,'SOCIJALNA_POMOC','Korisnik socijalne pomoći',10.00,0,NULL,'BRANIOCI',1,NULL,0),(10,'RODITELJ_LOGORAS','Roditelj sa logoraškim statusom',5.00,0,NULL,'BRANIOCI',1,21,0),(11,'STUDENT_RVI','Student RVI',NULL,1,'Postotak invalidnosti (%)','BRANIOCI',1,19,0),(12,'INVALID_RODITELJA','Invalidnost roditelja ili staratelja',NULL,1,'Postotak invalidnosti (%)','BRANIOCI',1,19,0),(13,'DIJETE_OSRBIH','Dijete učesnika OS RBiH',NULL,0,'Period učešća (od – do)','BRANIOCI',1,22,1),(14,'IZBJEGLICA','Izbjegli i raseljeni student',3.00,0,NULL,'DODATNI',1,23,0),(15,'BRAT_SESTRA','Brat ili sestra studiraju',2.00,0,NULL,'DODATNI',1,NULL,0),(16,'NAGRADE','Osvojene nagrade u toku školovanja',3.00,0,NULL,'DODATNI',1,10,0);
/*!40000 ALTER TABLE `vrsta_kriterija` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-01 13:01:18
