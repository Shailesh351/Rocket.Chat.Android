package chat.rocket.android.contacts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import chat.rocket.android.R
import chat.rocket.android.chatrooms.adapter.*
import chat.rocket.android.contacts.models.Contact
import chat.rocket.android.db.model.UserEntity
import chat.rocket.android.main.ui.MainActivity
import chat.rocket.android.util.extensions.inflate
import kotlinx.coroutines.experimental.launch

class ContactRecyclerViewAdapter(
        private val context: MainActivity,
        private val contactArrayList: List<ItemHolder<*>>

) : RecyclerView.Adapter<ViewHolder<*>>() {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<*> {
        return when (viewType) {
            VIEW_TYPE_CONTACT -> {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding: ViewDataBinding = DataBindingUtil.inflate(layoutInflater, R.layout.item_contact, parent, false)
                ContactViewHolder(binding.root)
            }
            VIEW_TYPE_HEADER -> {
                val view = parent.inflate(R.layout.item_heading)
                ContactHeaderViewHolder(view)
            }
            VIEW_TYPE_INVITE -> {
                val view = parent.inflate(R.layout.item_invite)
                InviteViewHolder(view)
            }
            else -> throw IllegalStateException("View type must be either Room, Header or Invite")
        }
    }

    override fun getItemCount() = contactArrayList.size

    override fun getItemId(position: Int): Long {
        val item = contactArrayList[position]
        return when (item) {
            is ContactItemHolder -> item.data.hashCode().toLong()
            is ContactHeaderItemHolder -> item.data.hashCode().toLong()
            is inviteItemHolder -> item.data.hashCode().toLong()
            else -> throw IllegalStateException("View type must be either Room, Header or Invite")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (contactArrayList[position]) {
            is ContactItemHolder -> VIEW_TYPE_CONTACT
            is ContactHeaderItemHolder -> VIEW_TYPE_HEADER
            is inviteItemHolder -> VIEW_TYPE_INVITE
            else -> throw IllegalStateException("View type must be either Room, Header or Invite")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder<*>, position: Int) {
        if (holder is ContactViewHolder) {
            holder.bind(contactArrayList[position] as ContactItemHolder)

            val contact: Contact = holder.data!!.data
            val username = contact.getUsername()
            var user: UserEntity?
            if (username != null) {
                launch {
                    user = context.presenter.getUser(username!!)
                    if (user != null) {
                        contact.setStatus(user!!.status)
                    }
                    holder.setContactStatus(contact)
                }
            }

            val inviteButton: Button = holder.itemView.findViewById(R.id.invite_contact)
            val dmButton: Button = holder.itemView.findViewById(R.id.chat_username)

            inviteButton.setOnClickListener { view ->
                run {
                    inviteButton.setText("INVITED")
                    if (contact.isPhone()) {
                        context.presenter.inviteViaSMS(contact.getPhoneNumber()!!)
                    } else {
                        context.presenter.inviteViaEmail(contact.getEmailAddress()!!)
                    }
                }
            }

            dmButton.setOnClickListener { view ->
                run {
                    context.presenter.openDirectMessageChatRoom(contact.getUsername().toString())
                }
            }

         } else if (holder is ContactHeaderViewHolder) {
            holder.bind(contactArrayList[position] as ContactHeaderItemHolder)

        } else if (holder is InviteViewHolder) {
            holder.bind(contactArrayList[position] as inviteItemHolder)
            holder.itemView.setOnClickListener {
                shareApp()
            }
         }
    }

    private fun shareApp() {
        context.presenter.shareViaApp(context)
    }

    companion object {
        const val VIEW_TYPE_CONTACT = 1
        const val VIEW_TYPE_HEADER = 2
        const val VIEW_TYPE_INVITE = 4
    }
}