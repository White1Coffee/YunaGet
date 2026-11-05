# YunaGet v0.2.1 - automatische itemselectie, xray en navigatie

Client-side Fabric-mod voor Minecraft Java 26.3. Vereist Java 25, Fabric Loader **0.19.5+** en Fabric API **0.165.0+26.3 of nieuwer voor 26.3**. Geen servermod, OP of servercommando's nodig. De server valideert normale interacties. Lunar Client is niet apart getest.

Installeer `yunaget-0.2.1.jar` in de client-modmap en verwijder eerdere YunaGet-JARs uit die map. Er mag slechts een versie tegelijk aanwezig zijn. De oude Loader-eis van 0.20.1 is al vervangen door 0.19.5; de metadata wordt daarop getest.

## Zo begin je

1. Druk **Y** of gebruik `/yunagetui`.
2. Open **Items**. Zoek op vertaalde naam of registry-ID; de lijst reageert tijdens typen.
3. Vink meerdere items aan door op het vakje of de itemnaam te klikken. Uitvinken kan op dezelfde manier. Iconen en aantallen in je tas helpen bij de keuze.
4. Of kies een goedgekeurde itemgroep: **Ertsen**, **Hout** of **Bouwblokken**. Groepen voegen items toe aan je selectie en wissen andere selecties niet.
5. Klik **Start automatisch**. YunaGet verzamelt passende losse drops en probeert daarna passende bekende blocks te bereiken en te minen.
6. Een turquoise lijn loopt vanaf het midden van je beeld naar de AutoWalk-bestemming. De route op de grond toont de geplande tussenstappen.
7. **F8 is standaard de NOODSTOP**. Ook de rode NOODSTOP-knop, de bestaande stoptoets en `/yunagetstop` stoppen alle automatisering. Stel toetsen bij onder Settings; bij een bestaand toetsconflict kan F8 onbezet blijven en moet je zelf een noodstoptoets kiezen.

**Er zijn geen verplichte gewenste aantallen meer.** Aangevinkte items blijven relevant, ook als je er al stacks van bezit. Verzamelen/minen gaat door tot je stopt, de selectie leegmaakt of er onvoldoende inventoryruimte is. Getallen in de Items-lijst zijn alleen de huidige voorraad. De minimale bewaaraantallen bij chest-sortering zijn een aparte optionele opslagregel.

## Wat automatisch gebeurt

- De collector kiest het dichtstbijzijnde passende geladen item-entity waarvoor inventoryruimte is. Onbereikbare of niet-opgepakte drops krijgen tijdelijk een pauze zodat andere doelen kunnen worden geprobeerd.
- Mining kiest het dichtstbijzijnde zichtbare bruikbare block binnen normaal interactiebereik. Is er geen direct doel, dan kiest hij een nabij bekend passend block uit de clientcache en zoekt een vrije standplaats met zicht op dat block.
- Die standplaats wordt met gewone loopinput bereikt. De best-first routeplanner verwerkt maximaal 100 cellen per inputtick en bewaart maximaal 2048 bezochte cellen. Maximaal een block op/af; geen vliegen, teleporteren, tunnels graven of deuren bedienen.
- Nieuwe dichterbij bekende doelen kunnen een miningroute vervangen. De nabijheidskeuze geldt voor de op dat moment bekende en bruikbare kandidaten, niet voor onbekende delen van de hele wereld. Losse drops hebben voorrang op nieuwe miningacties; sorteren heeft voorrang op beide.
- AutoTool kiest bruikbaar gereedschap uit de hotbar op correcte drops, snelheid, Efficiency en minimale durability. Optioneel wordt het oorspronkelijke slot hersteld. Handmatig gekozen slots worden als nieuwe voorkeur behouden.
- Fysiek bewegen, springen of sneaken onderbreekt AutoWalk en pauzeert automatisering. Hervat via Dashboard. Een geopend menu en een inactief venster onderbreken de bediening. Een wereld-/dimensiewissel pauzeert de taken.
- Een volle inventory pauzeert het minen; de collector probeert alleen drops die nog passen. Een ingestelde Auto Sorter kan ruimte maken. Er worden geen items weggegooid.

