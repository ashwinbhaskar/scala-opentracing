package com.colisweb.tracing.context

import java.util.UUID

import cats.data._
import cats.effect._
import com.colisweb.tracing.context.datadog.DDTracingContext
import com.colisweb.tracing.core.{Tags, TracingContext, TracingContextResource}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import TestUtils._
import _root_.datadog.opentracing._

class LogCorrelationSpec extends AnyFunSpec with StrictLogging with Matchers {

  implicit val slf4jLogger: org.slf4j.Logger = logger.underlying

  describe("Datadog log correlation") {
    it("Should log trace id as a JSON field when TracingContext has a trace id") {
      val traceId = UUID.randomUUID().toString
      val context = mockDDTracingContext(OptionT.none, OptionT.pure(traceId))
      testStdOut(
        context.logger.info("Hello"),
        _ should include(
          s""""dd.trace_id":"$traceId""""
        )
      )
    }

    it("Should log span id as JSON field when TracingContext has a span id") {
      val spanId = UUID.randomUUID().toString
      val context = mockDDTracingContext(OptionT.pure(spanId), OptionT.none)
      testStdOut(
        context.logger.info("Hello"),
        _ should include(
          s""""dd.span_id":"$spanId""""
        )
      )
    }

    it("Should not add anything when TracingContext has neither a span id nor a trace id") {
      val context = mockDDTracingContext(OptionT.none, OptionT.none)
      testStdOut(
        context.logger.info("Hello"),
        _ should (not include ("trace_id") and not include ("span_id"))
      )
    }
  }

  private def mockDDTracingContext(
      _spanId: OptionT[IO, String],
      _traceId: OptionT[IO, String]
  ): TracingContext[IO] = {
    val tracer = DDTracer.builder().build()
    val span = tracer.activeSpan()

    new DDTracingContext[IO](tracer, span, "Mocked service", UUID.randomUUID().toString) {
      override def span(operationName: String, tags: Tags): TracingContextResource[IO] = ???
      override def spanId: cats.data.OptionT[cats.effect.IO, String] = _spanId
      override def traceId: cats.data.OptionT[cats.effect.IO, String] = _traceId
      override def addTags(tags: Tags): cats.effect.IO[Unit] = ???
    }
  }
}
