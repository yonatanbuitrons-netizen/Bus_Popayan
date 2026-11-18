# Guía rápida de la demo — Seguimiento en vivo

Qué hace: tu compañero comparte su ubicación y en la app ves en tiempo real
**por dónde va (línea azul)**, **la ruta por calles hasta ti (línea verde)**,
**en cuánto tiempo llega** y **por qué calles viene**.

```
 Compañero (navegador o celular)            Tu PC                      Tu app Android
 ───────────────────────────────           ──────────────────          ───────────────────
 Panel web / botón "Compartir"  ──WebSocket──►  Node + Socket.IO  ──WebSocket──►  Mapa OSM + ETA
   evento conductor:ubicacion                 (backend/server.js)        evento bus:ubicacion
                                                                          │
                                                                          └─► OSRM (rutas gratis) → ruta + minutos
```

Nada de esto usa Google Maps ni necesita tarjeta: el mapa es OpenStreetMap
(osmdroid) y las rutas/tiempos salen de OSRM, ambos gratuitos.

---

## 1. Arrancar el backend (en tu PC)

```bash
cd backend
npm install
npm start
```

Debe imprimir algo así:

```
Servidor funcionando en http://localhost:3000/api
Panel para compartir ubicación:
  En este PC:        http://localhost:3000/
  Desde la red WiFi: http://192.168.1.15:3000/
```

* Si MongoDB no conecta, **el seguimiento en vivo sigue funcionando** (solo
  rutas/paraderos REST quedan sin datos).
* En Mac, si aparece "¿Permitir que node acepte conexiones entrantes?", dale **Permitir**.

## 2. Decidir cómo se conectan (elige UNA opción)

### Opción A — Todo en la misma WiFi (más simple)
* App en el **emulador**: deja `Config.SERVER_URL = "http://10.0.2.2:3000"`.
* App en **celular físico**: pon la IP que imprimió `npm start`, ej.
  `"http://192.168.1.15:3000"`.
* Tu compañero abre `http://192.168.1.15:3000/` en su navegador.

> En WiFi de universidad a veces los equipos no se ven entre sí. Si no carga,
> usa la opción B.

### Opción B — Túnel https (funciona desde cualquier red, recomendado)
En otra terminal de tu PC:

```bash
brew install cloudflared            # Mac (Windows: winget install Cloudflare.cloudflared)
cloudflared tunnel --url http://localhost:3000
```

Te da un enlace como `https://algo-raro.trycloudflare.com` (gratis, sin cuenta).

* Tu compañero abre ese enlace (desde su PC o su celular).
* Con https, el botón **"Usar mi GPS real"** del panel funciona también en celular.
* Si usas celular físico para la app, pon ese enlace en `Config.SERVER_URL`.
* El enlace cambia cada vez que reinicias `cloudflared`.

## 3. Ejecutar la app

1. En Android Studio: **Sync Project with Gradle Files** y luego **Run**.
2. Inicia sesión (cualquier correo y clave) → pestaña **Mapa**.
3. Acepta el permiso de ubicación.
   * En el **emulador**: botón `⋯` → *Location* → pon un punto de Popayán y
     *Set location*.
   * Si no hay GPS (salón cerrado): **mantén presionado el mapa** para fijar tu
     ubicación a mano. El botón de ubicación vuelve al GPS.

## 4. Tu compañero transmite (elige una)

* **Panel web** (`/` del servidor):
  * *Usar mi GPS real* → su ubicación real (necesita https o localhost).
  * *Simular recorrido* → toca el mapa para elegir destino y avanza por las
    calles reales a la velocidad elegida. **Ideal para la sustentación.**
  * *Mover con clic* → cada clic en el mapa lo mueve ahí.
* **App Android en su celular**: misma APK, botón **"Compartir mi ubicación"**.

Usen la misma ruta: **R001** (está en `Config.RUTA_DEMO` y en el panel).

## 5. Qué mostrar en la sustentación

1. Panel → *Simular recorrido* a 60 km/h hacia tu ubicación.
2. En la app: el ícono azul se mueve, deja la línea azul (recorrido), aparece
   la línea verde (ruta hasta ti) y la tarjeta muestra
   **"Llega en X min · Y km · por Calle A → Carrera B"**, con
   "En vivo · actualizado hace 2 s".
3. Cuando llega a menos de 60 m: **"¡Ya llegó a tu ubicación!"**.
4. *Detener transmisión* → la tarjeta muestra "Dejó de transmitir".

## Si algo falla

| Síntoma | Solución |
|---|---|
| Tarjeta dice "Conectando al servidor…" | Backend apagado o `Config.SERVER_URL` mal. En emulador debe ser `10.0.2.2`; puerto **3000**. |
| Mapa gris sin calles | El emulador/celular no tiene internet. |
| Dice "estimado en línea recta" | OSRM no respondió (internet); el tiempo se estima a 25 km/h. |
| Panel: "solo da el GPS en https" | Usa el túnel (opción B) o la simulación. |
| No ves la ubicación del compañero | Revisen que ambos usen la ruta **R001**. |

Prueba rápida del servidor: `http://localhost:3000/api` y
`http://localhost:3000/api/en-vivo` (últimas posiciones recibidas).