## Xray en zichtbare doelen

De standaard ingeschakelde **Xray van aangevinkte items** geeft passende bekende blocks een turquoise outline, ook achter muren. Dit gebruikt uitsluitend blockgegevens die de client al van de server heeft ontvangen. De mod hoeft daarvoor niet eerst fysiek door de omgeving te lopen. Hij verwerkt de aanwezige gegevens automatisch en gespreid over ticks; een grotere radius heeft opwarmtijd.

Server-side anti-xray, niet-geladen chunks en verborgen gegevens worden niet omzeild. Xray is alleen weergave en doelinformatie: het geeft geen langer bereik en er wordt niet door muren gemined. Volledig ingebedde ertsen zonder veilige vrije standplaats/pad worden overgeslagen. Open zo nodig zelf een normale toegang.

Onder Mining stel je zoekradius en alleen-zichtbare doelen in. Onder ESP zet je selectie-xray en de bestaande Ore ESP aan/uit. Ore ESP ondersteunt normale/deepslate-ertsen en ancient debris, eigen blocktypes, kleuren en afstand. Maximaal 1024 blockposities worden per tick verwerkt; caches zijn begrensd op 2048 ores en 2048 miningdoelen, de renderer op 512 outlines per frame. Updates en chunk-/wereldwissels maken caches ongeldig.

De miningdropkoppeling ondersteunt gewone block-items, raw ores, diamant/coal/etc., cobblestone uit stone en enkele andere vanilla-drops. Crafting/smelten wordt niet geautomatiseerd. Silk Touch en aangepaste serverloot kunnen afwijken. De groep Ertsen kiest bijvoorbeeld raw_iron en niet een vereiste hoeveelheid iron_ingot.

## UI en de goedgekeurde extra's

De UI heeft een vaste zijbalk, rustige achtergrondpanelen, een itemlijst met iconen en echte aan/uit-selectie, vaste zoek-/groeps-/startknoppen en losse statusregels. Daardoor verdwijnen de belangrijkste itemknoppen niet op een andere pagina.

- **Statuspaneel:** linksboven tijdens spelen, met functie, taak, AutoWalk-doel, afstand, selectie en inventorygebruik. Uitschakelbaar onder Settings.
- **Noodstop:** standaard F8, een NOODSTOP-knop en de bestaande stopbediening. Stopt mining, beweging, collector, sorteren en de chestmarkeermodus. De lijn verdwijnt wanneer de route wordt geannuleerd.
- **Itemgroepen:** Ertsen, Hout en Bouwblokken vullen de selectie in een klik aan. Nogmaals toevoegen maakt geen duplicaten. Individuele items blijven apart uitvinkbaar; Alles uitvinken wist alleen de itemselectie.

De navigatielijn en de geplande grondroute zijn apart aan/uit te zetten onder Settings. Zonder actief AutoWalk-doel wordt geen lijn getekend. Gekozen drops krijgen de bestaande item-glow; entity-glow en chest-glow blijven beschikbaar. Via ESP > Entitylijst kies je nog steeds een entitytype voor glow.

De tabs Dashboard, Items, Mining, Collector, ESP, Storage en Settings blijven aanwezig. Toetsen zijn instelbaar. De oude keybind met naam AMOUNT wordt bij migratie behouden als snelkoppeling naar de itemselectie; hij opent geen aantalveld meer.

## Chest Manager, glow en presets

### Markeren en deselecteren

1. Open Storage en kies **[+] Mark Chest**.
2. Kijk naar een chest en rechtsklik, of gebruik de ingestelde marktoets.
3. Open Storage opnieuw en klik de gemarkeerde chest om naam, filters, prioriteit en actieve status in te stellen.

