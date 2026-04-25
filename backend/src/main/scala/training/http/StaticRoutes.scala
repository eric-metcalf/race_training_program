package training.http

import cats.data.OptionT
import cats.effect.IO
import org.http4s.dsl.io.*
import org.http4s.{HttpRoutes, Request, StaticFile}

/** Serve the React+Vite SPA bundle out of `classpath:/public/`.
  *
  * Mounts at `/` as a fallback after the API and OpenAPI routes:
  *  - GET `/assets/foo.js` → resolves `/public/assets/foo.js`
  *  - GET `/foo/bar`       → file not found, falls back to `/public/index.html`
  *    so client-side routing (TanStack Router) can take over
  *  - HEAD/OPTIONS/etc.    → not handled here; passes through to the next route
  */
object StaticRoutes:

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] { case req @ GET -> _ =>
    val rel = req.uri.path.toString.stripPrefix("/")
    val resource = if rel.isEmpty || rel == "/" then "index.html" else rel
    serveResource(s"/public/$resource", req)
      .orElse(serveResource("/public/index.html", req))
      .getOrElseF(NotFound("not found"))
  }

  private def serveResource(path: String, req: Request[IO]) =
    OptionT(StaticFile.fromResource[IO](path, Some(req)).value)
