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

package forms.validation.testutils

import forms.validation.ErrorMessageFactory
import forms.validation.utils.ConstraintUtil.constraint
import play.api.data.validation.{Constraint, Invalid}

object DataMap {

  import forms.BusinessNameForm._
  import forms.SoleTraderForm
  import forms.EmailForm._
  import forms.IncomeTypeForm
  import forms.TermForm._
  import forms.IncomeSourceForm
  import forms.NotEligibleForm._
  import forms.submapping.DateMapping._
  import forms.PropertyIncomeForm
  import forms.preferences.BackToPreferencesForm


  type DataMap = Map[String, String]

  val EmptyMap = DataMap()

  def DataMap(elems: (String, String)*): DataMap = Map(elems: _*)

  def date(prefix: String)(day: String, month: String, year: String): DataMap =
    Map(prefix * dateDay -> day, prefix * dateMonth -> month, prefix * dateYear -> year)

  val emptyDate: String => DataMap = (prefix: String) => date(prefix)("", "", "")

  def busName(name: String): DataMap = Map(businessName -> name)

  def soleTrader(isSoleTrader: String): DataMap = Map(SoleTraderForm.soleTrader -> isSoleTrader)

  def incomeType(iType: String): DataMap = Map(IncomeTypeForm.incomeType -> iType)

  def propertyIncomeValue(iType: String): DataMap = Map(PropertyIncomeForm.incomeValue -> iType)

  def incomeSource(iType: String): DataMap = Map(IncomeSourceForm.incomeSource -> iType)

  def notEligibleChoice(iType: String): DataMap = Map(choice -> iType)

  def email(email: String): DataMap = Map(emailAddress -> email)

  def terms(acceptedTerms: String): DataMap = Map(hasAcceptedTerms -> acceptedTerms)

  def terms(acceptedTerms: Boolean): DataMap = terms(acceptedTerms.toString)

  def acceptPaperlessDataMap(accept: String): DataMap = Map(BackToPreferencesForm.backToPreferences -> accept)

  val alwaysFailInvalid: Invalid = ErrorMessageFactory.error("always fail")

  def alwaysFail[T]: Constraint[T] = constraint[T]((t: T) => alwaysFailInvalid)

}