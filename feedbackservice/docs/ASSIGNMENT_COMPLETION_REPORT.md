# Izvjestaj o realizaciji zadatka (Feedback Service)

## 1. Kontekst i cilj

Ovaj dokument opisuje kako su realizovani zahtjevi:

- Napraviti web servise koji ne zahtijevaju komunikaciju sa drugim mikroservisima.
- Primijeniti DTO sloj gdje ima smisla (ModelMapper, MapStruct ili slicno).
- Dokumentovati svaki servis i priloziti test dokaz (uspjesan i neuspjesan request-response).
- Implementirati validaciju modela.
- Implementirati testove za web servise.
- Testirati service klase.
- Provjeriti N+1 problem kroz Hibernate statistics.
- Implementirati uniforman error handling.
- Uspostaviti konzistentnu strukturu paketa i koristiti Lombok za smanjenje boilerplate koda.

Opseg ovog izvjestaja je modul `feedbackservice`.

## 2. Pregled realizacije po zadacima

1. Web servisi bez komunikacije sa drugim mikroservisima: zavrseno.
2. DTO implementacija i mapiranje: zavrseno.
3. Dokumentacija + Postman screenshot/JSON dokaz: zavrseno.
4. Validacija modela i ulaza: zavrseno.
5. Testovi web sloja: zavrseno.
6. Testovi service sloja: zavrseno.
7. N+1 provjera sa Hibernate statistics: zavrseno.
8. Uniforman error handling: zavrseno.
9. Konzistentna struktura paketa + Lombok refaktor: zavrseno.

## 3. Web servisi bez komunikacije sa drugim mikroservisima

### Sta je implementirano

Implementirana su dva samostalna REST API servisa:

- Problem reports API: [../src/main/java/com/sarajevotransit/feedbackservice/controller/ProblemReportController.java](../src/main/java/com/sarajevotransit/feedbackservice/controller/ProblemReportController.java)
- Line reviews API: [../src/main/java/com/sarajevotransit/feedbackservice/controller/LineReviewController.java](../src/main/java/com/sarajevotransit/feedbackservice/controller/LineReviewController.java)

Glavni endpointi:

- `POST /api/v1/reports` (kreiranje prijave)
- `GET /api/v1/reports` (lista prijava, filter + paginacija)
- `GET /api/v1/reports/{id}`
- `GET /api/v1/reports/line/{lineId}` (paginacija)
- `PATCH /api/v1/reports/{id}/status`
- `POST /api/v1/reviews` (kreiranje recenzije)
- `GET /api/v1/reviews?lineId=...&includeHidden=...` (paginacija)
- `GET /api/v1/reviews/{id}`
- `GET /api/v1/reviews/reviewer/{reviewerUserId}` (paginacija)
- `PATCH /api/v1/reviews/{id}/moderation-status`
- `GET /api/v1/reviews/summary` i `GET /api/v1/reviews/summary/{lineId}`

### Kako je implementirano

- Kontroleri delegiraju poslovnu logiku na service sloj.
- Service sloj koristi samo lokalne JPA repozitorije:
  - [../src/main/java/com/sarajevotransit/feedbackservice/service/ProblemReportService.java](../src/main/java/com/sarajevotransit/feedbackservice/service/ProblemReportService.java)
  - [../src/main/java/com/sarajevotransit/feedbackservice/service/LineReviewService.java](../src/main/java/com/sarajevotransit/feedbackservice/service/LineReviewService.java)
- Podaci se cuvaju i citaju iskljucivo iz lokalne baze putem:
  - [../src/main/java/com/sarajevotransit/feedbackservice/repository/ProblemReportRepository.java](../src/main/java/com/sarajevotransit/feedbackservice/repository/ProblemReportRepository.java)
  - [../src/main/java/com/sarajevotransit/feedbackservice/repository/LineReviewRepository.java](../src/main/java/com/sarajevotransit/feedbackservice/repository/LineReviewRepository.java)
- Kreiranje resursa vraca `201 Created` sa `Location` headerom.
- List endpointi podrzavaju `page`, `size`, `sort` (Spring Data `Pageable`).

### Zasto je ovako implementirano

- Ispunjava se zahtjev samostalnog servisa bez zavisnosti od drugih mikroservisa.
- Arhitektura je cista i testabilna: controller -> service -> repository.
- REST standardi su ispostovani (`201 Created`, paginacija, validacija inputa).

### Dokaz da nema poziva ka drugim servisima

