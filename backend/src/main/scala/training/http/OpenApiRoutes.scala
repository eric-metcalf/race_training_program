package training.http

import cats.effect.IO
import org.http4s.HttpRoutes
import sttp.apispec.openapi.circe.yaml.*
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

object OpenApiRoutes:

  def routes: HttpRoutes[IO] =
    val swagger = SwaggerInterpreter().fromEndpoints[IO](
      training.http.Endpoints.all,
      "Race Training API",
      "0.1.0"
    )
    Http4sServerInterpreter[IO]().toRoutes(swagger)

  /** OpenAPI YAML at /openapi.yaml — useful for clients that prefer YAML. */
  def openApiYaml: String =
    OpenAPIDocsInterpreter()
      .toOpenAPI(Endpoints.all, "Race Training API", "0.1.0")
      .toYaml
