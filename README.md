# QUARKUS demo Project

Un progetto demo per mostrare alcune funzionalita' di Quarkus.

In particolare la facilita' con cui si avviano i progetti, la facilita' con cui si aggiungono librerie (extensions) e la facilita' con cui si da persistenza agli oggetti tramite Panache.

## Per eseguire la demo

### PASSO 1

Genera il pacchetto per cominciare un progetto da https://code.quarkus.io/

per iniziare aggiungi solo le estensioni RESTEasy JAX-RS e RESTEasy jackson.

scarica lo zip, esplodi il file in una directory locale, apri un terminale ed esegui il comando:

```shell script
./mvnw compile quarkus:dev
```

apri il browser e collegati con http://localhost:8080, accedi anche al servizio http://localhost:8080/hello e verifica che tutto funzioni, welcome page nel primo link e il classico *Hello RESTEasy* nel secondo.

### PASSO 2

In **dev mode** edita la classe *GreetingResource.java* e cambia il saluto, (se non hai l'auto save abilitato salva il file), e ricarica sara' visibile il nuovo messaggio.

**NOTA:** aggiornare di conseguenza anche la classe di test altrimenti la compilazione in fase di build non funziona.


### PASSO 3 - aggiungere le estensioni per MongoDB

In questa parte diamo persistenza a degli oggetti usando MOngoDB e Panache, per questa demo si suppone che ci sia un DB mongo up & running (si può facilmente usare docker o podman).

sempre in **dev mode** da un'altra finestra del terminale aggiungi le estensioni per Panache usando il comando: 

```shell script
./mvnw quarkus:add-extension -Dextensions="quarkus-mongodb-client, quarkus-mongodb-panache"
```

aggiungi un paio di classi Java, NOTA: per la demo non e' importante usare i package ma nel caso la classe Car va messa in un package denominato *model* e la classe CarResource va messa in un package denominato *rest*.

**Car.java**

```shell script
package org.acme;

import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.common.MongoEntity;

@MongoEntity(collection="cars")
public class Car extends PanacheMongoEntity {

    public String brand;
    public String model;

}
```

**CarResource.java**

```shell script
package org.acme;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;

@Path("/cars")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CarResource {
    
    @GET
    public List<PanacheMongoEntityBase> list() {
        return Car.listAll();
    }

    @POST
    public Car create(Car car) {
        car.persist();
        return car;
    }

    @PUT
    public Car update(Car car) {
        car.update();
        return car;
    }

    @DELETE
    @Path("/{id}")
    public void delete(@PathParam("id") String id) {
        Car car = Car.findById(new ObjectId(id));
        if(car == null) {
            throw new NotFoundException();
        }
        car.delete();
    }

}
```

Ora occorre fornire le informazioni per accedere al database mongo, questo avviene tramite alcune properties nel file *src/main/resources/application.properties*, editare il file aggiungendo le seguenti informazioni.

```shell script
quarkus.mongodb.connection-string=${MONGOCONNSTRING:mongodb://localhost:27017/demo}
quarkus.mongodb.database=demo
```

per inserire o modificare o rimuovere oggetti dal database usare, tramite un secondo terminale, i seguenti comandi CURL:

```shell script
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"fiat","model":"panda"}' http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"fiat","model":"punto"}' http://localhost:8080/cars
> curl http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X PUT -d '{"id":"61e5378d40475349c273f4b2","brand":"fiat","model":"croma"}' http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"fiat","model":"punto"}' http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X DELETE http://localhost:8080/cars/61e5378d40475349c273f4b2
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"AlfaRomeo","model":"mito"}' http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"ford","model":"kuga"}' http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"mustang","model":"GT"}' http://localhost:8080/cars
> curl -H "Content-Type: application/json" -X POST -d '{"brand":"mustang","model":"shelby"}' http://localhost:8080/cars
```

### PASSO 4 - lavorare con Panache

Solo al fine di mostrare l'utilizzo di Panache si può a questo punto aggiungere una specifica query al database modificando le due classi precedenti in questo modo:

**Car.java**

aggiungere le seguenti righe:

```shell script
import java.util.List;

...

    public static List<Car> listByBrand(String brand){
        return Car.find("brand = ?1", brand).list();
    }

...
```

**CarResource.java**

aggiungere le seguenti righe:

```shell script
...

    @GET
    @Path("/{brand}")
    public List<Car> getByBrand(@PathParam("brand") String brand) {
        return Car.listByBrand(brand);
    }

...
```

inserire o cancellare oggetti dal database ed utilizzare la ricerca *By Brand*.

### PASSO 5 - mettere l'applicazione in un container

Aggiungere il dockerfile (spiegare il motivo del multi-stage build)

```shell script
# Build stage using Red Hat UBI 8 base image to compile and package application
FROM registry.access.redhat.com/ubi8/openjdk-11 as build

ENV S2I_SOURCE_DEPLOYMENTS_FILTER="*-runner.jar lib"
ENV MAVEN_ARGS_APPEND="-Dquarkus.package.type=uber-jar"

COPY --chown=jboss:0 ./ /tmp/src

RUN /usr/local/s2i/assemble

# Create final image
FROM registry.access.redhat.com/ubi8/openjdk-11
COPY --chown=jboss:0 --from=build /deployments /deployments
```

eseguire la build dell'immagine.
**NOTA:** controllare se nella directory di lavoro e' presente il file *.dockerignore*, se e' presente rimuoverlo perché impedisce il corretto funzionamento del tool S2I.

```shell script
> podman build -t qdemo .
```

eseguire l'applicazione tramite podman 

```shell script
> podman run --rm --name demoapp -d -p 8080:8080 -e MONGOCONNSTRING=mongodb://<mongodb-host>:27017/demo qdemo
```

quando l'applicazione viene eseguita dentro un container l'indirizzo *localhost* non è più valido occorre quindi fornire l'apposita variabile di riferimento, nell'esempio occorre sostituire l'indirizzo con quello ip della macchina dove è in esecuzione il database MongoDB.


### PASSO 6 - creare repo GIT

Aggiungere il progetto ad un repo GIT - [documentation]{https://docs.github.com/en/github/importing-your-projects-to-github/importing-source-code-to-github/adding-an-existing-project-to-github-using-the-command-line}.

questi passaggi richiedono che sia installato sul pc il tool *gh* (GitHub interactive CLI).

```shell script
> git init -b main

> git add . && git commit -m "initial commit"

> gh repo create
```

seguire il processo interattivo.

### PASSO 7 - Red Hat OpenShift

Se è stato creato il repo su GitHub a questo punto è possibile installare l'applicazione su una piattaforma OpenShift.

##### Deploy tramite developer console

Accedere ad openshift usando la vista *developer*.

1. creare un nuovo progetto;
2. dall'opzione **+Add** scegliere la strategia *GIT Repository - import from Git*;
3. specificare la URL del repo Git;
4. specificare gli ulteriori parametri (nome, deployment, ecc.). Si consiglia l'opzione Deployment;
5. completare la creazione con il comando *Create*.

accedere all'applicazione usando l'opportuno link disponibile nella vista *Topology* del progetto.

### PASSO 8 - Autenticazione tramite OIDC 

In questa parte aggiungiamo alcune funzionalita' base per gestire l'identità digitale. Il server di riferimento che gestisce l'identita' e i meccanismi di verifica dell'identita' e' esterno alla demo, in questa demo il server OIDC è erogato da un Red Hat SSO (Keycloak project).

Torniamo in **dev mode** e da un'altra finestra del terminale aggiungiamo le estensioni necessarie a gestire il protocollo OIDC usando il comando: 

```shell script
./mvnw quarkus:add-extension -Dextensions="oidc,keycloak-authorization"
```

nella finestra dove è in esecuzione l'ambiente in **dev mode** noterete dai log a console che dopo aver aggiunto le estensioni vengono generati una serie di errori. E' giusto che cio' accada in quanto l'estensione il runtime va a controllare il server di riferimento gia' allo startup. Occorre quindi aggiungere subito i parametri di configurazione OIDC nel file *src/main/resources/application.properties*.

Aggiungere al file i seguenti valori:

```shell script
# OIDC Configuration
quarkus.oidc.auth-server-url=https://<your-oidc-host>/auth/realms/<realm-name>
quarkus.oidc.client-id=<client-id>
quarkus.oidc.credentials.secret=<your-secret>
#quarkus.oidc.application-type=web-app
quarkus.http.auth.permission.authenticated.paths=/info
quarkus.http.auth.permission.authenticated.policy=authenticated
```

dopo aver inserito le informazioni di cui sopra, ovviamente sostituendo opportunamente i valori corretti, provando a lanciare dal secondo terminale una semplice get tipo:

```shell script
> curl http://localhost:8080/
```

vedrete dalla *dev console* che il runtime riparte questa volta senza errori.

A questo punto aggiungiamo un servizio rest che faccia uso dei meccanismi OIDC, ovvero che sia protetto e allo stesso tempo che permetta di recuperare alcune informazioni dal token di sicurezza JWT. Aggiungiamo le seguenti classi:

**Info.java**

```shell script
package org.acme;

public class Info {
    public String username;
    public String name;
}
```

**UserResource.java**

```shell script
package org.acme;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/info")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    JsonWebToken accessToken;
    
    @GET
    public Info get() {
        Info info = new Info();
        StringBuffer buffer = new StringBuffer();
        buffer.append(accessToken.getClaim("given_name").toString()).append(" ").append(accessToken.getClaim("family_name").toString());
        info.name = buffer.toString();
        info.username = accessToken.getName();
        return info;
    }

}
```

Ci sono 2 cose su cui porre l'attenzione, l'annotation *@Path* che corrisponde al path protetto nelle *application.properties* e l'*idToken* iniettato tramite CDI.

per provare il funzionamento occorre eseguire alcuni comandi tramite riga di comando, nella solita finestra alternativa alla *dev console* eseguiamo quindi i seguenti comandi (sono richiesti i package *curl* e *jq*):

```shell script
> export TKN=$(\
>    curl --insecure -X POST http(s)://<your-oidc-host>/auth/realms/<realm-name>/protocol/openid-connect/token \
>    --user <clientId>:<secret> \
>    -H 'content-type: application/x-www-form-urlencoded' \
>    -d 'username=<userid>&password=<password>&grant_type=password' | jq --raw-output '.access_token' \
> )

> curl http://localhost:8080/info -H "Authorization: Bearer "$TKN

```

se tutto funziona dovrebbe essere visibile un oggetto json contenente la userId ed il nome completo dell'utente specificato. 
