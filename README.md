# DedOSwitch 🎮

## Manejador de Cuentas Alternativas para Roblox Android Manager

¡Hola a todos! Estoy muy emocionado de presentarles la aplicación **DedOSwitch**. He estado trabajando en ella durante unos días y, aunque soy relativamente nuevo en este campo y gran parte del código es pegado, reciclado o generado, he dedicado mucho tiempo a investigar, resolver problemas y aprender sobre diferentes tecnologías como **Kotlin** y **Java**.

Espero que valoren mi esfuerzo y, si encuentran errores, no duden en informarlos. Haré todo lo posible por resolverlos y mejorar la aplicación.

## Aplicacion web
![image](https://github.com/D34THEV1L/DedOSwitch/assets/87221905/8f932124-6882-42b5-a926-d4d8140200e6)
## Aplicacion Movil
![image](https://github.com/D34THEV1L/DedOSwitch/assets/87221905/0165eae3-6b74-41b8-a191-d4f0d67b0688)


## Características

- Gestión de múltiples cuentas de Roblox en Android.
- Interfaz intuitiva y fácil de usar.
- Compatibilidad con websockets para comunicación en tiempo real.
- Desarrollado en Java.

## Instalación

### Requisitos

- [Android Studio](https://developer.android.com/studio) para compilar la aplicación móvil.
- Navegador web para la aplicación web (HTML, JavaScript).
- Cuenta en [PieSocket](https://piehost.com/piesocket) para crear un websocket.
- Emulador con Root, el programa funciona en MUmu player 12, no doy soporte a otros emuladores pero puede ser que funcione en todos.

### Pasos

1. Clona el repositorio:

    ```bash
    git clone https://github.com/tu-repositorio/DedOSwitch.git
    ```

3. Configura el websocket:
    - Crea una cuenta y un nuevo websocket en [PieSocket](https://piehost.com/piesocket).
    - Reemplaza la URL del websocket en el código:

      En `MyForegroundActivity` (Java):
      ```java
      private static final String WS_URL = "pon aquí tu URL de websocket";
      ```

      En `app.js` de la aplicación web:
      ```javascript
      const wsUrl = 'pon aquí tu URL de websocket';
      ```

4. Compila e instala la aplicación móvil utilizando Android Studio o una herramienta similar. ¿Porque no subes las aplicacion compilada tu? Es una muy buena pregunta apero debido a que tu no quieres estar teniendo que poner la URL del websocket emulador por emulador, asi nomas lo cambias en la variable y asi ya funciona de una.

5. Para la aplicación web, abre `index.html`. Si lo haces localmente, necesitarás un desbloqueador de CORS para hacer solicitudes a la API de imágenes del avatar en Roblox. Puedes usar esta extensión para Chrome: [CORS Unblock](https://chromewebstore.google.com/detail/cors-unblock/lfhmikememgdcahcdlaciloancbhjino).

## Uso

- Abre la aplicación móvil en tu emulador y sigue las instrucciones en pantalla para agregar y gestionar tus cuentas de Roblox.
- Accede a la aplicación web mediante `index.html` para complementar la gestión desde tu navegador.

## Errores Conocidos

- Si `emuladtorid`, `username`, o `userid` están vacíos, significa que la variable no se está guardando correctamente. Revisa el código o envíame un mensaje para revisarlo juntos.
- Al conectarte a un servidor como farm o drpo, si no se abre correctamente, puede deberse a que el comando root no se pudo ejecutar.

Adjuntaré videos de uso y los videos que utilicé para la creación de esta aplicación en el futuro cercano.

Comandos Root Basicos:
https://github.com/fandreuz/TUI-ConsoleLauncher/wiki/Root-commands

Foreground Service en Java:
https://www.youtube.com/watch?v=jFE9FZVykdI&t=5s

Html css:
https://www.youtube.com/watch?v=ELSm-G201Ls

Si desea agregar un comando root le recomiendo pedirle ayuda a una inteligencia artificial de que comando puede usar para lograr su cometido o buscar una documentacion de comandos en Unix.


---

¡Gracias por usar DedOSwitch! Si tienes alguna pregunta o encuentras algún error, no dudes en contactarme. Tu feedback es muy valioso para mejorar esta aplicación.