U kodu nema `RestTemplate`, `WebClient`, `FeignClient` niti OpenFeign konfiguracije u main kodu `feedbackservice` modula.

## 4. DTO sloj (sa ModelMapper)

### Sta je implementirano

Request/Response DTO klase su uvedene za API granicu:

- [../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateProblemReportRequest.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateProblemReportRequest.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/ProblemReportResponse.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/ProblemReportResponse.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateLineReviewRequest.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateLineReviewRequest.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/LineReviewResponse.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/LineReviewResponse.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/ReportStatusUpdateRequest.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/ReportStatusUpdateRequest.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/ReviewModerationRequest.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/ReviewModerationRequest.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/LineRatingSummaryResponse.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/LineRatingSummaryResponse.java)

Mapiranje je realizovano kroz:

- [../src/main/java/com/sarajevotransit/feedbackservice/mapper/ProblemReportMapper.java](../src/main/java/com/sarajevotransit/feedbackservice/mapper/ProblemReportMapper.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/mapper/LineReviewMapper.java](../src/main/java/com/sarajevotransit/feedbackservice/mapper/LineReviewMapper.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/config/ModelMapperConfig.java](../src/main/java/com/sarajevotransit/feedbackservice/config/ModelMapperConfig.java)

### Kako je implementirano

- Koristi se `ModelMapper` sa `STRICT` matching strategijom.
- Ulazni JSON se binduje na request DTO i validira preko Bean Validation anotacija.
- Entiteti nikada ne izlaze direktno ka klijentu; response ide preko response DTO objekata.
- Service sloj dodatno radi domain normalization (trim, enum normalizacija, poslovna pravila).

### Zasto je ovako implementirano

- DTO sloj odvaja API ugovor od persistence modela.
- Smanjuje coupling i rizik od izlaganja internih polja.
- Omogucava bezbjednije verzionisanje i jasniju kontrolu payload-a.

## 5. Dokumentacija servisa + Postman dokaz (uspjeh/neuspjeh)

### Sta je implementirano

- API dokumentacija: [../API_DOCUMENTATION.md](../API_DOCUMENTATION.md)
- Postman kolekcija: [postman/feedbackservice.postman_collection.json](postman/feedbackservice.postman_collection.json)
- Screenshot evidencija: [postman-screenshots/README.md](postman-screenshots/README.md)
- Uspjesan/neuspjesan request-response JSON dokaz:
  - [api-test-evidence/report-success.json](api-test-evidence/report-success.json)
  - [api-test-evidence/report-failure.json](api-test-evidence/report-failure.json)
  - [api-test-evidence/review-success.json](api-test-evidence/review-success.json)
  - [api-test-evidence/review-failure.json](api-test-evidence/review-failure.json)
- Screenshot fajlovi:
  - [postman-screenshots/report-success.png](postman-screenshots/report-success.png)
  - [postman-screenshots/report-failure.png](postman-screenshots/report-failure.png)
  - [postman-screenshots/review-success.png](postman-screenshots/review-success.png)
  - [postman-screenshots/review-failure.png](postman-screenshots/review-failure.png)

### Kako je implementirano

- Za oba glavna API toka (reports i reviews) dokumentovan je po jedan uspjesan i jedan neuspjesan scenario.
- U kolekciji su definisani i basic Postman test scriptovi za status kod i kljucna polja odgovora.
- Dokumentacija prati format endpoint opisa (URL, metoda, params, response primjeri).

### Zasto je ovako implementirano

- Omogucava auditabilan trag dokazivanja realizacije zadatka.
- Olaksava demonstraciju i reprodukciju API ponasanja bez citanja izvornog koda.

## 6. Validacija modela

### Sta je implementirano

Validacija je uvedena na tri nivoa.

DTO nivo:

- [../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateProblemReportRequest.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateProblemReportRequest.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateLineReviewRequest.java](../src/main/java/com/sarajevotransit/feedbackservice/dto/CreateLineReviewRequest.java)

Entity nivo:

- [../src/main/java/com/sarajevotransit/feedbackservice/model/ProblemReport.java](../src/main/java/com/sarajevotransit/feedbackservice/model/ProblemReport.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/model/LineReview.java](../src/main/java/com/sarajevotransit/feedbackservice/model/LineReview.java)

Controller nivo (@Valid i @Positive za request body/path/query):

