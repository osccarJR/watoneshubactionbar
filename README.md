WatonesHubActionBar

WatonesHubActionBar es un plugin ligero y optimizado para servidores Paper / Spigot que muestra anuncios persistentes en el ActionBar y en la BossBar, pensado especialmente para lobbies de redes de Minecraft.

Incluye rotación automática de mensajes, animación suave del progreso de la BossBar y recarga en caliente sin reiniciar el servidor.

========================================
CARACTERÍSTICAS
========================================

- ActionBar permanente con rotación de mensajes
- BossBar configurable con anuncios visibles
- Rotación automática de mensajes (ActionBar y BossBar)
- Animación fluida del progreso de la BossBar
- Muy optimizado (una sola task)
- Soporte para colores con & y MiniMessage
- Configuración por mundos
- Comando de reload sin reinicio
- Código limpio, estable y seguro

========================================
REQUISITOS
========================================

Servidor: Paper / Spigot
Versión recomendada: 1.20+
Java: 17+ (Java 21 compatible)

========================================
INSTALACIÓN
========================================

1. Coloca el archivo .jar en la carpeta plugins/
2. Inicia o reinicia el servidor
3. Edita el archivo config.yml
4. Usa /whab reload para aplicar cambios sin reiniciar

========================================
CONFIGURACIÓN - ACTIONBAR
========================================

actionbar:
  messages:
    - "&7ᴍᴄ.ᴡᴀᴛᴏɴᴇꜱ.ɴᴇᴛ"
    - "&dDiscord: &fdiscord.gg/watones"
  interval: 40

========================================
CONFIGURACIÓN - BOSSBAR
========================================

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

========================================
MUNDOS
========================================

worlds:
  - Lobby

Si la lista está vacía, se mostrará en todos los mundos.

========================================
COMANDOS
========================================

/whab reload
Recarga la configuración del plugin sin reiniciar el servidor.

========================================
PERMISOS
========================================

watoneshubactionbar.reload
Permite usar el comando /whab reload
Por defecto: OP

========================================
RENDIMIENTO
========================================

- Una sola task global
- Sin listeners innecesarios
- Consumo extremadamente bajo
- Seguro para lobbies con alta concurrencia
- No usa async con API de Bukkit
- No crea BossBars por jugador

========================================
USOS RECOMENDADOS
========================================

- IP del servidor
- Promociones de la tienda
- Invitaciones a Discord
- Anuncios de modalidades y eventos

========================================
AUTOR
========================================

Emilio
Desarrollado para Watones Network

========================================
LICENCIA
========================================

Licencia MIT.
Puedes modificar y adaptar este plugin libremente.

Plugin diseñado para verse profesional y rendir como uno.
