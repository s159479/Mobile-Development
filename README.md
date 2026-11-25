# Mobile-Development

## Informatie student

Jasper Van Hove  
Jonas Schaerlaeken

## Planning

| deadline | taak | persoon |
|----------|----------------------|---------|
| 11/11 | project aanmaken | Jonas |
| 11/11 | firebase opzetten | Jasper |
| 11/11 | room klaarzetten | Jasper |
| 11/11 | registreren | Jonas |
| 11/11 | inloggen en uitloggen | Jonas |
| 11/11 | winkel aanmaken | Jasper |
| 18/11 | locatiepermissie | Jasper |
| 18/11 | pakketten in de buurt | Jasper |
| 18/11 | lijstweergave voor klant |Jonas|
| 18/11 | kaartweergave voor klant |Jonas|
| 25/11 | pakket aanbieden |Jonas|
| 25/11 | detailscherm |Jonas|
| 25/11 | reserveren |Jonas|
| 25/11 | ophaalcode |Jonas|
| 25/11 | mijn bestellingen |Jasper|
| 25/11 | favorieten |Jasper|
| 2/12 | adres automatisch invullen |Jasper|
| 2/12 | foto toevoegen |Jasper|
| 2/12 | eigen pakketten |Jonas|
| 9/12 | laatste checkup |/|
| 9/12 | loading toevoegen |/|

### Sprint 1

#### project aanmaken
- afhankelijk
    - /

- doel
    - project aanmaken in android studio

#### firebase opzetten
- afhankelijk
    - /

- doel
    - firebase account aanmaken
    - firestore opzetten
    - activeren authentication
    - security rules

#### room klaarzetten
- afhankelijk
    - project aanmaken

- doel
    - room database klaarzetten voor gebruik in de toekomst

#### registreren
- afhankelijk
    - firebase opzetten
    - project aanmaken

- doel 
    - naam, email, wachtwoord, boolean klant/handelaar --> wachtwoord minstens: standaart
    - geef foutmeldingen visueel
    - email moet email zijn en mag nog niet bestaan

#### inloggen en uitloggen
- afhankelijk 
    - registreren

- doel
    - inloggen nakijken of alles klopt --> email + wachtwoord
    - geef foutmeldingen visueel
    - toon foutmelding zonder internet
    - uitloggen terug naar inlog pagina sturen

#### winkel aanmaken
- afhankelijk
    - inloggen

- doel
    - naam, categorie, locatie = hardcoded for now

### Sprint 2

#### locatiepermissie
- afhankelijk
    - inloggen

- doel
    - toestemming vragen voor de locatie (nauwkeurig of bij benadering)
    - uitleg tonen waarom de locatie nodig is
    - bij weigeren: melding tonen en knop naar de app-instellingen
    - huidige locatie van de gebruiker ophalen voor later gebruik

#### pakketten in de buurt
- afhankelijk
    - locatiepermissie
    - winkel aanmaken

- doel
    - winkels en pakketten ophalen rond de locatie van de klant
    - enkel pakketten die nog beschikbaar zijn (quantityLeft > 0 en ophaaltijd nog niet voorbij)
    - gesorteerd op afstand, standaard binnen 5 km
    - wordt hergebruikt door de lijstweergave en kaartweergave voor de klant

- velden van een pakket in Firestore (bags/{bagId}), te gebruiken bij "pakket aanbieden"
    - storeId: string (uid van de handelaar = id van de winkel)
    - title, description: string
    - price: number
    - quantityTotal, quantityLeft: number (geheel getal)
    - pickupStart, pickupEnd: timestamp

#### lijstweergave voor klant
- afhankelijk
    - pakketten in de buurt
    - locatiepermissie
    - inloggen

- doel
    - klant krijgt na het inloggen een scherm met tabbladen (Ontdekken, Kaart, Bestellingen, Favorieten)
    - tabblad Ontdekken toont de beschikbare pakketten in de buurt, gesorteerd op afstand
    - per pakket: titel, winkelnaam, ophaalvenster, prijs, aantal over en afstand in km
    - de lijst wordt realtime bijgewerkt: reserveert iemand het laatste pakket, dan verdwijnt het meteen
    - toon een laadindicator tijdens het ophalen en een melding als er geen pakketten zijn
    - toon foutmelding zonder internet, met een knop om opnieuw te proberen
    - bestellingen en favorieten zijn voorlopig nog leeg ("komt binnenkort")

