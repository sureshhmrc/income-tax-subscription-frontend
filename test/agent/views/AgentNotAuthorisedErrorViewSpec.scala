/*
 * Copyright 2018 HM Revenue & Customs
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

package agent.views

import agent.assets.MessageLookup.{AgentNotAuthorisedError => messages, Base => common}
import core.views.ViewSpecTrait
import play.api.i18n.Messages.Implicits._

class AgentNotAuthorisedErrorViewSpec extends ViewSpecTrait {

  val action = ViewSpecTrait.testCall
  val request = ViewSpecTrait.viewTestRequest

  lazy val page = agent.views.html.agent_not_authorised(action)(request, applicationMessages, appConfig)

  "The Agent Not Authorised Error view" should {
    val testPage = TestView(
      name = "Agent Not Authorised Error view",
      title = messages.title,
      heading = messages.heading,
      page = page
    )

    testPage.mustHavePara(
      messages.para1
    )

    testPage.mustHaveContinueToSignUpButton()

    testPage.mustHaveSignOutLink(common.signOut, request.path)
  }
}