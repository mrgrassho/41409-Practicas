# INFORME - TP1
 
## Pasos para su implementaciòn
<ul>
  <li>
    <b>1. Descargar el repositorio:</b>
    Una vez ingresado al link de GitHub hacer click en Clone or Downland > Download ZIP 
  </li>
  <li>
    <b>2. Descomprimir ZIP </b>
    En linux: en Terminal ubicarse en la carpeta del ZIP e ingresar "unzip  41409-Practicas-master.zip  <u>carpeta</u>"
    En Windows: Click derecho en 41409-Practicas-master.zip > Extraer Aqui.
  </li>
  <li>
    <b> 3. Importar proyecto</b> 
    <p>En Eclipse: </p> 
              <p> 3.1 Ir a File> Import. </p> 
              <p> 3.2 Abrir carpeta Maven, seleccionar Existing Maven Proyects y hacer click en Next. </p>
              <p> 3.3 En la nueva ventana que se abre hacer click en Browse... Buscar el directorio donde se encuentra el proyecto    
                   descargado</p>
              <p> 3.4 Luego para finalizar, click en Finish</p>
  </li>
   <li>
    <b>4. Ejecuciòn.</b>
    <p>Tendrà que ser en este orden para poder lograr el objetivo de los ejercicios: </p>
       <p>4.1 Ejecutar el servidor </p>
          <p>  -Para punto1: ServerTCP </p>
           <p> -Para punto2: ServerTCP </p>
           <p> -Para punto3y4: MQServer </p>
           <p> -Para punto5: ServerMain </p>
           <p> -Para punto6: ServerMain </p>
           <p> -Para punto6: ServerMain </p>
       <p> 4.2 Ejecutar el Cliente
           <p> -Para punto1: CLientTCP </p>
           <p> -Para punto2: CLientTCP </p>
           <p> -Para punto3y4: MQCLient </p>
           <p> -Para punto5: ClientRMI </p>
           <p> -Para punto6: ClientRMI </p>
           <p> -Para punto6: ClienteMain </p>
   </li>
</ul>

## Conclusiones
1) Resultado: El mensaje se enviò desde el cliente hasta el servidor, y se obtuvo una respuesta con ese mismo mensaje aclarando quien lo envìa. Como observaciòn, se aclara que el Servidor no puede aceptar la peticiòn de mas de un cliente al mismo tiempo.

6) Al introducir el error desde el lado del servidor, especificamente en la linea de codigo 15 y 35 de las funciones de suma y de resta respectivamente, se puede apreciar que uno de los dos vectores se inicializa nuevamente, por ende queda vacìo. 
Cuando volvemos al cliente, con el resultado de la suma o resta de los vectores, vemos que siguen con los mismos valores que se cargo del lado del cliente, sin sufrir los cambios que se hicieron anteriormente en los procesos del servidor. 
<p>Se concluye que la forma de pasajes de parametros por RMI es "por valor". No puede ser "por referencia" debido que, al poder estar ubicados en distintos lugares y equipos, no tendràn un esquema de direcciones a memoria en comun para acceder a los datos de las variables.</p>

