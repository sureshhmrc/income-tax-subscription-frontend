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

import incometax.subscription.models._
import play.api.libs.json.Json


case object Incomplete

case class NewIncomeSourceModel(rentUkProperty: RentUkPropertyModel,
                                workForYourself: Option[WorkForYourselfModel]) {

  import NewIncomeSourceModel._

  def getIncomeSourceType: Either[Incomplete.type, IncomeSourceType] = {
    (rentUkProperty, workForYourself) match {
      case (RentUkPropertyModel(YES, Some(YES)), _) => Right(Property)
      case (RentUkPropertyModel(YES, Some(NO)), Some(WorkForYourselfModel(YES))) => Right(Both)
      case (RentUkPropertyModel(YES, Some(NO)), Some(WorkForYourselfModel(NO))) => Right(Property)
      case (RentUkPropertyModel(NO, _), Some(WorkForYourselfModel(YES))) => Right(Business)
      case (RentUkPropertyModel(NO, _), Some(WorkForYourselfModel(NO))) => Right(Other)
      case _ => Left(Incomplete)
    }
  }

  def needSecondPage: Boolean = rentUkProperty.needSecondPage

}

object NewIncomeSourceModel {
  implicit val format = Json.format[NewIncomeSourceModel]
  val YES = "Yes"
  val NO = "No"
}
