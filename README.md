# LifeVisuals (Fabric 1.21.4)

Klient wizualny PvP: Target HUD, ESP, Glow, czasteczki, Damage Indicator, Totem Pop. Menu: klawisz **P**.

## Budowanie
Wymagane: JDK 21 i internet (Gradle pobiera Minecraft 1.21.4, Yarn, Fabric Loom, Fabric API).

    gradle build          # lub ./gradlew build, jesli masz wrapper
    # wynik: build/libs/LifeVisuals-1.0.0.jar  (NIE ten z "-sources")

Bez instalowania czegokolwiek: wrzuc folder na GitHub – workflow `.github/workflows/build.yml`
zbuduje JAR (zakladka Actions -> artefakt `LifeVisuals-jar`).

## Instalacja
1. Zainstaluj Fabric Loader dla 1.21.4 (fabricmc.net/use).
2. Wrzuc `LifeVisuals-1.0.0.jar` oraz **Fabric API 0.119.4+1.21.4** do `.minecraft/mods`.
3. Uruchom profil Fabric 1.21.4, w grze nacisnij **P**.

Ustawienia zapisuja sie w `.minecraft/config/fazevisuals.json`.

## Ograniczenia (uczciwie)
- Dane celu (HP, zbroja, rece) pochodza z tego, co serwer wysyla klientowi. Serwery ukrywajace
  zdrowie graczy pokaza tam to, co klient faktycznie zna.
- Damage Indicator = spadek HP ofiary w ~0,7 s po moim ataku. Jesli w tym czasie trafi ja ktos inny,
  wartosc bedzie zawyzona. Krytyk jest przewidywany po stronie klienta (te same warunki co w grze).
- Box ESP / Target ESP respektuja glebie (nie widac ich przez sciany) – celowo, zeby mod pozostal
  wizualny i nie dawal przewagi niedozwolonej na serwerach. Glow rysuje obrys tylko wokol celu.
- Menu: suwaki, ON/OFF i wybor kolorow dzialaja na wlasnym UI (bez widgetow vanilla).
