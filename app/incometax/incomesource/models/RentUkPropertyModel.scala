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

package incometax.incomesource.models

import play.api.libs.json.Json

case class RentUkPropertyModel(rentUkProperty: String, onlySourceOfSelfEmployedIncome: Option[String]) {

  import NewIncomeSourceModel._

  def needSecondPage: Boolean = this match {
    case RentUkPropertyModel(YES, Some(YES)) => false
    case RentUkPropertyModel(YES, Some(NO)) => true
    case RentUkPropertyModel(NO, _) => true
  }
}

object RentUkPropertyModel{
  implicit val format = Json.format[RentUkPropertyModel]

  implicit class RentUkPropertyModelUtil(rentUkPropertyModel: Option[RentUkPropertyModel]) {
    def needSecondPage: Boolean = rentUkPropertyModel.fold(false)(_.needSecondPage)
  }

}

