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

package core.config.filters

import javax.inject.{Inject, Singleton}

import akka.stream.Materializer
import core.config.AppConfig
import play.api.mvc.Call
import uk.gov.hmrc.whitelist.AkamaiWhitelistFilter

@Singleton
class WhitelistFilter @Inject()(appConfig: AppConfig,
                                val mat: Materializer
                               ) extends AkamaiWhitelistFilter {


  override lazy val whitelist: Seq[String] = appConfig.whitelistIps

  override lazy val destination: Call = Call("GET", appConfig.shutterPage)

  override lazy val excludedPaths: Seq[Call] = appConfig.ipExclusionList

}

