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

package connectors.matching

import javax.inject.{Inject, Singleton}

import config.AppConfig
import connectors.httpparsers.LockoutStatusHttpParser._
import connectors.models.matching.LockOutRequest
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost}

import scala.concurrent.Future

@Singleton
class UserLockoutConnector @Inject()(val appConfig: AppConfig,
                                     val httpGet: HttpGet,
                                     val httpPost: HttpPost
                                      ) {

  def agentLockoutUrl(arn: String): String = appConfig.clientMatchingUrl + UserLockoutConnector.agentLockoutUri(arn)

  def lockoutAgent(arn: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
    httpPost.POST[LockOutRequest, LockoutStatusResponse](agentLockoutUrl(arn), LockOutRequest(appConfig.matchingLockOutSeconds))

  def getLockoutStatus(arn: String)(implicit hc: HeaderCarrier): Future[LockoutStatusResponse] =
    httpGet.GET[LockoutStatusResponse](agentLockoutUrl(arn))

}

object UserLockoutConnector {
  def agentLockoutUri(arn: String): String = s"/lock/$arn"
}
