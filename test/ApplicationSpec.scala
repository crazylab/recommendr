import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.JsArray
import play.api.test.Helpers._
import play.api.test._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
//@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends PlaySpec with OneAppPerSuite  {
  implicit override lazy val app = new FakeApplication(additionalConfiguration = Map(("minimumSupport","0")))

//  sequential
  "Application" must {

      "send 404 on a bad request" in {
          route(FakeRequest(GET, "/boum"))
      }

      "search for product " in {

          val search = route(FakeRequest(GET, "/products/search/ICE")).get
          status(search) mustBe OK
          contentType(search) mustBe Some("application/json")
          contentAsString(search) must include("ICE")

      }

      "render the index page" in {
        val home = route(FakeRequest(GET, "/")).get

        status(home) mustBe OK
        contentType(home) mustBe Some("text/html")
        contentAsString(home) must include( "Your new application is ready.")
      }

      "post transactions " in {
        val header: FakeHeaders = FakeHeaders(Seq(("Content-Type", "application/json")))
        val trx = route(FakeRequest(POST, "/transactions", header, "[[\"p1\", \"p2\",\"p3\"],[\"p2\",\"p3\",\"p4\"]]")).get
        status(trx) mustBe OK
      }
      "trigger processing " in {
        val trx = route(FakeRequest(POST, "/updateScore")).get
        status(trx) mustBe OK
      }
      "zget asoociation reco " in {
        Thread.sleep(20000) //TODO any better way to test async processing?
        val req = route(FakeRequest(GET, "/products/p2")).get
        status(req) mustBe OK
        println(contentAsString(req))
        val products: JsArray = contentAsJson(req).as[JsArray]
        products.value.length must be > 0
      }

  }
}
