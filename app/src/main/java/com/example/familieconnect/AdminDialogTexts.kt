package com.example.familieconnect

import android.widget.EditText
import android.widget.TextView

object AdminDialogTexts {

    fun refresh(
        language: String,
        readmeText: TextView,
        codeInputAdmin: EditText,
        mapLabel: TextView
    ) {
        readmeText.text =
            if (language == "en") README_EN else README_NL

        codeInputAdmin.hint =
            if (language == "en") "Family code" else "Gezinscode"

        mapLabel.text =
            if (language == "en") "Map" else "Kaart"
    }

    private val README_NL = """
README

FamilieConnect is bedoeld voor kleine groepen, typisch een gezin of familie,
om elkaar eenvoudig te kunnen volgen en indien nodig snel hulp te bieden.

Iedere groep werkt met een unieke gezinscode. Alleen personen die deze code
kennen, kunnen de andere leden van de groep zien.

INSTALLATIE

1. Installeer de app met het APK-bestand.
2. Sta alle gevraagde rechten toe.
3. Schakel batterijoptimalisatie uit en controleer regelmatig of die nog uit staat.

Dit is belangrijk voor een betrouwbare werking op de achtergrond.

GEBRUIK

Met FamilieConnect kunt u:

• Kijken waar de groepsleden zich bevinden.
• In geval van nood op SOS drukken om een noodsignaal uit te sturen.
• Dubbeltikken op de kaart om naar de marker van een lid te navigeren.
• Nogmaals dubbeltikken om naar het volgende lid te gaan.
• Op een marker tikken om dat lid op de kaart te volgen.

MARKERS

Groen = actuele en zeer nauwkeurige positie.

Oranje = minder nauwkeurig, maar nog bruikbaar.

Rood = laatst bekende positie. Het lid is daar geweest,
maar bevindt zich daar mogelijk niet meer.

Geen marker zichtbaar = het toestel van dit lid staat uit,
heeft geen verbinding of heeft een lege batterij.

INSTELLEN

1. Installeer de app.
2. Open de app en voer uw naam in.
3. Open CONF.
4. Voer de beheer-PIN in.
5. Maak een gezinscode en sla deze op.
6. Voer dezelfde gezinscode in op de toestellen van alle leden.

Zonder de beheer-PIN kunnen deze instellingen niet worden gewijzigd.

PRIVACY

FamilieConnect bewaart geen routegeschiedenis.

Alleen de laatst bekende positie wordt gebruikt.
""".trimIndent()

    private val README_EN = """
README

FamilieConnect is designed for small groups, typically a family,
to easily keep track of each other and provide quick assistance when needed.

Each group uses a unique family code. Only people who know this code
can see the other members of the group.

INSTALLATION

1. Install the app using the APK file.
2. Grant all requested permissions.
3. Disable battery optimization and regularly check that it remains disabled.

This is important to ensure reliable background operation.

USING FAMILIECONNECT

With FamilieConnect you can:

• See the location of all group members.
• Press SOS in an emergency to send an alert.
• Double-tap the map to navigate to a member's marker.
• Double-tap again to move to the next member.
• Tap a marker to follow that member on the map.

MARKERS

Green = current and highly accurate position.

Orange = less accurate, but still usable.

Red = last known position. The member was there,
but may no longer be at that location.

No marker visible = the member's device is switched off,
has no connection or has an empty battery.

SETUP

1. Install the app.
2. Open the app and enter your name.
3. Open CONF.
4. Enter the administrator PIN.
5. Create a family code and save it.
6. Enter the same family code on all group members' devices.

Without the administrator PIN, these settings cannot be changed.

PRIVACY

FamilieConnect does not store route history.

Only the most recent known position is used.
""".trimIndent()
}