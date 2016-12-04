/*
 * Copyright (C) 2009-2016 Lightbend Inc. <http://www.lightbend.com>
 */
package controllers

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

import akka.actor._
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.{Keep, Sink, Source}
import org.reactivestreams.Publisher
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

  val (actorRef, publisher) =
    Source.actorRef[String](1000, OverflowStrategy.dropHead).toMat(Sink.asPublisher(fanout = true))(Keep.both).run()

  val sourceFromPublisher = Source.fromPublisher(publisher).log("actorPublisher")

  def streamClock() = Action {
    Ok.chunked(sourceFromPublisher via EventSource.flow).as(ContentTypes.EVENT_STREAM)
  }

  def kickRandomTime() = Action {
    val s = DateTimeFormatter.ofPattern("HH mm ss").format(ZonedDateTime.now().minusSeconds(scala.util.Random.nextInt))
    actorRef ! s
    Ok(s"We sent '$s'")
  }

}
