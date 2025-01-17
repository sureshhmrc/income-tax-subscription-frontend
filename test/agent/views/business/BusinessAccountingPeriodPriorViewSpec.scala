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

package agent.views.business

import agent.assets.MessageLookup.Business.{AccountingPeriodPrior => messages}
import agent.forms.AccountingPeriodPriorForm
import core.forms.submapping.YesNoMapping
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import core.views.ViewSpecTrait

class BusinessAccountingPeriodPriorViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  private def page(isEditMode: Boolean, addFormErrors: Boolean) = agent.views.html.business.accounting_period_prior(
    accountingPeriodPriorForm = AccountingPeriodPriorForm.accountingPeriodPriorForm.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean) =
    TestView(
      name = "Business Accounting Period View",
      title = messages.title,
      heading = messages.heading,
      page = page(isEditMode = isEditMode, addFormErrors = false)
    )

  "The Business Accounting Period view" should {

    val testPage = documentCore(isEditMode = false)

    testPage.mustHaveBackLinkTo(backUrl)


    val form = testPage.getForm("Business Accounting Period form")(actionCall = action)

    form.mustHaveRadioSet(
      legend = messages.heading,
      radioName = AccountingPeriodPriorForm.accountingPeriodPrior
    )(
      YesNoMapping.option_yes -> messages.yes,
      YesNoMapping.option_no -> messages.no
    )

    form.mustHaveContinueButton()

  }

  "The Business Accounting Period view in edit mode" should {
    val editModePage = documentCore(isEditMode = true)

    editModePage.mustHaveUpdateButton()
  }

  "Append Error to the page title if the form has error" should {
    def documentCore() =
      TestView(
        name = "Business Accounting Period View",
        title = titleErrPrefix + messages.title,
        heading = messages.heading,
        page = page(isEditMode = false, addFormErrors = true)
      )

    val testPage = documentCore()
  }

}
