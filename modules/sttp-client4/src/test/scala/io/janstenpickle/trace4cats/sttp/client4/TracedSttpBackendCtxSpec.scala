package io.janstenpickle.trace4cats.sttp.client4

import cats.data.Kleisli
import cats.effect.IO
import trace4cats.ToHeaders
import io.janstenpickle.trace4cats.sttp.client4.Instances._
import trace4cats.sttp.client4.syntax._
import trace4cats.sttp.common.{RunIOToId, TraceContext}

class TracedSttpBackendCtxSpec
    extends BaseSttpBackendTracerSpec[IO, Kleisli[IO, TraceContext[IO], *], TraceContext[IO]](
      RunIOToId,
      TraceContext("bf2665b3-2201-466d-868d-8bd3ab151d79", _),
      _.liftTraceContext(spanLens = TraceContext.span[IO], headersGetter = TraceContext.headers[IO](ToHeaders.standard))
    )
