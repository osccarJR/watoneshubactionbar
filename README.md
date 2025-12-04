# WatonesHubActionBar

WatonesHubActionBar es un plugin ligero y altamente optimizado para servidores Paper / Spigot, diseñado para mostrar anuncios persistentes en el ActionBar y en la BossBar, ideal para lobbies de redes de Minecraft.

Incluye rotación automática de mensajes, animación fluida del progreso de la BossBar y recarga en caliente sin necesidad de reiniciar el servidor.

--------------------------------------------------
CARACTERÍSTICAS
--------------------------------------------------

- ActionBar permanente con rotación de mensajes
- BossBar configurable con anuncios visibles
- Rotación automática de mensajes (ActionBar y BossBar)
- Animación fluida del progreso de la BossBar
- Extremadamente optimizado (una sola tarea global)
- Soporte para colores con & y MiniMessage
- Configuración por mundos (ideal para lobby)
- Recarga sin reinicio (/whab reload)
- Código limpio, estable y seguro

--------------------------------------------------
REQUISITOS
--------------------------------------------------

Servidor: Paper / Spigot  
Versión recomendada: 1.20+  
Java: 17 o superior (compatible con Java 21)

--------------------------------------------------
INSTALACIÓN
--------------------------------------------------

1. Descarga el archivo .jar del plugin
2. Colócalo en la carpeta plugins/
3. Inicia o reinicia el servidor
4. Edita el archivo config.yml a tu gusto
5. Aplica cambios sin reiniciar usando /whab reload

--------------------------------------------------
CONFIGURACIÓN - ACTIONBAR
--------------------------------------------------

actionbar:
  messages:
    - "&7ᴍᴄ.ᴡᴀᴛᴏɴᴇꜱ.ɴᴇᴛ"
    - "&dDiscord: &fdiscord.gg/watones"
  interval: 40

--------------------------------------------------
CONFIGURACIÓN - BOSSBAR
--------------------------------------------------

bossbar:
  enabled: true

  messages:
    - "&f25% de &dDESCUENTO &fen la tienda"
    - "&fIP oficial: &dmc.watones.net"

  color: PINK
  style: SOLID

  rotation-interval: 4800

  progress:
    enabled: true
    initial: 1.0
    step: 0.0004167
    pingpong: true

--------------------------------------------------
MUNDOS DONDE SE MOSTRARÁ
--------------------------------------------------

worlds:
  - Lobby

Si la lista está vacía, el plugin se mostrará en todos los mundos.

--------------------------------------------------
COMANDOS
--------------------------------------------------

/whab reload
Recarga la configuración del plugin sin reiniciar el servidor.

--------------------------------------------------
PERMISOS
--------------------------------------------------

watoneshubactionbar.reload  
Permite usar el comando /whab reload  
Por defecto: OP

--------------------------------------------------
RENDIMIENTO Y ESTABILIDAD
--------------------------------------------------

- Una sola tarea global
- Sin listeners innecesarios
- Consumo extremadamente bajo
- Seguro para lobbies con alta concurrencia (100+ jugadores)
- No usa tareas async con la API de Bukkit
- No crea BossBars por jugador

--------------------------------------------------
CASOS DE USO RECOMENDADOS
--------------------------------------------------

- IP del servidor
- Promociones de la tienda
- Invitaciones a Discord
- Anuncios de modalidades
- Eventos temporales

--------------------------------------------------
AUTOR
--------------------------------------------------

Emilio  
Desarrollado para Watones Network

--------------------------------------------------
LICENCIA
--------------------------------------------------

Licencia MIT.
Eres libre de modificar este plugin y adaptarlo a tu propio servidor o red.

Plugin diseñado para verse profesional y rendir como uno.
