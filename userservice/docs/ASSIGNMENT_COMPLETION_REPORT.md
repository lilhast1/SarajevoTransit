# Izvjestaj o realizaciji zadatka (User Service)

## 1. Kontekst i cilj

Ovaj dokument opisuje kako je implementiran zadatak:

- Napraviti web servise koji ne zahtijevaju komunikaciju sa drugim mikroservisima.
- Primijeniti DTO pristup tamo gdje ima smisla.
- Dokumentovati servise i priloziti dokaz testiranja (uspjesan i neuspjesan request-response).
- Implementirati validaciju modela.
- Implementirati testove za web servise i service sloj.
- Provjeriti da repozitorijumski upiti nemaju N+1 problem koristeci Hibernate statistics.
- Implementirati uniforman error handling kroz aplikaciju.

Opseg ovog izvjestaja je userservice modul.

## 2. Web servisi bez poziva drugih mikroservisa

### Sta je implementirano

Implementirana su dva glavna REST kontrolera:

- User profile API: [../src/main/java/com/sarajevotransit/userservice/controller/UserController.java](../src/main/java/com/sarajevotransit/userservice/controller/UserController.java)
- Loyalty API: [../src/main/java/com/sarajevotransit/userservice/controller/LoyaltyController.java](../src/main/java/com/sarajevotransit/userservice/controller/LoyaltyController.java)

Servisi rade lokalno nad vlastitom bazom i internim repository slojem:

- [../src/main/java/com/sarajevotransit/userservice/service/UserService.java](../src/main/java/com/sarajevotransit/userservice/service/UserService.java)
- [../src/main/java/com/sarajevotransit/userservice/service/LoyaltyService.java](../src/main/java/com/sarajevotransit/userservice/service/LoyaltyService.java)

### Kako je implementirano

- Endpointi su izlozeni preko @RestController i mapirani pod /api/users i /api/v1/users varijantama.
- CRUD i domena operacije (travel history, ticket purchases, loyalty earn/redeem) izvrsavaju se direktno kroz JPA repozitorije.
- Za kreiranje resursa koristi se HTTP 201 Created sa Location headerom.
- List endpointi imaju paginaciju i sortiranje (page, size, sort).

### Zasto je ovako implementirano

- Arhitektura je namjerno self-contained za userservice: manja sprega, jednostavniji deploy, lakse testiranje.
- Ovim je ispunjen zahtjev da servis ne zavisi od komunikacije sa drugim mikroservisima.

Napomena: U userservice kodu nema RestTemplate/WebClient/Feign klijenata za pozive drugih servisa.

## 3. DTO sloj (Request/Response modeli)

### Sta je implementirano

Implementiran je pun DTO sloj kroz request/response record klase, npr:

- [../src/main/java/com/sarajevotransit/userservice/dto/CreateUserRequest.java](../src/main/java/com/sarajevotransit/userservice/dto/CreateUserRequest.java)
- [../src/main/java/com/sarajevotransit/userservice/dto/UserProfileResponse.java](../src/main/java/com/sarajevotransit/userservice/dto/UserProfileResponse.java)
- [../src/main/java/com/sarajevotransit/userservice/dto/LoyaltyEarnRequest.java](../src/main/java/com/sarajevotransit/userservice/dto/LoyaltyEarnRequest.java)
- [../src/main/java/com/sarajevotransit/userservice/dto/LoyaltyBalanceResponse.java](../src/main/java/com/sarajevotransit/userservice/dto/LoyaltyBalanceResponse.java)

Mapiranje model -> DTO je implementirano eksplicitno u service sloju:

- [../src/main/java/com/sarajevotransit/userservice/service/UserService.java](../src/main/java/com/sarajevotransit/userservice/service/UserService.java)

### Kako je implementirano

- Ulazni JSON se mapira u Request DTO (record) uz Bean Validation anotacije.
- Izlaz ka klijentu uvijek ide preko Response DTO, a ne direktno preko JPA entiteta.
- Mapping metode (toUserProfileResponse, toTravelHistoryResponse, itd.) centralizuju transformaciju podataka.

### Zasto je ovako implementirano

- DTO sloj odvaja API ugovor od baze i interne domenske logike.
- Smanjuje rizik od slucajnog izlaganja osjetljivih ili nepotrebnih polja.
- Rucno mapiranje je izabrano radi pune kontrole i citljivosti (u ovom opsegu nije bila neophodna dodatna biblioteka poput ModelMapper/MapStruct).

## 4. Dokumentacija servisa i Postman dokaz

### Sta je implementirano

Dokumentacija i API test dokazi su organizovani u docs folderu:

