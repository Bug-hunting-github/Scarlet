/*
 * © 2018 Match Group, LLC.
 */

package com.tinder.scarlet.v2

import com.tinder.scarlet.MessageAdapter
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.internal.servicemethod.MessageAdapterResolver
import com.tinder.scarlet.internal.servicemethod.StreamAdapterResolver
import com.tinder.scarlet.internal.utils.RuntimePlatform
import com.tinder.scarlet.messageadapter.builtin.BuiltInMessageAdapterFactory
import com.tinder.scarlet.retry.BackoffStrategy
import com.tinder.scarlet.streamadapter.builtin.BuiltInStreamAdapterFactory
import com.tinder.scarlet.v2.service.Coordinator
import com.tinder.scarlet.v2.service.LifecycleEventSource
import com.tinder.scarlet.v2.service.Session
import com.tinder.scarlet.v2.service.StateMachineFactory
import com.tinder.scarlet.v2.service.TimerEventSource
import com.tinder.scarlet.v2.stub.StubInterface
import com.tinder.scarlet.v2.stub.StubMethod
import io.reactivex.schedulers.Schedulers

// TODO transition adapter
interface EventAdapter {

    interface Factory {

    }
}

interface EventAdapterResolver {


}

class Scarlet internal constructor(
    private val stubInterfaceFactory: StubInterface.Factory
) {

    fun <T> create(service: Class<T>): T = stubInterfaceFactory.create(service)

    /**
     * Same as [create].
     */
    inline fun <reified T : Any> create(): T = create(T::class.java)

    data class Configuration(
        val protocol: Protocol,
        val topic: Topic = Topic.Main,
        val lifecycle: Lifecycle,
        val backoffStrategy: BackoffStrategy,
        val streamAdapterFactories: List<StreamAdapter.Factory> = emptyList(),
        val messageAdapterFactories: List<MessageAdapter.Factory> = emptyList()
    )

    class Factory {

        // TODO protocol coordinator cache

        fun create(configuration: Configuration): Scarlet {

            val coordinator = Coordinator(
                StateMachineFactory(),
                Session(
                    configuration.protocol,
                    configuration.topic
                ),
                LifecycleEventSource(
                    configuration.lifecycle
                ),
                TimerEventSource(
                    Schedulers.io(),
                    configuration.backoffStrategy
                ),
                Schedulers.io()
            )

            val stubInterfaceFactory = StubInterface.Factory(
                RuntimePlatform.get(),
                coordinator,
                StubMethod.Factory(
                    configuration.createStreamAdapterResolver(),
                    configuration.createMessageAdapterResolver()
                )
            )

            return Scarlet(stubInterfaceFactory)
        }

        private fun Configuration.createStreamAdapterResolver(): StreamAdapterResolver {
            return StreamAdapterResolver(streamAdapterFactories + BuiltInStreamAdapterFactory())
        }

        private fun Configuration.createMessageAdapterResolver(): MessageAdapterResolver {
            return MessageAdapterResolver(messageAdapterFactories + BuiltInMessageAdapterFactory())
        }

    }
}
