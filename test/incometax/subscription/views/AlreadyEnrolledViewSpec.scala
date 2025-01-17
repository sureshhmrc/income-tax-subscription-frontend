/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package incometax.subscription.views

import assets.MessageLookup
import assets.MessageLookup.{AlreadyEnrolled => messages, Base => common}
import core.controllers.SignOutController
import core.models.DateModel
import core.views.ViewSpecTrait
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._

class AlreadyEnrolledViewSpec extends ViewSpecTrait {

  val submissionDateValue = DateModel("1", "1", "2016")
  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = incometax.subscription.views.html.enrolled.already_enrolled()(request, applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Already Enrolled view" should {

    s"have the title '${MessageLookup.AlreadyEnrolled.title}'" in {
      document.title() must be(MessageLookup.AlreadyEnrolled.title)
    }

          s"has a heading (H1)" which {

        lazy val heading = document.select("H1")

        s"has the text '${MessageLookup.AlreadyEnrolled.heading}'" in {
          heading.text() mustBe MessageLookup.AlreadyEnrolled.heading
        }

        s"has a line '${MessageLookup.AlreadyEnrolled.line1}'" in {
          document.select(".form-group").text must be(MessageLookup.AlreadyEnrolled.line1)
        }
      }

    "have a sign out button" in {
      val actionSignOut = document.getElementById("sign-out-button")
      actionSignOut.attr("role") mustBe "button"
      actionSignOut.text() mustBe MessageLookup.Base.signOut
      actionSignOut.attr("href") mustBe SignOutController.signOut(request.path).url
    }

  }
}