- Postman kolekcija: [postman/userservice.postman_collection.json](postman/userservice.postman_collection.json)
- Screenshot evidencija: [postman-screenshots/README.md](postman-screenshots/README.md)
- JSON request-response evidencija:
  - [api-test-evidence/user-create-success.json](api-test-evidence/user-create-success.json)
  - [api-test-evidence/user-create-failure.json](api-test-evidence/user-create-failure.json)
  - [api-test-evidence/loyalty-earn-success.json](api-test-evidence/loyalty-earn-success.json)
  - [api-test-evidence/loyalty-redeem-failure.json](api-test-evidence/loyalty-redeem-failure.json)

Screenshot fajlovi:

- [postman-screenshots/user-create-success.png](postman-screenshots/user-create-success.png)
- [postman-screenshots/user-create-failure.png](postman-screenshots/user-create-failure.png)
- [postman-screenshots/loyalty-earn-success.png](postman-screenshots/loyalty-earn-success.png)
- [postman-screenshots/loyalty-redeem-failure.png](postman-screenshots/loyalty-redeem-failure.png)

### Kako je implementirano

- Za svaki kljucni endpoint sacuvan je primjer uspjesnog i neuspjesnog poziva.
- Uspjesni scenariji pokrivaju validne ulaze i expected HTTP 201/200 odgovore.
- Neuspjesni scenariji pokrivaju invalidan payload ili poslovno pravilo (npr. nedovoljan loyalty balance).

### Zasto je ovako implementirano

- Dokumentacija je prakticna i auditabilna: kolekcija + screenshot + json dokaz.
- Omogucava brzo reproduciranje i evaluaciju API ponasanja.

## 5. Validacija modela

### Sta je implementirano

Validacija je implementirana na vise nivoa:

- DTO validacija preko anotacija i @Valid:
  - [../src/main/java/com/sarajevotransit/userservice/dto/CreateUserRequest.java](../src/main/java/com/sarajevotransit/userservice/dto/CreateUserRequest.java)
  - [../src/main/java/com/sarajevotransit/userservice/dto/LoyaltyEarnRequest.java](../src/main/java/com/sarajevotransit/userservice/dto/LoyaltyEarnRequest.java)
- Parametarska validacija (path/query) preko @Validated i @Positive/@Min/@Max u kontrolerima:
  - [../src/main/java/com/sarajevotransit/userservice/controller/UserController.java](../src/main/java/com/sarajevotransit/userservice/controller/UserController.java)
  - [../src/main/java/com/sarajevotransit/userservice/controller/LoyaltyController.java](../src/main/java/com/sarajevotransit/userservice/controller/LoyaltyController.java)
- Entitetska validacija preko anotacija u model klasama:
  - [../src/main/java/com/sarajevotransit/userservice/model/UserProfile.java](../src/main/java/com/sarajevotransit/userservice/model/UserProfile.java)
  - [../src/main/java/com/sarajevotransit/userservice/model/TravelHistoryEntry.java](../src/main/java/com/sarajevotransit/userservice/model/TravelHistoryEntry.java)

### Kako i zasto

- Fail-fast validacija stiti servis od loseg inputa prije ulaska u business logiku.
- Standardizovane poruke validacije olaksavaju klijentsku obradu gresaka.

## 6. Testovi web servisa

### Sta je implementirano

Controller/integration testovi sa MockMvc:

- [../src/test/java/com/sarajevotransit/userservice/controller/UserControllerIntegrationTests.java](../src/test/java/com/sarajevotransit/userservice/controller/UserControllerIntegrationTests.java)
- [../src/test/java/com/sarajevotransit/userservice/controller/LoyaltyControllerIntegrationTests.java](../src/test/java/com/sarajevotransit/userservice/controller/LoyaltyControllerIntegrationTests.java)
- [../src/test/java/com/sarajevotransit/userservice/controller/WebServiceIntegrationTests.java](../src/test/java/com/sarajevotransit/userservice/controller/WebServiceIntegrationTests.java)

### Kako je implementirano

- Pokriveni su success i failure slucajevi.
- Verifikuju se HTTP statusi, response payload i paginacija.
- Testovi rade nad Spring contextom (@SpringBootTest) i test profilom.

### Zasto

- Validira kompletan web sloj (routing, validacija, serialization, exception mapping).

## 7. Testovi service klasa

### Sta je implementirano

Mockito unit testovi:

- [../src/test/java/com/sarajevotransit/userservice/service/UserServiceTest.java](../src/test/java/com/sarajevotransit/userservice/service/UserServiceTest.java)
- [../src/test/java/com/sarajevotransit/userservice/service/LoyaltyServiceTest.java](../src/test/java/com/sarajevotransit/userservice/service/LoyaltyServiceTest.java)

### Kako i zasto

