/*
 * Copyright 2021 MixtapeBot and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package obsidian.server.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.lang.management.OperatingSystemMXBean
import java.lang.reflect.Method

class CpuTimer {
  val processRecentCpuUsage: Double
    get() = try {
      //between 0.0 and 1.0, 1.0 meaning all CPU cores were running threads of this JVM
      // see com.sun.management.OperatingSystemMXBean#getProcessCpuLoad and https://www.ibm.com/support/knowledgecenter/en/SSYKE2_7.1.0/com.ibm.java.api.71.doc/com.ibm.lang.management/com/ibm/lang/management/OperatingSystemMXBean.html#getProcessCpuLoad()
      val cpuLoad = callDoubleGetter("getProcessCpuLoad", osBean)
      cpuLoad ?: ERROR
    } catch (ex: Throwable) {
      logger.debug("Couldn't access process cpu time", ex)
      ERROR
    }

  val systemRecentCpuUsage: Double
    get() = try {
      //between 0.0 and 1.0, 1.0 meaning all CPU cores were running threads of this JVM
      // see com.sun.management.OperatingSystemMXBean#getProcessCpuLoad and https://www.ibm.com/support/knowledgecenter/en/SSYKE2_7.1.0/com.ibm.java.api.71.doc/com.ibm.lang.management/com/ibm/lang/management/OperatingSystemMXBean.html#getProcessCpuLoad()
      val cpuLoad = callDoubleGetter("getSystemCpuLoad", osBean)
      cpuLoad ?: ERROR
    } catch (ex: Throwable) {
      logger.debug("Couldn't access system cpu time", ex)
      ERROR
    }


  /**
   * The operating system bean used to get statistics.
   */
  private val osBean: OperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean()

  // Code below copied from Prometheus's StandardExports (Apache 2.0) with slight modifications
  private fun callDoubleGetter(getterName: String, obj: Any): Double? {
    return callDoubleGetter(obj.javaClass.getMethod(getterName), obj)
  }

  /**
   * Attempts to call a method either directly or via one of the implemented interfaces.
   *
   *
   * A Method object refers to a specific method declared in a specific class. The first invocation
   * might happen with method == SomeConcreteClass.publicLongGetter() and will fail if
   * SomeConcreteClass is not public. We then recurse over all interfaces implemented by
   * SomeConcreteClass (or extended by those interfaces and so on) until we eventually invoke
   * callMethod() with method == SomePublicInterface.publicLongGetter(), which will then succeed.
   *
   *
   * There is a built-in assumption that the method will never return null (or, equivalently, that
   * it returns the primitive data type, i.e. `long` rather than `Long`). If this
   * assumption doesn't hold, the method might be called repeatedly and the returned value will be
   * the one produced by the last call.
   */
  private fun callDoubleGetter(method: Method, obj: Any): Double? {
    try {
      return method.invoke(obj) as Double
    } catch (e: IllegalAccessException) {
      // Expected, the declaring class or interface might not be public.
    }

    // Iterate over all implemented/extended interfaces and attempt invoking the method with the
    // same name and parameters on each.
    for (clazz in method.declaringClass.interfaces) {
      try {
        val interfaceMethod: Method = clazz.getMethod(method.name, * method.parameterTypes)
        val result = callDoubleGetter(interfaceMethod, obj)

        if (result != null) {
          return result
        }
      } catch (e: NoSuchMethodException) {
        // Expected, class might implement multiple, unrelated interfaces.
      }
    }

    return null
  }

  companion object {
    private const val ERROR = -1.0
    private val logger: Logger = LoggerFactory.getLogger(CpuTimer::class.java)
  }
}
