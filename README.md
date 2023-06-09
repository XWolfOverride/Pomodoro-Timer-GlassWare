# Temporizador Pomodoro para Google Glass

[![License](https://img.shields.io/badge/license-MIT-green)](https://opensource.org/license/mit/)
[![Twitter Follow](https://img.shields.io/twitter/follow/xwolfoverride?style=social)](https://twitter.com/xwolfoverride)

> Para compilar es necesario usar gradle.
> Para ejecutar se necesita el modo desarrollo.

## Modo de uso:

- En el menú de Google Glass te aparecerá la opción Pomodoro.
- Se abrirá una nueva sección con el contador pomodoro.
- La pantalla de las gafas se puede apagar para ahorrar batería.
- Al acabarse el tramo actual se oirá una melodía y se continuará con el siguiente tramo:

## Tramos Pomodoro:
  - Contador de **25** minutos en modo actividad
  - Contador de **5** minutos para pausa
  - Contador de **25** minutos en modo actividad
  - Contador de **5** minutos para pausa
  - Contador de **25** minutos en modo actividad
  - Contador de **15** minutos para pausa

## Notas:
 - Generar versión:
   - `gradlew build`
 - Instalar:
   - `adb install pomdoro-glass.apk`
 - Desinstalar
   - `adb uninstall es.xwolf.android.glass.pomodoro`

The application is in spanish. If you want to translate it to other language you can change easy the texts inside src/main/res/values/strings.xml