package trace4cats.sttp.client4

import cats.effect.kernel.{Async, MonadCancelThrow}
import cats.syntax.flatMap._
import cats.syntax.functor._
import sttp.capabilities.{Effect => SttpEffect}
import sttp.client4.ResponseException.UnexpectedStatusCode
import sttp.client4.impl.cats.implicits._
import sttp.client4.{Backend, GenericRequest, Response}
import sttp.model.Headers
import sttp.monad.{MonadError => SttpMonadError}
import trace4cats.Span
import trace4cats.context.Provide
import trace4cats.model.{AttributeValue, SampleDecision, SpanKind, TraceHeaders}
import trace4cats.optics.{Getter, Lens}
import trace4cats.sttp.common.{SttpHeaders, SttpStatusMapping}

class SttpBackendTracer[F[_], G[_], Ctx](
  backend: Backend[F],
  spanLens: Lens[Ctx, Span[F]],
  headersGetter: Getter[Ctx, TraceHeaders],
  spanNamer: SttpSpanNamer,
  dropHeadersWhen: String => Boolean,
  responseAttributesGetter: Getter[Response[_], Map[String, AttributeValue]],
)(implicit P: Provide[F, G, Ctx], F: MonadCancelThrow[F], G: Async[G])
    extends Backend[G] {
  override def send[T](request: GenericRequest[T, Any with SttpEffect[G]]): G[Response[T]] = {
    P.kleislift { parentCtx =>
      val parentSpan = spanLens.get(parentCtx)
      parentSpan
        .child(
          spanNamer(request),
          SpanKind.Client,
          { case UnexpectedStatusCode(body, responseMetadata) =>
            SttpStatusMapping.statusToSpanStatus(body.toString, responseMetadata.code)
          },
        )
        .use { childSpan =>
          val childCtx = spanLens.set(childSpan)(parentCtx)
          val lower = P.provideK(childCtx)
          val ctxBackend = backend.mapK(P.liftK, lower)

          val ctxHeaders = headersGetter.get(childCtx)
          val req = request.headers(SttpHeaders.converter.to(ctxHeaders).headers: _*)

          val reqHeaderAttrs = SttpHeaders.requestFields(Headers(req.headers), dropHeadersWhen)
          val isSampled = childSpan.context.traceFlags.sampled == SampleDecision.Include
          // only extract request attributes if the span is sampled as the host parsing is quite expensive
          val reqExtraAttrs =
            if (isSampled)
              SttpRequest.toAttributes(request)
            else
              Map.empty

          for {
            _ <- childSpan.putAll(reqHeaderAttrs ++ reqExtraAttrs: _*)
            resp <- lower(ctxBackend.send(req))
            _ <- childSpan.setStatus(SttpStatusMapping.statusToSpanStatus(resp.statusText, resp.code))
            respHeaderAttrs = SttpHeaders.responseFields(Headers(resp.headers), dropHeadersWhen)
            // responseAttributesGetter could be expensive, so only call if the span is sampled
            respExtraAttrs =
              if (isSampled)
                responseAttributesGetter.get(resp)
              else
                Map.empty
            _ <- childSpan.putAll(respHeaderAttrs ++ respExtraAttrs: _*)
          } yield resp
        }
    }
  }

  override def monad: SttpMonadError[G] = implicitly

  def close(): G[Unit] = P.lift(backend.close())
}