- Mockuju se repository zavisnosti radi izolacije business logike.
- Pokrivene su domenske grane: duplicate email, normalization, earning/redeeming points, insufficient points, fallback logika za suggestions.
- Obezbjedjuju stabilan feedback i brze regresione provjere.

## 8. N+1 provjera sa Hibernate statistics

### Sta je implementirano

- U test konfiguraciji omogucen hibernate statistics:
  - [../src/test/resources/application.properties](../src/test/resources/application.properties)
- N+1 performance testovi:
  - [../src/test/java/com/sarajevotransit/userservice/repository/RepositoryQueryPerformanceTests.java](../src/test/java/com/sarajevotransit/userservice/repository/RepositoryQueryPerformanceTests.java)
- Fetch optimizacija nad korisnickim upitom:
  - [../src/main/java/com/sarajevotransit/userservice/repository/UserProfileRepository.java](../src/main/java/com/sarajevotransit/userservice/repository/UserProfileRepository.java)

### Kako je implementirano

- Test koristi SessionFactory statistics i broji prepare statement count.
- Za kriticne upite postavljeni su pragovi (<=2 ili <=1), cime se hvata eventualni N+1 regresioni scenario.
- UserProfile upiti koriste EntityGraph za eager fetch wallet/preference relacija.

### Zasto

- Performance kontrola je uvedena kao testabilno pravilo, ne samo manualna provjera.

## 9. Uniforman error handling

### Sta je implementirano

Globalni handler:

- [../src/main/java/com/sarajevotransit/userservice/exception/GlobalExceptionHandler.java](../src/main/java/com/sarajevotransit/userservice/exception/GlobalExceptionHandler.java)

Payload format:

- [../src/main/java/com/sarajevotransit/userservice/dto/ApiErrorResponse.java](../src/main/java/com/sarajevotransit/userservice/dto/ApiErrorResponse.java)

### Kako je implementirano

- Greske se mapiraju na konzistentan odgovor sa poljima:
  - timestamp
  - status
  - error
  - message
  - path
  - validationErrors
- Pokriveni su tipicni slucajevi: validation, malformed JSON, not found, conflict, business bad request, fallback 500.
- Za neocekivane greske vraca se bezbjedna poruka bez stack trace detalja.

### Zasto

- Uniformnost olaksava frontend obradu i observability.
- Izbjegava se izlaganje internih detalja sistema.

## 10. Dodatno: real PostgreSQL verifikacija i bootstrap

### Sta je implementirano

- Default datasource za runtime i postgres-it test profil ide na localhost:5432/userdb:
  - [../src/main/resources/application.properties](../src/main/resources/application.properties)
  - [../src/test/resources/application-postgres-it.properties](../src/test/resources/application-postgres-it.properties)
- Bootstrap konfiguracija za provjeru/kreiranje baze prije inicijalizacije datasource-a:
  - [../src/main/java/com/sarajevotransit/userservice/config/PostgresDatabaseBootstrapConfig.java](../src/main/java/com/sarajevotransit/userservice/config/PostgresDatabaseBootstrapConfig.java)
- Seed podaci pri praznoj bazi:
  - [../src/main/java/com/sarajevotransit/userservice/config/DataSeeder.java](../src/main/java/com/sarajevotransit/userservice/config/DataSeeder.java)
- Real DB integracioni testovi:
  - [../src/test/java/com/sarajevotransit/userservice/integration/PostgresUserDbIntegrationTests.java](../src/test/java/com/sarajevotransit/userservice/integration/PostgresUserDbIntegrationTests.java)

### Zasto

- Omogucava validaciju da aplikacija radi i van H2 test in-memory setupa.
- Priblizava testiranje produkcijskom okruzenju.

## 11. Rezime zavrsenosti po stavkama zadatka

1. Web servisi bez komunikacije sa drugim mikroservisima: zavrseno.
2. DTO implementacija: zavrseno, uz eksplicitno mapiranje u service sloju.
3. Dokumentacija + uspjesan/neuspjesan Postman dokaz: zavrseno (kolekcija, screenshoti, JSON evidencija).
4. Validacija modela: zavrseno (DTO + kontroler + model nivo).
5. Testovi web servisa: zavrseno (MockMvc integration).
6. Testovi service klasa: zavrseno (Mockito unit testovi).
7. N+1 provjera sa Hibernate statistics: zavrseno (automatski assertion testovi).
8. Uniforman error handling: zavrseno (globalni, strukturiran i konzistentan format).

## 12. Test artefakti (Surefire)

Generisani test izvjestaji su dostupni u:

- [../target/surefire-reports](../target/surefire-reports)

Kljucni izvjestaji:

