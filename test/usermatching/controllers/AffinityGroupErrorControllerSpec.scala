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

package usermatching.controllers

import assets.MessageLookup
import core.controllers.ControllerBaseSpec
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers._

class AffinityGroupErrorControllerSpec extends ControllerBaseSpec {

  override val controllerName: String = "AffinityGroupErrorController"
  override val authorisedRoutes: Map[String, Action[AnyContent]] = Map()

  object TestAffinityGroupErrorController extends AffinityGroupErrorController()(
    MockBaseControllerConfig.applicationConfig,
    messagesApi)

  "Calling the show action of the AffinityGroupErrorController" should {

    lazy val result = TestAffinityGroupErrorController.show(subscriptionRequest)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return 200" in {
      status(result) must be(Status.OK)
    }

    "return HTML" in {
      contentType(result) must be(Some("text/html"))
      charset(result) must be(Some("utf-8"))
    }

    s"have the title '${MessageLookup.AffinityGroup.title}'" in {
      document.title() must be(MessageLookup.AffinityGroup.title)
    }
  }
}
