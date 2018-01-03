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

package incometax.subscription.models

import core.Constants
import core.Constants.GovernmentGateway._
import play.api.libs.json.{JsValue, Json, Writes}

case class EmacEnrolmentRequest(userId: String, nino: String)

object EmacEnrolmentRequest {
  implicit val writes = new Writes[EmacEnrolmentRequest] {
    override def writes(request: EmacEnrolmentRequest): JsValue = {
      Json.obj(
        "userId" -> request.userId,
        "friendlyName" -> Constants.GovernmentGateway.ggFriendlyName,
        "type" -> "principal",
        "verifiers" -> Json.arr(
          Json.obj(
            "key" -> NINO,
            "value" -> request.nino
          )
        )
      )
    }
  }
}
