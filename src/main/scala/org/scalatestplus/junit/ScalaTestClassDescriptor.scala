/*
 * Copyright 2001-2022 Artima, Inc.
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
package org.scalatestplus.junit

import org.junit.platform.engine.support.descriptor.{AbstractTestDescriptor, ClassSource}
import org.junit.platform.engine.{TestDescriptor, UniqueId}

/**
 * <code>TestDescriptor</code> for ScalaTest suite.
 *
 * @param parent The parent descriptor.
 * @param theUniqueId The unique ID.
 * @param suiteClass The class of the ScalaTest suite.
 */
class ScalaTestClassDescriptor(parent: TestDescriptor, val theUniqueId: UniqueId, val suiteClass: Class[_]) extends AbstractTestDescriptor(theUniqueId, suiteClass.getName, ClassSource.from(suiteClass)) {

  // TODO: Need to add a anom test so that this will be executed, is there a better way?
  addChild(new ScalaTestDescriptor(theUniqueId.append("test", "anom"), "anom"))

  /**
   * Type of this <code>ScalaTestClassDescriptor</code>.
   *
   * @return <code>TestDescriptor.Type.CONTAINER</code>
   */
  override def getType: TestDescriptor.Type = TestDescriptor.Type.CONTAINER
}

object ScalaTestClassDescriptor {
  val segmentType = "class"
}