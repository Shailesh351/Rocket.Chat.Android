package chat.rocket.android.helper

import chat.rocket.android.core.lifecycle.CancelStrategy
import chat.rocket.android.db.DatabaseManager
import chat.rocket.android.server.domain.CurrentServerRepository
import chat.rocket.android.server.infraestructure.ConnectionManagerFactory
import chat.rocket.common.model.RoomType
import chat.rocket.common.model.roomTypeOf
import chat.rocket.core.internal.rest.chatRoomRoles
import chat.rocket.core.model.ChatRoomRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class ChatRoomRoleHelper @Inject constructor(
	private val dbManager: DatabaseManager,
	private val strategy: CancelStrategy,
	serverInteractor: CurrentServerRepository,
	factory: ConnectionManagerFactory
) {
	private var currentServer = serverInteractor.get()!!
	private val manager = factory.create(currentServer)
	private val client = manager.client

	suspend fun getChatRoles(chatRoomId: String): List<ChatRoomRole> {
		val (chatRoomType, chatRoomName) = getChatRoomDetails(chatRoomId)

		return try {
			if (roomTypeOf(chatRoomType) !is RoomType.DirectMessage) {
				withContext(Dispatchers.IO + strategy.jobs) {
					client.chatRoomRoles(
						roomType = roomTypeOf(chatRoomType),
						roomName = chatRoomName
					)
				}
			} else {
				emptyList()
			}
		} catch (ex: Exception) {
			Timber.e(ex)
			emptyList()
		}
	}

	private suspend fun getChatRoomDetails(chatRoomId: String): Pair<String, String> {
		return withContext(Dispatchers.IO + strategy.jobs) {
			return@withContext dbManager.getRoom(chatRoomId)?.chatRoom.let {
				Pair(it?.type ?: "", it?.name ?: "")
			}
		}
	}
}