#### kaartweergave voor klant
- afhankelijk
    - pakketten in de buurt
    - locatiepermissie
    - lijstweergave voor klant

- doel
    - tabblad Kaart toont een OSMDroid-kaart met de eigen locatie en een marker per winkel met beschikbare pakketten
    - tik op een marker om de winkelnaam en het aantal pakketten te zien, met een knop "Bekijk"
    - gebruikt dezelfde gegevens als de lijstweergave, dus ook realtime bijgewerkt
    - toon een laadindicator en een foutmelding met knop om opnieuw te proberen
    - de MapView volgt de levenscyclus van het scherm (onResume, onPause, onDetach)

### Sprint 3

#### detailscherm
- afhankelijk
    - pakketten in de buurt
    - lijstweergave voor klant
    - kaartweergave voor klant

- doel
    - detailscherm openen door op een pakket in de lijst te tikken of op "Bekijk" bij een marker op de kaart
    - titel, winkel, adres, beschrijving, prijs (met doorstreepte originele waarde), ophaalvenster, aantal over en afstand tonen
    - kleine kaart met de winkel
    - foto is voorlopig een plaatshouder, komt bij "foto toevoegen"

#### reserveren
- afhankelijk
    - detailscherm
    - inloggen

- doel
    - aantal kiezen, maximaal wat er nog over is
    - reserveren in een Firestore-transactie: het aantal over daalt correct, ook als twee klanten tegelijk het laatste pakket nemen
    - na het reserveren een ophaalcode van 6 tekens tonen die de klant in de winkel toont
    - geef foutmeldingen visueel: uitverkocht, geen internet, mislukt
    - lijst en kaart worden realtime bijgewerkt, een uitverkocht pakket verdwijnt meteen

- velden van een reservatie in Firestore (reservations/{id}), te gebruiken bij "mijn bestellingen"
    - bagId, storeId, customerId: string
    - quantity: number (geheel getal)
    - pickupCode: string (6 tekens)
    - status: string ("reserved")
    - createdAt: timestamp
    - extra: bagTitle, storeName: string en pickupStart, pickupEnd: timestamp

#### ophaalcode
- afhankelijk
    - reserveren

- doel
    - bij elke reservatie wordt een ophaalcode van 6 tekens gemaakt
    - de code bevat geen 0, O, 1 en I omdat die makkelijk verwisseld worden
    - de code wordt bewaard in het veld pickupCode van de reservatie (reservations/{id})
    - na het reserveren toont het detailscherm de code groot, met de tekst dat de klant ze in de winkel moet tonen
    - de code blijft zichtbaar, ook als het laatste pakket uitverkocht is en uit de lijst verdwijnt
    - de handelaar die de code controleert en de reservatie als opgehaald markeert komt later bij "eigen pakketten"

#### pakket aanbieden
- afhankelijk
    - winkel aanmaken

- doel
    - handelaar kan een pakket aanbieden via de knop "Nieuw pakket"
    - titel, beschrijving, prijs, originele waarde, aantal en ophaalvenster (begin- en einduur van vandaag)
    - geef foutmeldingen visueel: titel verplicht, prijs groter dan 0, originele waarde minstens de prijs, minstens 1 pakket, geldig uur, einduur na beginuur en nog niet voorbij
    - toon foutmelding zonder internet
    - eerst een winkel aanmaken, anders een melding
    - extra velden in bags/{bagId}: originalValue, storeName, location, active
    - foto komt later bij "foto toevoegen"

#### mijn bestellingen
- afhankelijk
    - reserveren
    - ophaalcode
    - room klaarzetten

- doel
    - tabblad Bestellingen toont de actieve en eerdere reservaties van de klant
    - actief: nog op te halen, met de ophaalcode groot erbij, eerstvolgende ophaling bovenaan
    - eerder: opgehaald, geannuleerd of niet opgehaald (ophaalvenster voorbij), nieuwste bovenaan
    - per bestelling: titel, winkel, aantal, ophaaldag en ophaalvenster en status
    - elke wijziging in Firestore wordt naar Room geschreven (CachedReservation), het scherm leest enkel uit Room zodat het ook offline werkt
    - zonder internet een melding dat je de bewaarde bestellingen ziet
    - bij uitloggen worden de bewaarde bestellingen van de gsm gewist
