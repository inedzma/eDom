# E-Dom

Informacioni sistem za prijavu i smještaj studenata u studentski dom
Studentskog centra Univerziteta u Zenici.

Projekat je izrađen u okviru diplomskog rada iz predmeta *Metode razvoja softvera*.

## Dijelovi sistema

| Modul | Opis |
|---|---|
| `edom-server` | Spring Boot server: web aplikacija za studente i javne stranice, te REST API za desktop aplikaciju |
| `edom-desktop` | JavaFX aplikacija za administraciju Studentskog centra |
| `edom-common` | zajedničke klase (DTO) koje koriste server i desktop |

**Studenti** (web): registracija, online prijava na konkurs, prilaganje dokumenata,
praćenje statusa i bodova, želje za smještaj i zahtjev za promjenu sobe.

**Administracija** (desktop): pregled i bodovanje prijava, ručni unos papirnih prijava,
rang lista, raspored u sobe (ručno ili prijedlogom algoritma), useljenje i iseljenje,
ugovori i reversi, postavke konkursa.

## Preduslovi

- Java 25
- MySQL 8
- Maven (samo za izgradnju iz izvornog koda)

## Instalacija

### 1. Baza podataka

Napravite praznu bazu i u nju učitajte početnu strukturu:

```sql
CREATE DATABASE edom CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
```

```bash
mysql -u root -p edom < instalacija/edom-baza.sql
```

(U MySQL Workbenchu: odaberite bazu `edom`, *File → Open SQL Script* → `edom-baza.sql` → *Execute*.)

Skripta pravi sve tabele, šifarnike (statuse, kriterije, vrste dokumenata, spratove i sobe)
i jedan administratorski nalog. Ne sadrži nikakve podatke o studentima.

### 2. Postavke servera

Kopirajte `edom-server/src/main/resources/application.properties.example`
u `application.properties` u istom folderu i upišite:

- adresu, korisničko ime i lozinku baze,
- e-mail nalog za slanje obavijesti (za Gmail je potrebna *lozinka aplikacije*),
- tajni ključ za JWT (`edom.jwt.secret`), najmanje 32 znaka.

Fajl `application.properties` se ne postavlja na GitHub jer sadrži lozinke.

### 3. Folder `data`

Server čuva postavke konkursa, tekstove javne stranice, galeriju i obavještenja u folderu
`edom-server/data`. Šabloni ugovora i reversa već se nalaze u `edom-server/data/obrasci`.

Kopirajte u `edom-server/data` početne fajlove:

```
instalacija/postavke.properties     ->  edom-server/data/postavke.properties
instalacija/web-tekstovi.properties ->  edom-server/data/web-tekstovi.properties
```

### 4. Pokretanje servera

Iz IntelliJ IDEA: pokrenite klasu `EdomServerApplication`.

Ili iz komandne linije, iz foldera `edom-server` (da bi server pronašao folder `data`):

```bash
mvn clean package
java -jar target/edom-server-*.jar
```

Web aplikacija je dostupna na **http://localhost:8080**.

### 5. Desktop aplikacija

Prvo pokrenite server, zatim iz IntelliJ IDEA pokrenite klasu
`org.example.edom.EDomApplication` iz modula `edom-desktop`.

Desktop se po defaultu spaja na server na adresi `http://localhost:8080`.
Ako server radi na drugom računaru, adresa se zadaje pri pokretanju opcijom:

    -Dedom.api.url=https://adresa-servera

(u IntelliJ IDEA: *Run → Edit Configurations → VM options*;
iz komandne linije: `java -Dedom.api.url=https://adresa-servera -jar edom-desktop.jar`).

## Prva prijava

| Korisničko ime | Lozinka       |
|--------------|---------------|
| `admin`      | `Admin.2026!` |

**Odmah nakon prve prijave promijenite lozinku i e-mail adresu administratora.**

Zatim u desktop aplikaciji, u *Postavkama*, podesite akademsku godinu, rokove konkursa,
brojeve odluka i podatke o direktoru. Galerija, obavještenja i tekstovi javne stranice
uređuju se na web stranici nakon prijave kao administrator.

## Testovi

```bash
mvn test
```

Integracijski testovi koriste bazu iz `application.properties`; svaki test se izvršava
u transakciji koja se na kraju poništava, pa podaci u bazi ostaju nepromijenjeni.

## Nadogradnja na novu verziju

Ako nova verzija mijenja strukturu baze, uz nju dolazi SQL skripta. Postupak:

1. napraviti rezervnu kopiju baze i foldera `data`,
2. pokrenuti SQL skriptu iz napomene uz verziju,
3. zamijeniti jar fajl i ponovo pokrenuti server.

## Zaštita podataka

Sistem obrađuje lične podatke studenata (JMBG, podaci o domaćinstvu, priloženi dokumenti).
Bazu i folder `data` iz stvarne upotrebe nikada ne postavljati u repozitorij.

## Autor

- Nedžma Imamović, Politehnički fakultet, Univerzitet u Zenici
- Mentor: Doc. dr. sci. Mujo Hodžić