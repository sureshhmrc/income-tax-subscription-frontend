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

package incometax.incomesource.views

import assets.MessageLookup.{Base => common, CannotUseService => messages}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class CannotUseServiceViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall

  lazy val page = incometax.incomesource.views.html.cannot_use_service(
    postAction = action)(
    FakeRequest(),
    implicitMessages,
    appConfig
  )

  "The Cannot Use Service view" should {

    val testPage = TestView(
      name = "Cannot Use Service View",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(messages.line1)

    testPage.mustHaveSignOutLink(common.signOut)

  }

}

