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

package agent.services

import javax.inject.{Inject, Singleton}

import agent.connectors.AgentServicesConnector

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class ClientRelationshipService @Inject()(val agentServicesConnector: AgentServicesConnector) {
  def isPreExistingRelationship(arn: String, nino: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    agentServicesConnector.isPreExistingRelationship(arn, nino)
  }

  def createClientRelationship(arn: String, mtdid: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    agentServicesConnector.createClientRelationship(arn, mtdid)
  }
}