- [../src/main/java/com/sarajevotransit/feedbackservice/controller/ProblemReportController.java](../src/main/java/com/sarajevotransit/feedbackservice/controller/ProblemReportController.java)
- [../src/main/java/com/sarajevotransit/feedbackservice/controller/LineReviewController.java](../src/main/java/com/sarajevotransit/feedbackservice/controller/LineReviewController.java)

### Kako je implementirano

- Bean Validation anotacije (`@NotNull`, `@Positive`, `@NotBlank`, `@Size`, `@Min`, `@Max`) blokiraju neispravan input.
- Dodata su i poslovna pravila u service sloju:
  - report mora imati barem jedan vehicle identifikator ili `stationId`
  - `vehicleType` mora biti iz skupa `BUS|TRAM|TROLLEY|MINIBUS`
  - `rideDate` ne smije biti u buducnosti niti stariji od 30 dana

### Zasto je ovako implementirano

- Kombinacija strukturne i poslovne validacije stiti bazu i API ugovor.
- Greske se hvataju rano i vracaju klijentu u konzistentnom formatu.

## 7. Testovi web servisa

### Sta je implementirano

`@SpringBootTest` + MockMvc integration testovi:

- [../src/test/java/com/sarajevotransit/feedbackservice/controller/ProblemReportControllerIntegrationTests.java](../src/test/java/com/sarajevotransit/feedbackservice/controller/ProblemReportControllerIntegrationTests.java)
- [../src/test/java/com/sarajevotransit/feedbackservice/controller/LineReviewControllerIntegrationTests.java](../src/test/java/com/sarajevotransit/feedbackservice/controller/LineReviewControllerIntegrationTests.java)

### Kako je implementirano

Pokriveni su i uspjesni i neuspjesni tokovi:

- POST create report/review vraca `201 Created` + `Location`.
- Validation failure vraca `400` sa detaljima validacije.
- Malformed JSON vraca `400` sa `malformed_json` error kodom.
- Invalid enum i invalid path/query scenariji su testirani.
- Paginacija je testirana kroz `page/size/sort` parametre i `Page` JSON strukturu.

### Zasto je ovako implementirano

- Potvrdjuje da je kompletan HTTP sloj stabilan: routing, bindovanje, validacija, serializacija i exception mapping.

## 8. Testovi service klasa

### Sta je implementirano

Mockito unit testovi:

- [../src/test/java/com/sarajevotransit/feedbackservice/service/ProblemReportServiceTest.java](../src/test/java/com/sarajevotransit/feedbackservice/service/ProblemReportServiceTest.java)
- [../src/test/java/com/sarajevotransit/feedbackservice/service/LineReviewServiceTest.java](../src/test/java/com/sarajevotransit/feedbackservice/service/LineReviewServiceTest.java)

### Kako je implementirano

- Repository i mapper zavisnosti su mockovane.
- Testirane su glavne business grane:
  - odbijanje nevalidnih request-a
  - normalizacija string polja i enum vrijednosti
  - podrazumijevani statusi (`RECEIVED`, `VISIBLE`)
  - fallback ponasanje za summary bez podataka
  - `NotFoundException` za nepostojeci id

### Zasto je ovako implementirano

- Unit testovi izolovano provjeravaju business logiku, brzo i deterministicki.
- Brze hvataju regresije u domen pravilima bez podizanja cijelog web sloja.

## 9. N+1 provjera kroz Hibernate statistics

### Sta je implementirano

- Performance testovi:
  - [../src/test/java/com/sarajevotransit/feedbackservice/repository/RepositoryQueryPerformanceTests.java](../src/test/java/com/sarajevotransit/feedbackservice/repository/RepositoryQueryPerformanceTests.java)
- Optimizacija za report upite sa `photoUrls`:
  - [../src/main/java/com/sarajevotransit/feedbackservice/repository/ProblemReportRepository.java](../src/main/java/com/sarajevotransit/feedbackservice/repository/ProblemReportRepository.java)

### Kako je implementirano

- U testu se aktivira Hibernate `Statistics` preko `SessionFactory`.
- Nakon seed podataka, mjeri se `prepareStatementCount` za ciljne upite.
- Testovi su striktni i provjeravaju tacan broj (`isEqualTo(1)`) za:
  - paged retrieval reporta
  - paged retrieval review-a po liniji

### Zasto je ovako implementirano

- N+1 problem se kontrolise automatizovano, ne manualno.
- Svaka regresija u query planu uzrokuje fail testa.

