# INFORME - TP1
 
## Pasos para su ejecución
<ul>
  <li>
    1. Descargar el repositorio:
    <p> Una vez ingresado al link de GitHub hacer click en Clone or Downland > Download ZIP  </p>
  </li>
  <li>
    2. Descomprimir ZIP 
    <p> En linux: en Terminal ubicarse en la carpeta del ZIP e ingresar "unzip  41409-Practicas-master.zip  <u>carpeta</u>" </p>
    <p> En Windows: Click derecho en 41409-Practicas-master.zip > Extraer Aqui. </p>
  </li>
  <li>
    2. Importar proyecto 
    <p> En Eclipse ir a File> Import. </p>
    <p> Abrir carpeta Maven, seleccionar Existing Maven Proyects y poner Next. 
    <p> En la nueva ventana que se abre hacer click en Browse... Buscar el directorio donde se encuentra el proyecto descargado y luego para finalizar, click en Finish</p>
  </li>
</ul>

## Conclusiones
1) Resultado: El mensaje se enviò desde el cliente hasta el servidor, y se obtuvo una respuesta con ese mismo mensaje aclarando quien lo envìa. Como observaciòn, se aclara que el Servidor no puede aceptar la peticiòn de mas de un cliente al mismo tiempo.

6) Al introducir el error desde el lado del servidor, especificamente en la linea de codigo 15 y 35 de las funciones de suma y de resta respectivamente, se puede apreciar que uno de los dos vectores se inicializa nuevamente, por ende queda vacìo. 
Cuando volvemos al cliente, con el resultado de la suma o resta de los vectores, vemos que siguen con los mismos valores que se cargo del lado del cliente, sin sufrir los cambios que se hicieron anteriormente en los procesos del servidor. 
Se concluye que la forma de pasajes de parametros por RMI es "por valor". No puede ser "por referencia" debido que, al poder estar ubicados en distintos lugares y equipos, no tendràn un esquema de direcciones a memoria en comun para acceder a los datos de las variables.

