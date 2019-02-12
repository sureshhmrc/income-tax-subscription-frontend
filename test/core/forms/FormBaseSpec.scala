
package core.forms

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{MessagesApi, MessagesImpl, MessagesProvider}
import play.i18n.Lang

class FormBaseSpec extends PlaySpec with GuiceOneAppPerSuite {

  implicit val messagesProvider: MessagesProvider = {
    MessagesImpl(app.injector.instanceOf[Lang], app.injector.instanceOf[MessagesApi])
  }

}
