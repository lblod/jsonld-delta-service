# JSON-LD Delta Service

Provides a JSON-LD representation of the consolidated [delta-notifier](https://github.com/mu-semtech/delta-notifier)
messages

## Configuration

### Add JSON-LD Delta Service to the stack

The following assumes a [semantic works application stack](https://semantic.works/docs)

Include the service in `docker-compose.yml`.

```
  jsonld-delta-service:
    image: lblod/jsonld-delta-service:0.3.0
    volumes:
      - ./data/files:/share
      - ./config/kalliope:/config
    environment:
      SERVER_PORT: "80"
      LOGGING_LEVEL: "INFO"
      SPARQL_ENDPOINT: "http://database:8890/sparql"
      SPRING_SECURITY_CONFIG: "/config/security.yml"
```

### Configure the dispatcher

Add the jsonld-delta-service routes to the [dispatcher](https://github.com/mu-semtech/mu-dispatcher) configuration.
e.g.:

```
...
match "/v3/api-docs/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://jsonld-delta-service/v3/api-docs/"
  end

  match "/consolidated/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://jsonld-delta-service/consolidated/"
  end
...


```

### Add security config

- Create a file under `/config/kalliope/security.yml`
- Paste the following:

 ```
  application:
  security:
    enabled: true
    source: /config/source.json
    output: /config/out.json
    allowedIpAddresses:
      - 10.10.10.10 # list of whitelisted ips
server:
  forward-headers-strategy: NATIVE
  tomcat:
    remote-ip-header: x-real-ip # letsencrypt proxy

```

- If it's the first time app is started in this development server, you need to create the credentials.
- Create a file under `/config/kalliope/source.json`
- Add the users (change values accordingly). The file will be automatically deleted at startup and replaced by an
  encrypted one under `config/kalliope/out.json`
- Example of `/config/kalliope/source.json` :

``` 
     [
    {
      "username": "boris",
      "password": "5678",
      "roles": [
        "ADMIN"
      ]
    },
    {
      "username": "nordine",
      "password": "1234",
      "roles": [
        "USER"
      ]
    }
  ]

```

### Boot up the system

Boot your microservices-enabled system using docker-compose.

    cd /path/to/mu-project
    docker-compose up

You can shut down using `docker-compose stop` and remove everything using `docker-compose rm`.

## Usage

OpenAPI documentation is available via `/v3/api-docs/`

### `/consolidated`

The JSON-LD response contains:

- A named graph with the consolidated representation of all delta messages. i.e. the result of applying all inserted and
  delete messages.
- The default graph contains a timestamp to be used in subsequent requests

```json
   ...
],
"@id": "http://mu.semte.ch/graphs/kalliope/consolidated",
"date": "2021-05-27T09:26:03.351256816Z",
  "@context": {
    "date": {
      "@id": "http://purl.org/dc/terms/date",
      "@type": "http://www.w3.org/2001/XMLSchema#dateTime"
    }
  }
}
```
