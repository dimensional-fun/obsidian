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

package obsidian.server.io

import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.NanoIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingIpRoutePlanner
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

object RoutePlannerUtil {
    /**
     * Detail information block for an AbstractRoutePlanner
     */
    fun getDetailBlock(planner: AbstractRoutePlanner): RoutePlannerStatus.IRoutePlannerStatus {
        val ipBlock = planner.ipBlock
        val ipBlockStatus = IpBlockStatus(ipBlock.type.simpleName, ipBlock.size.toString())

        val failingAddresses = planner.failingAddresses
        val failingAddressesStatus = failingAddresses.entries.map {
            FailingAddress(it.key, it.value, Date(it.value).toString())
        }

        return when (planner) {
            is RotatingIpRoutePlanner -> RotatingIpRoutePlannerStatus(
                ipBlockStatus,
                failingAddressesStatus,
                planner.rotateIndex.toString(),
                planner.index.toString(),
                planner.currentAddress.toString()
            )

            is NanoIpRoutePlanner -> NanoIpRoutePlannerStatus(
                ipBlockStatus,
                failingAddressesStatus,
                planner.currentAddress.toString()
            )

            is RotatingNanoIpRoutePlanner -> RotatingNanoIpRoutePlannerStatus(
                ipBlockStatus,
                failingAddressesStatus,
                planner.currentBlock.toString(),
                planner.addressIndexInBlock.toString()
            )

            else -> GenericRoutePlannerStatus(ipBlockStatus, failingAddressesStatus)
        }
    }
}

data class RoutePlannerStatus(
    val `class`: String?,
    val details: IRoutePlannerStatus?
) {
    interface IRoutePlannerStatus
}

@Serializable
data class GenericRoutePlannerStatus(
    @SerialName("ip_block")
    val ipBlock: IpBlockStatus,

    @SerialName("failing_addresses")
    val failingAddresses: List<FailingAddress>
) : RoutePlannerStatus.IRoutePlannerStatus

@Serializable
data class RotatingIpRoutePlannerStatus(
    @SerialName("ip_block")
    val ipBlock: IpBlockStatus,

    @SerialName("failing_addresses")
    val failingAddresses: List<FailingAddress>,

    @SerialName("rotate_index")
    val rotateIndex: String,

    @SerialName("ip_index")
    val ipIndex: String,

    @SerialName("current_address")
    val currentAddress: String
) : RoutePlannerStatus.IRoutePlannerStatus

@Serializable
data class FailingAddress(
    @SerialName("failing_address")
    val failingAddress: String,

    @SerialName("failing_timestamp")
    val failingTimestamp: Long,

    @SerialName("failing_time")
    val failingTime: String
) : RoutePlannerStatus.IRoutePlannerStatus

@Serializable
data class NanoIpRoutePlannerStatus(
    @SerialName("ip_block")
    val ipBlock: IpBlockStatus,

    @SerialName("failing_addresses")
    val failingAddresses: List<FailingAddress>,

    @SerialName("current_address_index")
    val currentAddressIndex: String
) : RoutePlannerStatus.IRoutePlannerStatus

@Serializable
data class RotatingNanoIpRoutePlannerStatus(
    @SerialName("ip_block")
    val ipBlock: IpBlockStatus,

    @SerialName("failing_addresses")
    val failingAddresses: List<FailingAddress>,

    @SerialName("block_index")
    val blockIndex: String,

    @SerialName("current_address_index")
    val currentAddressIndex: String
) : RoutePlannerStatus.IRoutePlannerStatus

@Serializable
data class IpBlockStatus(val type: String, val size: String)
