# JSON-LD Delta Service

Provides a JSON-LD representation of the consolidated [delta-notifier](https://github.com/mu-semtech/delta-notifier) messages

## Configuration

### Add JSON-LD Delta Service to the stack
The following assumes a [semantic works application stack](https://semantic.works/docs)

Include the service in `docker-compose.yml`.

```
  jsonld-delta-service:
    image: lblod/jsonld-delta-service
    volumes:
      - ./data/files:/share
    environment:
      SERVER_PORT: "80"
      LOGGING_LEVEL: "INFO"
      SPARQL_ENDPOINT: "http://database:8890/sparql"
```

### Configure Delta Notifier
Include the delta notifier into the stack following the instructions documented on [delta-notifier](https://github.com/mu-semtech/delta-notifier). And configure `config/delta/rules.js` to send delta messages to the jsonld delta service in the `v0.0.1` format. e.g.:
```
export default [
    {
      match: {
        subject: {
        }
      },
      callback: {
        url: "http://jsonld-delta-service/delta", method: "POST"
      },
      options: {
        resourceFormat: "v0.0.1",
        gracePeriod: 1000,
        ignoreFromSelf: true
      }
    }
  ]
```
### Configure the dispatcher

Add the jsonld-delta-service routes to the [dispatcher](https://github.com/mu-semtech/mu-dispatcher) configuration. e.g.:
```
...
match "/v3/api-docs/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://jsonld-delta-service/v3/api-docs/"
  end

  match "/changes/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://jsonld-delta-service/changes/"
  end

  match "/consolidated/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://jsonld-delta-service/consolidated/"
  end
...
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
- A named graph with the consolidated representation of all delta messages. i.e. the result of applying all instert and delete messages.
- The default graph contains a timestamp to be used in subsequent requests to the `/changes` route. e.g.
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

### `/changes`
Use the date from a previous `/consolidated` or `/changes` response as `since` parameter.

The response contains:
- Two named graphs with respectively the deleted and inserted triples since the provided date.
- The default graph contains a timestamp to be used in subsequent requests. Same as for the `/changes` response.

