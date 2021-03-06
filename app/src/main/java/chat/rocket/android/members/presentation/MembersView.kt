package chat.rocket.android.members.presentation

import chat.rocket.android.core.behaviours.LoadingView
import chat.rocket.android.core.behaviours.MessageView
import chat.rocket.android.members.uimodel.MemberUiModel

interface MembersView: LoadingView, MessageView {

    /**
     * Shows a list of members of a room.
     *
     * @param dataSet The data set to show.
     * @param total The total number of members.
     * @param clearDataSet If true it will clear the previous data set.
     */
    fun showMembers(dataSet: List<MemberUiModel>, total: Long, clearDataset: Boolean)

    /**
     * Show Invite Users Button
     **/
    fun showInviteUsersButton()

    /**
     * Hide Invite Users Button
     **/
    fun hideInviteUserButton()
}