- [../target/surefire-reports/TEST-com.sarajevotransit.userservice.controller.UserControllerIntegrationTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.userservice.controller.UserControllerIntegrationTests.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.userservice.controller.LoyaltyControllerIntegrationTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.userservice.controller.LoyaltyControllerIntegrationTests.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.userservice.service.UserServiceTest.xml](../target/surefire-reports/TEST-com.sarajevotransit.userservice.service.UserServiceTest.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.userservice.service.LoyaltyServiceTest.xml](../target/surefire-reports/TEST-com.sarajevotransit.userservice.service.LoyaltyServiceTest.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.userservice.repository.RepositoryQueryPerformanceTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.userservice.repository.RepositoryQueryPerformanceTests.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.userservice.integration.PostgresUserDbIntegrationTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.userservice.integration.PostgresUserDbIntegrationTests.xml)

## 13. Pokrivenost dodatnih zahtjeva iz assignment-a

### 13.1 PATCH metoda (JSON Patch stil)

- Endpoint:
  - [../src/main/java/com/sarajevotransit/userservice/controller/UserController.java](../src/main/java/com/sarajevotransit/userservice/controller/UserController.java)
- Service logika:
  - [../src/main/java/com/sarajevotransit/userservice/service/UserService.java](../src/main/java/com/sarajevotransit/userservice/service/UserService.java)

Implementiran je PATCH endpoint za parcijalnu izmjenu profila korisnika putem JSON Patch operacija (`add`, `replace`, `remove`) nad poljima `/fullName` i `/email`, uz validaciju rezultata i 409 provjeru jedinstvenosti email-a.

### 13.2 Paginacija i sortiranje

Paginacija/sort su dostupni na list endpointima kroz `page`, `size`, `sort` parametre i `Page<T>` odgovore:

- User list, travel history, ticket purchases, loyalty transactions.
- Implementacija helpera:
  - [../src/main/java/com/sarajevotransit/userservice/service/PaginationUtils.java](../src/main/java/com/sarajevotransit/userservice/service/PaginationUtils.java)

### 13.3 Custom upiti (nisu generisani derivacijom)

Custom JPQL upiti su implementirani u repozitorijima:

- Line usage statistika:
  - [../src/main/java/com/sarajevotransit/userservice/repository/TravelHistoryRepository.java](../src/main/java/com/sarajevotransit/userservice/repository/TravelHistoryRepository.java)
- Ownership check za delete:
  - [../src/main/java/com/sarajevotransit/userservice/repository/TravelHistoryRepository.java](../src/main/java/com/sarajevotransit/userservice/repository/TravelHistoryRepository.java)
- Ticket purchase agregacija (group by ticketType):
  - [../src/main/java/com/sarajevotransit/userservice/repository/TicketPurchaseHistoryRepository.java](../src/main/java/com/sarajevotransit/userservice/repository/TicketPurchaseHistoryRepository.java)

### 13.4 Batch unos

- Endpoint za batch travel history insert:
  - [../src/main/java/com/sarajevotransit/userservice/controller/UserController.java](../src/main/java/com/sarajevotransit/userservice/controller/UserController.java)
- Transakcijska batch logika:
  - [../src/main/java/com/sarajevotransit/userservice/service/UserService.java](../src/main/java/com/sarajevotransit/userservice/service/UserService.java)

Implementiran je unos liste stavki (`/travel-history/batch`) sa rollback ponasanjem ako bilo koja stavka ne prodje.

### 13.5 Koristenje Entity Grapha

EntityGraph optimizacija je prisutna u user upitima:

- [../src/main/java/com/sarajevotransit/userservice/repository/UserProfileRepository.java](../src/main/java/com/sarajevotransit/userservice/repository/UserProfileRepository.java)

Time se izbjegava N+1 pri ucitavanju korisnika sa wallet/preference relacijama.

### 13.6 Transakcijske metode servisa (vise repository poziva)

Primjeri transakcijskih metoda sa vise repository operacija:

- Loyalty earn/redeem:
  - [../src/main/java/com/sarajevotransit/userservice/service/LoyaltyService.java](../src/main/java/com/sarajevotransit/userservice/service/LoyaltyService.java)
- Batch travel insert i patch/update operacije:
  - [../src/main/java/com/sarajevotransit/userservice/service/UserService.java](../src/main/java/com/sarajevotransit/userservice/service/UserService.java)

### 13.7 Najmanje jedna DELETE metoda

Implementiran je ownership-safe delete endpoint:

- [../src/main/java/com/sarajevotransit/userservice/controller/UserController.java](../src/main/java/com/sarajevotransit/userservice/controller/UserController.java)
- [../src/main/java/com/sarajevotransit/userservice/service/UserService.java](../src/main/java/com/sarajevotransit/userservice/service/UserService.java)

Brisanje je dostupno na `/api/v1/users/{userId}/travel-history/{entryId}` i vraca `204 NO CONTENT`.
