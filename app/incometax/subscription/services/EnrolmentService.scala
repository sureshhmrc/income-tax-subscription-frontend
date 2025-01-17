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

package incometax.subscription.services

import core.Constants
import core.Constants.GovernmentGateway._
import core.config.AppConfig
import incometax.subscription.connectors.TaxEnrolmentsConnector
import incometax.subscription.models._
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(config: AppConfig,
                                 enrolmentStoreConnector: TaxEnrolmentsConnector,
                                 authConnector: AuthConnector)(implicit ec: ExecutionContext) {

  def enrol(mtditId: String, nino: String)(implicit hc: HeaderCarrier): Future[Either[EnrolFailure, EnrolSuccess.type]] = {
    authConnector.authorise(EmptyPredicate, credentials and groupIdentifier) flatMap {
      case Credentials(ggCred, GGProviderId) ~ Some(groupId) =>
        val enrolmentKey = EnrolmentKey(Constants.mtdItsaEnrolmentName, MTDITID -> mtditId)
        val enrolmentRequest = EmacEnrolmentRequest(ggCred, nino)
        enrolmentStoreConnector.allocateEnrolment(groupId, enrolmentKey, enrolmentRequest)
      case _ ~ None =>
        Future.failed(new InternalServerException("Failed to enrol - user did not have a group identifier (not a valid GG user)"))
      case Credentials(_, _) ~ _ =>
        Future.failed(new InternalServerException("Failed to enrol - user had a different auth provider ID (not a valid GG user)"))
    }
  }

}

