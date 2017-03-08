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

package views.business

import assets.MessageLookup.{AccountingPeriod => messages, Base => commonMessages}
import forms.AccountingPeriodForm
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import utils.UnitTestTrait

class BusinessAccountingPeriodViewSpec extends UnitTestTrait {

  lazy val backUrl = controllers.routes.IncomeSourceController.showIncomeSource().url

  lazy val page = views.html.business.accounting_period(
    accountingPeriodForm = AccountingPeriodForm.accountingPeriodForm,
    postAction = controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod(),
    backUrl = backUrl
  )(FakeRequest(), applicationMessages, appConfig)
  lazy val document = Jsoup.parse(page.body)

  "The Business Accounting Period view" should {

    s"have a back buttong pointed to $backUrl" in {
      val backLink = document.select("#back")
      backLink.isEmpty mustBe false
      backLink.attr("href") mustBe backUrl
    }

    s"have the title '${messages.title}'" in {
      document.title() mustBe messages.title
    }

    s"have the heading (H1) '${messages.heading}'" in {
      document.select("h1").text() mustBe messages.heading
    }

    s"have the line_1 (P) '${messages.line_1}'" in {
      document.select("p").text() must include(messages.line_1)
    }

    s"have the line_2 (P) '${messages.line_2}'" in {
      document.select("p").text() must include(messages.line_2)
    }

    "has a form" which {

      s"Has a legend with the text '${commonMessages.startDate}'" in {
        document.select("#startDate legend span.form-label-bold").text() mustBe commonMessages.startDate
      }

      s"Has a legend with the text '${commonMessages.endDate}'" in {
        document.select("#endDate legend span.form-label-bold").text() mustBe commonMessages.endDate
      }

      "has a continue button" in {
        document.select("#continue-button").isEmpty mustBe false
      }

      s"has a post action to '${controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod().url}'" in {
        document.select("form").attr("action") mustBe controllers.business.routes.BusinessAccountingPeriodController.submitAccountingPeriod().url
        document.select("form").attr("method") mustBe "POST"
      }

    }

    "has an accordion" in {
      document.select("details").isEmpty mustBe false
      document.select("details summary").text mustBe messages.hint
      document.select("details div").text mustBe messages.Hint.line_1
    }

  }
}
