# Punto2 - Red P2P

- [Build & Run](#Build-&-Run)
- [Fuera de localhost](#Fuera-de-localhost)

## Build & Run

1. Instalar dependencias de java
```sh
mvn install
```

2. Correr:

  - Master: `punto1.master.ServerMain`
  - Peers:  `punto1.peer.PeerMain`

3. Abrir la consola del Peer, ingresar `help` para ver los commandos.

4. Para correr mas de un peer:

  - Crear un archivo y ubicarlo dentro de `punto1/peer/resources` (no es mandatorio ubicarlo dentro de esa carpeta pero mantiene ordena el proyecto).
  - Correr el comando:
  ```
  set-db [nombre-archivo]
  ```  
  Esto cambia el json por el default. Ya que cada Peer persiste localmente los archivos que comparte a la red P2P.

## Fuera de localhost

- Para correr peers fuera de localhost será necesario configurar el archivo `peer/resources/master-info.json` con los correspondientes datos del Master.
