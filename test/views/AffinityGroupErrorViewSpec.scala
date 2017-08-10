/*
 * Copyright 2017 HM Revenue & Customs
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

package views

import assets.MessageLookup.{AffinityGroup => messages}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class AffinityGroupErrorViewSpec extends ViewSpecTrait {

  lazy val page = views.html.affinity_group_error()(FakeRequest(), applicationMessages, appConfig)

  "The Affinity Group Error view" should {

    val testPage = TestView(
      name = "Affinity Group Error view",
      title = messages.title,
      heading = messages.heading,
      page = page)

    testPage.mustHavePara(messages.line1)
    testPage.mustHavePara(messages.line2)

    val div = testPage.getById("affinity div", "signOut")
    val para = div.selectHead("affinity link", "p")

    para.mustHaveALink("sign in using a different type of account.", controllers.routes.SignOutController.signOut().url)
  }

}