De marktoets kan ook direct tijdens het spelen worden gebruikt. **Nogmaals markeren van dezelfde chest deselecteert hem.** Unmark in de chesteditor verwijdert de markering eveneens. Single en double chests worden ondersteund; beide helften van een double chest verwijzen naar dezelfde markering. Alleen gewone/trapped chests met een standaard chestmenu worden gesorteerd; geen Ender Chests, barrels of modcontainers.

Gemarkeerde chests krijgen een groene glow-outline; uitgeschakelde chests worden oranje. Double chests krijgen een gezamenlijke outline. Deselecteren verwijdert de YunaGet-outline. Chest-glow en zichtbaarheid door muren zijn apart instelbaar; glow kan ook aan een toets worden gekoppeld. De algemene ESP-renderafstand geldt ook voor chests. Niet-geladen of verdwenen chests worden niet getekend.

### Filters en presets

Een filter is een door komma's gescheiden lijst met:

- Specifieke IDs: `minecraft:diamond,minecraft:diamond_block`.
- Tags die de clientregistry kent: `#minecraft:logs,#c:ingots`.
- Eigen categorieen: `@Wood`. Definieer die onder de Storage-instellingen.

Ontbrekende tags en categorieen matchen niets. Cyclische categorieen lopen niet oneindig door. De hoogste numerieke prioriteit wint onder specifieke filters. Een expliciete fallback-chest wordt alleen gekozen wanneer geen specifiek filter matcht en fallback aan staat. Een volle/onbereikbare specifieke chest laat de items in de inventory; er wordt niet zomaar naar een onjuiste fallback uitgeweken.

De chesteditor heeft presets **Diamonds, Ores, Building Blocks, Wood, Mob Drops, Food, Tools en Miscellaneous**. Een preset vervangt filters, prioriteit en fallbackinstelling; ingebouwde presets vullen ook de naam in. Miscellaneous is een expliciete fallback. Tagpresets werken alleen voor tags die de client kent en sommige lijsten zijn bewust voorbeelden, geen complete catalogus van alle moditems.

Je kunt eigen filterpresets opslaan, toepassen en verwijderen. Ze bevatten geen wereldpositie en worden gekopieerd bij toepassen, zodat chests onafhankelijk bewerkbaar blijven. Een preset bewaart de bewerkte filters en de actuele prioriteit/fallback van de chest. Maximaal 128 eigen presets, 256 chestmarkeringen en 512 filters per chest/preset.

### Server- en wereldscheiding

Markeringen zijn gekoppeld aan serveradres of lokaal savepad, wereldprofiel, dimensie en blockpositie. Chests uit andere scopes worden niet gebruikt of getoond.

Een normale multiplayer-client krijgt niet altijd een unieke ID voor meerdere werelden achter hetzelfde serveradres en dezelfde dimensie. Kies daarom onder Storage een eigen **wereldprofiel** per dergelijke wereld. De standaardwaarde is `default`. Wissel dit voordat je markeringen in een andere serverwereld gebruikt. Singleplayer gebruikt het lokale savepad automatisch.

### Sorteren en serverbevestiging

Nu sorteren start een enkele sorteerronde. Automatisch sorteren kan worden ingeschakeld bij een percentage bezette inventoryslots. AutoWalk naar chest is optioneel en standaard uit. Favoriete item-IDs, hotbar overslaan, minimale bewaaraantallen, maximale afstand en prioriteit blijven gelden.

De sorter:

1. Bepaalt per actuele stack een filterbestemming.
2. Controleert wereld, afstand, chestblock en bereik; navigeert zo nodig met de gewone planner.
3. Opent met de gewone clientinteractie en wacht op een echt geopend chestmenu.
4. Valideert de containeridentiteit, 27/54 chestslots, alle 36 player-inventoryslots en een lege cursor. Inventoryslots worden herkend aan hun container en getContainerSlot(), niet aan hardgecodeerde menu-offsets.
5. Verplaatst een passende hele stack met normale QUICK_MOVE/shift-click.
6. Wacht op servergegevens die corresponderende bron- en bestemmingswijzigingen aantonen. Lokale voorspelling telt niet als bevestiging.
7. Als vanilla geen aparte wijzigingspackets verstuurt omdat de voorspelling klopt, sluit/heropent hij na een korte wachttijd dezelfde chest met normale interacties en controleert de verse serverinhoud. Hij klikt de verplaatsing niet nogmaals.
8. Gaat pas na bevestiging verder, sluit de chest en probeert de volgende bestemming.

