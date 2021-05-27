defmodule Dispatcher do
  use Matcher

  define_accept_types [
    json: [ "application/json", "application/vnd.api+json" ],
    html: [ "text/html", "application/xhtml+html" ],
    sparql: [ "application/sparql-results+json" ],
    any: [ "*/*" ]
  ]

  define_layers [ :static, :sparql, :api_services, :frontend_fallback, :resources, :not_found ]

  options "/*path", _ do
    conn
    |> Plug.Conn.put_resp_header( "access-control-allow-headers", "content-type,accept" )
    |> Plug.Conn.put_resp_header( "access-control-allow-methods", "*" )
    |> send_resp( 200, "{ \"message\": \"ok\" }" )
  end

  ###############
  # SPARQL
  ###############
  match "/sparql", %{ layer: :sparql, accept: %{ sparql: true } } do
    forward conn, [], "http://db:8890/sparql"
  end

  ###############
  # API SERVICES
  ###############
  match "/v3/api-docs/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/v3/api-docs"
  end

  match "/delta/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/delta"
  end

  match "/bulk/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/bulk"
  end

  match "/changes/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/changes"
  end

  match "/inserts/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/inserts"
  end

  match "/deletes/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/deletes"
  end

  match "/consolidated/*path", %{ layer: :api_services, accept: %{ json: true } } do
    forward conn, path, "http://kalliope-api/consolidated"
  end

  #################
  # NOT FOUND
  #################
  match "/*_path", %{ layer: :not_found } do
    send_resp( conn, 404, "Route not found.  See config/dispatcher.ex" )
  end

end
