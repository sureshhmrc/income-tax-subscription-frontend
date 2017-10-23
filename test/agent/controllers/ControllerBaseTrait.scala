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

package agent.controllers

import agent.auth.MockConfig
import agent.config.{AppConfig, BaseControllerConfig}
import agent.utils.UnitTestTrait


trait ControllerBaseTrait extends UnitTestTrait {

  def mockBaseControllerConfig(appConfig: AppConfig): BaseControllerConfig = new BaseControllerConfig(
    applicationConfig = appConfig) {
    override lazy val postSignInRedirectUrl = appConfig.ggSignInContinueUrl
  }

  lazy val MockBaseControllerConfig = mockBaseControllerConfig(MockConfig)

}
