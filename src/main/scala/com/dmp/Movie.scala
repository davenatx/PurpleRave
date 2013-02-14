package com.dmp

import com.typesafe.config._
import dispatch._
import net.liftweb.json._
import scala.annotation.tailrec
import com.weiglewilczek.slf4s.Logging

// RottenTomatoes REST Model for lists
case class RotMovie(id: String, title: String, year: Int, mpaa_rating: String, runtime: Int, critics_consensus: Option[String], release_dates: RotReleaseDate,
  ratings: RotRating, synopsis: String, posters: RotPoster, abridged_cast: List[RotCast], alternate_ids: RotAltId, links: RotLink)
case class RotReleaseDate(theater: String, dvd: String)
case class RotRating(critics_rating: String, critics_score: Int, audience_rating: String, audience_score: Int) // Stoped
case class RotPoster(thumbnail: String, profile: String, detailed: String, original: String)
case class RotCast(name: String, id: String, characters: List[String]) //List of Cast in Movie
case class RotDirector(name: String)
case class RotAltId(imdb: String)
case class RotLink(`self`: String, alternate: String, cast: String, clips: String, reviews: String, similar: String)

// Same as movie plus genres, abridged_directors, and studio
case class RotMovieInfo(id: BigInt, title: String, year: Int, genres: List[String], mpaa_rating: String, runtime: Int, critics_consensus: Option[String],
  release_dates: RotReleaseDate, ratings: RotRating, synopsis: String, posters: RotPoster, abridged_cast: List[RotCast], abridged_directors: List[RotDirector],
  studio: String, alternate_ids: RotAltId, links: RotLink)

// Output for database
case class PurpleRaveMovie(title: String, dvdReleaseDate: String, cost: Float, genre: String, mpaaRating: String) {
  override def toString = title + "," + dvdReleaseDate + "," + cost + "," + genre + "," + mpaaRating
}

/**
 * Parse Top Rentals from Rotten Tomatoes
 *
 * http://api.rottentomatoes.com/api/public/v1.0
 */
object RotTomatoes extends Logging {

  implicit val formats = DefaultFormats // Brings in default date formats etc

  lazy val config = ConfigFactory.load("settings.properties") // Properties file
  lazy val apiKey = config.getString("apiKey")
  lazy val country = config.getString("country")

  val setupRequest = (endPoint: String, params: Map[String, String]) => url(endPoint) <<? params

  /** Can take a limit up to 50 */
  def topRentals(limit: String): List[RotMovie] = {
    val endPoint = "http://api.rottentomatoes.com/api/public/v1.0/lists/dvds/top_rentals.json"
    val params = Map("limit" -> limit, "country" -> country, "apikey" -> apiKey)

    // Transform children of movies to List[JValues]
    val toMoviesJson = (json: String) => (parse(json) \ "movies").children

    // Setup the request
    val topRentalSvc = setupRequest(endPoint, params)

    // Make request and retrieve JSON response
    val json = for {
      str <- Http(topRentalSvc OK as.String)() // Blocking
    } yield str

    toMoviesJson(json) map (_.extract[RotMovie])
  }

  /** Return a specific movie */
  def movieInfo(id: String): RotMovieInfo = {
    val endPoint = "http://api.rottentomatoes.com/api/public/v1.0/movies/" + id + ".json"
    val params = Map("apikey" -> apiKey)

    // Transform children of movies to List[JValues]
    val toMovieJson = (json: String) => parse(json)

    // Setup the request
    val movieInfoSvc = setupRequest(endPoint, params)

    // Make request and retrieve JSON response
    val json = for {
      str <- Http(movieInfoSvc OK as.String)() // Blocking
    } yield str

    toMovieJson(json).extract[RotMovieInfo]
  }

  /** Print to CSV File */
  def printCSV(f: java.io.File)(op: java.io.PrintWriter => Unit) {
    val p = new java.io.PrintWriter(f)
    try { op(p) } finally { p.close() }
  }

  /** Build Model with PurpleRaveMovie Entries */
  @tailrec
  def buildCSV(xs: List[RotMovie], acc: List[PurpleRaveMovie]): List[PurpleRaveMovie] = xs match {
    case head :: tail => {
      Thread.sleep(1000)
      logger.info("Reading Movie Info: " + head.title)
      val mInfo = movieInfo(head.id)
      buildCSV(tail, (PurpleRaveMovie(mInfo.title, mInfo.release_dates.dvd, 25.00f, mInfo.genres.head, mInfo.mpaa_rating)) :: acc)
    }
    case List() => acc.reverse
  }

  def main(args: Array[String]) {
    val file = new java.io.File("purple_rave.csv")

    printCSV(file) {
      writer =>
        {
          for (movie <- buildCSV(topRentals("50"), List())) writer.println(movie)
        }
    }

  }

}