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

package incometax.business.views

import assets.MessageLookup.{BusinessName => messages}
import core.views.ViewSpecTrait
import incometax.business.forms.BusinessNameForm
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class BusinessNameViewSpec extends ViewSpecTrait {

  val backUrl = ViewSpecTrait.testBackUrl
  val action = ViewSpecTrait.testCall

  def page(isEditMode: Boolean, isRegistration: Boolean, addFormErrors: Boolean) = incometax.business.views.html.business_name(
    businessNameForm = BusinessNameForm.businessNameForm.form.addError(addFormErrors),
    postAction = action,
    backUrl = backUrl,
    isRegistration = isRegistration,
    isEditMode = isEditMode
  )(FakeRequest(), applicationMessages, appConfig)

  def documentCore(isEditMode: Boolean, isRegistration: Boolean) = TestView(
    name = s"Business Name View for ${if (isRegistration) "registration" else "sign up"}",
    title = messages.title,
    heading = messages.heading,
    page = page(isEditMode = isEditMode, isRegistration = isRegistration, addFormErrors = false)
  )

  "The Business Name view" should {
    for (isRegistration <- List(false, true)) {
      val testPage = documentCore(isEditMode = false, isRegistration = isRegistration)

      testPage.mustHaveBackLinkTo(backUrl)

      if (isRegistration) testPage.mustHavePara(messages.Registration.line_1)
      else testPage.mustHavePara(messages.SignUp.line_1)

      val form = testPage.getForm("Business Name form")(actionCall = action)

      form.mustHaveTextField(
        name = BusinessNameForm.businessName,
        label = messages.heading,
        showLabel = false)

      form.mustHaveContinueButton()
    }
  }

  "The Business Name view in edit mode" should {
    for (isRegistration <- List(false, true)) {

      val editModePage = documentCore(isEditMode = true, isRegistration = isRegistration)

      editModePage.mustHaveUpdateButton()
    }
  }

  "Append Error to the page title if the form has error" should {
    def documentCore() = TestView(
      name = s"Business Name View for sign up",
      title = titleErrPrefix + messages.title,
      heading = messages.heading,
      page = page(isEditMode = false, isRegistration = false, addFormErrors = true)
    )

    val testPage = documentCore()
  }

}