Er wordt nooit bewust gedropt, een buiten-slot aangeklikt of een stack direct in een serverinventory geschreven. Alleen hele stacks worden verplaatst; een stack die de minimale bewaaraantallen zou onderschrijden blijft volledig bij de speler. Daardoor kan meer dan het ingestelde minimum achterblijven. Volle chests/ontbrekende paden worden overgeslagen. Bij timeout, onverwachte GUI, cursoritem, andere layout, ontbrekende bevestiging of handmatig klikken/toetsen in de sorteercontainer wordt veilig afgebroken en gepauzeerd. Een handmatige invoer die het sorteren annuleert wordt niet nog op de oude container uitgevoerd.

De client kan toegang tot protected chests pas vaststellen wanneer de server reageert. Een plugin die een andere maar identiek ogende standaard chest-GUI aanbiedt kan niet altijd van een echte chest worden onderscheiden; aangepaste containerplugins moeten apart getest worden. Containers worden tijdens verificatie mogelijk zichtbaar gesloten en heropend.


## Configuratie, build en validatie

Bestaande keybinds en glowinstellingen staan in `config/yunaget.properties`. De andere instellingen, geselecteerde items, chestmarkeringen, categorieen en presets staan in `config/yunaget-v2.json`.

Oude `targets` met doelgetallen worden eenmaal omgezet naar `selectedItems`: de itemkeuze blijft behouden, de aantallen en stop-op-aantal-eisen verdwijnen. Bestaande chestmarkeringen/presets en overige instellingen blijven behouden. Bij deze migratie worden bekende doelen achter muren als zoekdoel toegestaan; de zichtbaarheidseis voor het daadwerkelijke breken blijft altijd gelden. Configuratie opnieuw laden pauzeert de automatisering.

Build met JDK 25 en de meegeleverde Gradle 9.8.1-wrapper:

```powershell
$env:JAVA_HOME = 'PAD/NAAR/JDK-25'
./gradlew.bat build
```

Linux/macOS: `./gradlew build` (zo nodig eerst `chmod +x gradlew`). Loom 1.19.3, Minecraft 26.3, Loader 0.19.5, Fabric API 0.165.0+26.3. JAR: `build/libs/yunaget-0.2.1.jar`.

JUnit-tests behandelen filters, categoriecycli, sorteervoorrang, reserves, containerbevestiging, state-overgangen, presets, client-only/Loader-metadata, selectie zonder aantallen, onafhankelijke vinkjes, selectiegrenzen, itemgroepen en de dichtstbijzijnde geldige kandidaat. Testrapport: `build/reports/tests/test/index.html`.

**Er zijn geen Minecraft-speltests of Lunar Client-integratietests uitgevoerd.** Builds en unit tests bewijzen geen correcte zichtbare UI/rendering of universele servercompatibiliteit. Test in een testwereld:

- Zoek en vink meerdere items aan; wis de selectie, voeg groepen toe en herstart met oude configuratie.
- Laat passende drops op verschillende afstanden liggen en controleer doelwissels, pickup, volle inventory en geen hoeveelheidsstop.
- Controleer bekende blocks achter muren, onbereikbare doelen, AutoTool en gewone mininginteracties.
- Controleer de lijn, de grondroute en het statuspaneel bij lopen, GUI openen, handmatige input, noodstop en disconnect.
- Test obstakels en gevaar. De planner is conservatief en eenvoudig; hij garandeert geen veiligheid tegen mobs, explosies, servercorrecties of veranderende wereldsituaties.
- Test single/double chest markeren/deselecteren, glow, presets, filters, prioriteit en serverbevestiging bij sorteren.
