/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.analytics

import android.os.Bundle
import no.nordicsemi.android.toolbox.lib.utils.Profile

/**
 * Base class for Firebase Analytics events.
 */
sealed class FirebaseEvent(val eventName: String, val params: Bundle?)

/**
 * Represents an event that is logged when the app is opened.
 * This event does not carry any additional parameters.
 */
data object AppOpenEvent : FirebaseEvent("APP_OPEN", null)

/**
 * Creates a new instance of [LinkOpenEvent] with the given link.
 * The link's display name is used as a parameter.
 *
 * @param link The link that was opened.
 */
class LinkOpenEvent(link: Link) : FirebaseEvent(
    eventName = EVENT_NAME,
    params = createParams(link),
) {
    private companion object {
        const val EVENT_NAME = "LINK_OPEN"
        const val PARAM_KEY = "LINK"

        fun createParams(link: Link) = Bundle().apply {
            putString(PARAM_KEY, link.displayName)
        }
    }
}

/**
 * Represents an event that is logged when a profile is connected.
 * This event can be created with a [Profile] or a [Link].
 */
class ProfileConnectedEvent(profile: Profile) : FirebaseEvent(
    eventName = EVENT_NAME,
    params = createParams(profile),
) {

    private companion object {
        const val EVENT_NAME = "PROFILE_CONNECTED"
        const val PARAM_KEY = "PROFILE_NAME"

        fun createParams(profile: Profile) = Bundle().apply {
            putString(PARAM_KEY, profile.toString())
        }
    }
}

/**
 * Represents an event related to UART (Universal Asynchronous Receiver-Transmitter) analytics.
 */
sealed class UARTAnalyticsEvent(eventName: String, params: Bundle?) :
    FirebaseEvent(eventName, params)

/**
 * Represents an event that is logged when a UART message is send or received.
 * This event can be created with a [UARTMode].
 */
class UARTSendAnalyticsEvent(mode: UARTMode) : UARTAnalyticsEvent(
    eventName = EVENT_NAME,
    params = createParams(mode),
) {

    companion object {
        const val EVENT_NAME = "UART_SEND_EVENT"
        const val PARAM_KEY = "MODE"

        fun createParams(mode: UARTMode) = Bundle().apply {
            putString(PARAM_KEY, mode.displayName)
        }
    }
}

/**
 * Represents an event that is logged when a UART preset configuration is created.
 */
class UARTCreateConfiguration : UARTAnalyticsEvent(EVENT_NAME, null) {

    private companion object {
        const val EVENT_NAME = "UART_CREATE_CONF"
    }
}

/**
 * Represents an event that is logged when a UART preset configuration is changed.
 *
 * This event does not carry any additional parameters.
 */
class UARTChangeConfiguration : UARTAnalyticsEvent(EVENT_NAME, null) {

    private companion object {
        const val EVENT_NAME = "UART_CHANGE_CONF"
    }
}
