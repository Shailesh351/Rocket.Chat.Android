package chat.rocket.android.chatroom.service

import android.app.job.JobParameters
import android.app.job.JobService
import chat.rocket.android.analytics.AnalyticsManager
import chat.rocket.android.db.DatabaseManagerFactory
import chat.rocket.android.server.domain.GetAccountsInteractor
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import chat.rocket.android.server.infraestructure.DatabaseMessageMapper
import chat.rocket.android.server.infraestructure.DatabaseMessagesRepository
import chat.rocket.core.internal.realtime.socket.connect
import chat.rocket.core.internal.rest.sendMessage
import chat.rocket.core.model.Message
import dagger.android.AndroidInjection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import chat.rocket.core.internal.realtime.socket.model.State


class MessageService : JobService() {
    @Inject
    lateinit var factory: ConnectionManagerFactory
    @Inject
    lateinit var dbFactory: DatabaseManagerFactory
    @Inject
    lateinit var getAccountsInteractor: GetAccountsInteractor
    @Inject
    lateinit var analyticsManager: AnalyticsManager

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return false
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        GlobalScope.launch(Dispatchers.IO) {
            getAccountsInteractor.get().forEach { account ->
                retrySendingMessages(params, account.serverUrl)
            }
            jobFinished(params, false)
        }
        return true
    }

    private suspend fun retrySendingMessages(params: JobParameters?, serverUrl: String) {
        val dbManager = dbFactory.create(serverUrl)
        val messageRepository =
            DatabaseMessagesRepository(dbManager, DatabaseMessageMapper(dbManager))
        val temporaryMessages = messageRepository.getAllUnsent()
            .sortedWith(compareBy(Message::timestamp))
        if (temporaryMessages.isNotEmpty()) {
            val client = factory.create(serverUrl).client
            temporaryMessages.forEach { message ->
                try {
                    if(client.state is State.Disconnected || client.state is State.Waiting){
                        client.connect()
                    }
                    client.sendMessage(
                        message = message.message,
                        messageId = message.id,
                        roomId = message.roomId,
                        avatar = message.avatar,
                        attachments = message.attachments,
                        alias = message.senderAlias
                    )
                    messageRepository.save(message.copy(synced = true))
                    analyticsManager.logMessageSent(message.type.toString(), serverUrl)
                    Timber.d("Sent scheduled message given by id: ${message.id}")
                } catch (ex: Exception) {
                    analyticsManager.logSendMessageException(temporaryMessages.size, ex.toString(), serverUrl)
                    Timber.e(ex)
                    // TODO - remove the generic message when we implement :userId:/message subscription

                    if (ex is IllegalStateException) {
                        Timber.e(ex, "Probably a read-only problem...")
                        // TODO: For now we are only going to reschedule when api is fixed.
                        messageRepository.removeById(message.id)
                        jobFinished(params, false)
                    } else {
                        // some other error
                        if (ex.message?.contains("E11000", true) == true) {
                            // XXX: Temporary solution. We need proper error codes from the api.
                            messageRepository.save(message.copy(synced = false))
                        }
                        jobFinished(params, true)
                    }
                }
            }
        }
    }

    companion object {
        const val RETRY_SEND_MESSAGE_ID = 1
    }
}