## 10. Uniforman error handling

### Sta je implementirano

- Globalni exception handler:
  - [../src/main/java/com/sarajevotransit/feedbackservice/exception/GlobalExceptionHandler.java](../src/main/java/com/sarajevotransit/feedbackservice/exception/GlobalExceptionHandler.java)
- Uniforman response model:
  - [../src/main/java/com/sarajevotransit/feedbackservice/exception/ApiErrorResponse.java](../src/main/java/com/sarajevotransit/feedbackservice/exception/ApiErrorResponse.java)

### Kako je implementirano

Svi error odgovori koriste isti oblik:

- `timestamp`
- `status`
- `error`
- `message`
- `path`
- `validationErrors`

Pokriveni slucajevi:

- `NotFoundException`
- `BadRequestException`
- `MethodArgumentNotValidException`
- `ConstraintViolationException`
- `HttpMessageNotReadableException`
- `MethodArgumentTypeMismatchException`
- fallback `Exception`

### Zasto je ovako implementirano

- Klijent uvijek dobija citljiv i konzistentan format.
- Ne iznose se stack trace detalji u odgovoru za validacione greske.
- Lakse je standardizovati frontend obradu gresaka.

## 11. Pracenje preporucenih izvora (kako su primijenjeni)

### Spring guide: rest-service

Primijenjeno kroz klasicni REST controller + service pristup, validaciju i korektne HTTP status kodove.

### Spring guide: accessing-data-rest

Primijenjeno kroz Spring Data JPA repozitorije, paginaciju (`Pageable`) i query metode.

### Spring tutorial: building REST APIs

Primijenjeno kroz odvajanje slojeva, API ugovor kroz DTO, i pravilan error handling.

### Bocoup: documenting your API

Primijenjeno kroz strukturisanu endpoint dokumentaciju, primjere request/response i reproduktivnu Postman kolekciju sa screenshot dokazima.

### Baeldung: spring-boot-testing

Primijenjeno kombinovanjem integration testova (web sloj) i unit testova (service sloj).

### Baeldung: spring-hibernate-n1-problem

Primijenjeno kroz Hibernate statistics i automatske assertione za broj SQL upita.

## 12. Dodatna validacija na realnoj PostgreSQL bazi

### Sta je implementirano

Poseban integration test set za realni `feedbackdb`:

- [../src/test/java/com/sarajevotransit/feedbackservice/integration/PostgresFeedbackDbIntegrationTests.java](../src/test/java/com/sarajevotransit/feedbackservice/integration/PostgresFeedbackDbIntegrationTests.java)

### Kako i zasto

- Test provjerava da je datasource stvarno PostgreSQL (`jdbc:postgresql://.../feedbackdb`).
- Pokriva realni persist/read i paginaciju nad PostgreSQL bazom.
- Time se potvrdjuje da implementacija nije ogranicena samo na H2 test okruzenje.

## 13. Verifikacija trenutnog stanja testova

Pokrenuto lokalno:

```bash
.\mvnw.cmd test
```

Rezultat posljednjeg pokretanja:

- Tests run: 47
- Failures: 0
- Errors: 0
- Skipped: 0
- Build: SUCCESS

Surefire izvjestaji su dostupni u:

- [../target/surefire-reports](../target/surefire-reports)

Kljucni izvjestaji:

- [../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.controller.ProblemReportControllerIntegrationTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.controller.ProblemReportControllerIntegrationTests.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.controller.LineReviewControllerIntegrationTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.controller.LineReviewControllerIntegrationTests.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.service.ProblemReportServiceTest.xml](../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.service.ProblemReportServiceTest.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.service.LineReviewServiceTest.xml](../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.service.LineReviewServiceTest.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.repository.RepositoryQueryPerformanceTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.repository.RepositoryQueryPerformanceTests.xml)
- [../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.integration.PostgresFeedbackDbIntegrationTests.xml](../target/surefire-reports/TEST-com.sarajevotransit.feedbackservice.integration.PostgresFeedbackDbIntegrationTests.xml)

## 14. Zakljucak

Svi trazeni zadaci su implementirani u `feedbackservice` modulu sa jasnim dokazima u kodu, dokumentaciji i test artefaktima.
Implementacija je uskladjena sa trazenim smjernicama za REST dizajn, testiranje, DTO pristup, validaciju, N+1 kontrolu, uniforman error handling i Lombok-based smanjenje boilerplate koda.
