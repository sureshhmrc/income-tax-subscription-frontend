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

package connectors

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.models.Enrolment
import play.api.http.Status._
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EnrolmentConnector @Inject()(appConfig: AppConfig,
                                   val http: HttpGet) {

  def getIncomeTaxSAEnrolment(uri: String)(implicit hc: HeaderCarrier): Future[Option[Enrolment]] = {
    val getUrl = s"${appConfig.authUrl}$uri/enrolments"
    http.GET[HttpResponse](getUrl).map {
      response =>
        response.status match {
          case OK => response.json.as[Seq[Enrolment]].find(_.key == EnrolmentConnector.enrolmentKey)
          case _ => None
        }
    }
  }

}

object EnrolmentConnector {

  val enrolmentKey = "HMRC-MTD"
  val enrolmentIdentifier = "MTDITID"

}