package com.colisweb.tracing.http.server

import cats.data._
import cats.effect._
import com.colisweb.tracing.context.{TracingContext, TracingContextBuilder}
import org.http4s._
import sttp.tapir.Endpoint
import sttp.tapir.server.http4s.{Http4sServerOptions, _}

import scala.reflect.ClassTag

trait TracedRoutes {
  implicit class TracedEndpoint[I, E, O](e: Endpoint[I, E, O, Nothing]) {


    def toTracedRoute[F[_]: Sync](logic: (I, TracingContext[F]) => F[Either[E, O]])(
        implicit builder: TracingContextBuilder[F],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] = {

      TracedHttpRoutes.wrapHttpRoutes(
        Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
          e.toRoutes(input => logic(input, req.tracingContext))(
              serverOptions,
              implicitly,
              implicitly
            )
            .run(req.request)
        },
        builder
      )
    }
  }

  implicit class TracedEndpointRecoverErrors[I, E <: Throwable, O](
      e: Endpoint[I, E, O, Nothing]
  ) {
    def toTracedRouteRecoverErrors[F[_]: Sync](logic: (I, TracingContext[F]) => F[O])(
        implicit builder: TracingContextBuilder[F],
        eClassTag: ClassTag[E],
        cs: ContextShift[F],
        serverOptions: Http4sServerOptions[F]
    ): HttpRoutes[F] =
      TracedHttpRoutes.wrapHttpRoutes(
        Kleisli[OptionT[F, ?], TracedRequest[F], Response[F]] { req =>
          e.toRouteRecoverErrors(input => logic(input, req.tracingContext))(
              serverOptions,
              implicitly,
              implicitly,
              implicitly,
              implicitly
            )
            .run(req.request)
        },
        builder
      )
  }
}
