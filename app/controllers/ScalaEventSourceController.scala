/*
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package controllers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Source}
import play.api.http.ContentTypes
import play.api.libs.EventSource
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class ScalaEventSourceController @Inject()(
  implicit actorSystem: ActorSystem,
  mat: Materializer,
  ec: ExecutionContext) extends Controller with ScalaTicker {

  def index() = Action {
    Ok(views.html.scalaeventsource())
  }

  // Obtain a Sink and Source which will publish and receive from the "bus" respectively.
  val (sink, source) =
    MergeHub.source[String](perProducerBufferSize = 16)
      .toMat(BroadcastHub.sink(bufferSize = 256))(Keep.both)
      .run()

  def streamClock() = Action {
    Ok.chunked(source via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  def kickRandomTime() = Action {
    val s = DateTimeFormatter.ofPattern("HH mm ss").format(ZonedDateTime.now().minusSeconds(scala.util.Random.nextInt))

    Source.single(s).runWith(sink)
    Ok(s"We sent '$s'")
  }

}
