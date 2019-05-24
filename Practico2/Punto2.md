# Punto2 - Concurrencia

- [Build & Run](#Build-&-Run)
- [Conclusiones](#Conclusiones)

### Build & Run

1. Instalar dependencias de java
```sh
mvn install
```

- Correr las clases en el siguiente orden para forzar el error de actualizacion:
```sh
ExtraccionServer
DepositoServer
ClienteDeposito
ClienteExtraccion
ClienteExtraccion
```

### Conclusiones

Se decide sincronizar las partes de código en las que se lee, procesa escribe en el archivo de saldos. Forzando así que las transacciones sean atómicas y que no se modifiquen entre ellas. Ejemplo de transaccion sincronizada:

```java
synchronized (br) {
  // Lectura -------
  Double saldo = new Double(br.readLine());
  log.info(" [-] *Antes* de Deposito -> Monto:" + monto + ", Saldo:" + saldo);
  // Proceso -------
  saldo += monto;
  try {
    Thread.sleep(40);
  } catch (InterruptedException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
  // Escritura --------
  FileWriter writer = new FileWriter(filename);
  writer.write(String.valueOf(saldo), 0, String.valueOf(saldo).length());
  String json = gson.toJson("Deposito Exitoso! Saldo Actual: "+saldo);
  outputChannel.print(json);
  log.info(" [-] Deposito Exitoso!");
  writer.close();
  log.info(" [-] *Despues* de Deposito -> Monto:" + monto + ", Saldo:" + saldo);
  br.close();
}
```
