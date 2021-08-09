package com.chooloo.www.koler.ui.dialpad

import ContactsUtils
import android.content.Context
import android.os.Bundle
import android.telephony.PhoneNumberFormattingTextWatcher
import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DEL
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.chooloo.www.koler.R
import com.chooloo.www.koler.databinding.DialpadBinding
import com.chooloo.www.koler.ui.base.BaseFragment
import com.chooloo.www.koler.ui.contacts.ContactsFragment
import com.chooloo.www.koler.ui.widgets.DialpadKey
import com.chooloo.www.koler.util.*
import com.chooloo.www.koler.util.AudioManager.Companion.SHORT_VIBRATE_LENGTH
import com.chooloo.www.koler.call.CallManager

class DialpadFragment : BaseFragment(), DialpadContract.View {
    private val _audioManager by lazy { AudioManager(baseActivity) }
    private val _contactsManager by lazy { ContactsUtils(baseActivity) }
    private var _onTextChangedListener: (text: String?) -> Unit? = { _ -> }
    private val _binding by lazy { DialpadBinding.inflate(layoutInflater) }
    private val _animationManager by lazy { AnimationManager(baseActivity) }
    private val _presenter by lazy { DialpadPresenter<DialpadContract.View>(this) }
    private var _onKeyDownListener: (keyCode: Int, event: KeyEvent) -> Unit? = { _, _ -> }
    private val _suggestionsFragment by lazy { ContactsFragment.newInstance(true, false) }

    override val isDialer by lazy { argsSafely.getBoolean(ARG_IS_DIALER) }

    override val suggestionsCount: Int
        get() = _suggestionsFragment.itemCount

    override var number: String
        get() = _binding.dialpadEditText.text.toString()
        set(value) {
            _binding.dialpadEditText.setText(value)
        }

    override var isSuggestionsVisible: Boolean
        get() = _binding.dialpadSuggestionsScrollView.visibility == VISIBLE
        set(value) {
            if (value && !isSuggestionsVisible) {
                _animationManager.bounceIn(_binding.dialpadSuggestionsScrollView)
            } else if (!value && isSuggestionsVisible) {
                _binding.dialpadSuggestionsScrollView.visibility = GONE
            }
        }

    override var isAddContactButtonVisible: Boolean
        get() = _binding.dialpadButtonAddContact.visibility == VISIBLE
        set(value) {
            if (value && !isAddContactButtonVisible) {
                _animationManager.bounceIn(_binding.dialpadButtonAddContact)
            } else if (!value && isAddContactButtonVisible) {
                _animationManager.showView(_binding.dialpadButtonAddContact, false)
            }
        }

    override var isDeleteButtonVisible: Boolean
        get() = _binding.dialpadButtonDelete.visibility == VISIBLE
        set(value) {
            if (value && !isDeleteButtonVisible) {
                _animationManager.bounceIn(_binding.dialpadButtonDelete)
            } else if (!value && isDeleteButtonVisible) {
                _animationManager.showView(_binding.dialpadButtonDelete, false)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = _binding.root

    override fun onSetup() {
        _suggestionsFragment.setOnContactsChangedListener(_presenter::onSuggestionsChanged)

        _binding.apply {
            dialpadButtonAddContact.setOnClickListener { _presenter.onAddContactClick() }
            dialpadButtonCall.apply {
                visibility = if (isDialer) VISIBLE else GONE
                setOnClickListener { _presenter.onCallClick() }
            }
            dialpadButtonDelete.apply {
                setOnClickListener { _presenter.onDeleteClick() }
                setOnLongClickListener { _presenter.onLongDeleteClick() }
            }
            dialpadEditText.apply {
                isClickable = isDialer
                isLongClickable = isDialer
                isFocusableInTouchMode = isDialer
                isCursorVisible = isDialer

                if (isDialer) {
                    addTextChangedListener(PhoneNumberFormattingTextWatcher())
                }
                setText(argsSafely.getString(ARG_NUMBER))
                addOnTextChangedListener {
                    _presenter.onTextChanged(it)
                    _onTextChangedListener.invoke(it)
                }
            }

            View.OnClickListener {
                (it as DialpadKey).keyCode.also {
                    _presenter.onKeyClick(it)
                    _onKeyDownListener.invoke(it, KeyEvent(ACTION_DOWN, it))
                }
            }
                .also {
                    key0.setOnClickListener(it)
                    key1.setOnClickListener(it)
                    key2.setOnClickListener(it)
                    key3.setOnClickListener(it)
                    key4.setOnClickListener(it)
                    key5.setOnClickListener(it)
                    key6.setOnClickListener(it)
                    key7.setOnClickListener(it)
                    key8.setOnClickListener(it)
                    key9.setOnClickListener(it)
                    keyHex.setOnClickListener(it)
                    keyStar.setOnClickListener(it)
                }

            View.OnLongClickListener {
                _presenter.onLongKeyClick((it as DialpadKey).keyCode)
            }
                .also {
                    key0.setOnLongClickListener(it)
                    key1.setOnLongClickListener(it)
                    key2.setOnLongClickListener(it)
                    key3.setOnLongClickListener(it)
                    key4.setOnLongClickListener(it)
                    key5.setOnLongClickListener(it)
                    key6.setOnLongClickListener(it)
                    key7.setOnLongClickListener(it)
                    key8.setOnLongClickListener(it)
                    key9.setOnLongClickListener(it)
                    keyHex.setOnLongClickListener(it)
                    keyStar.setOnLongClickListener(it)
                }

            _presenter.onTextChanged(dialpadEditText.text.toString())
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        childFragmentManager
            .beginTransaction()
            .add(_binding.dialpadSuggestionsContainer.id, _suggestionsFragment)
            .commitNow()
    }


    //region dialpad view

    override fun call() {
        if (number.isEmpty()) {
            baseActivity.showMessage(R.string.error_enter_number)
        } else {
            CallManager.call(baseActivity, _binding.dialpadEditText.text.toString())
        }
    }

    override fun vibrate() {
        _audioManager.vibrate(SHORT_VIBRATE_LENGTH)
    }

    override fun backspace() {
        _binding.dialpadEditText.onKeyDown(KEYCODE_DEL, KeyEvent(ACTION_DOWN, KEYCODE_DEL))
    }

    override fun addContact() {
        _contactsManager.openAddContactView(_binding.dialpadEditText.text.toString())
    }

    override fun callVoicemail() {
        CallManager.callVoicemail(baseActivity)
    }

    override fun playTone(keyCode: Int) {
        _audioManager.playToneByKey(keyCode)
    }

    override fun invokeKey(keyCode: Int) {
        _binding.dialpadEditText.onKeyDown(keyCode, KeyEvent(ACTION_DOWN, keyCode))
    }

    override fun setSuggestionsFilter(filter: String) {
        _suggestionsFragment.applyFilter(filter)
    }

    //endregion


    fun setOnTextChangedListener(onTextChangedListener: (text: String?) -> Unit?) {
        _onTextChangedListener = onTextChangedListener
    }

    fun setOnKeyDownListener(onKeyDownListener: (keyCode: Int, event: KeyEvent) -> Unit?) {
        _onKeyDownListener = onKeyDownListener
    }


    companion object {
        const val TAG = "dialpad_bottom_dialog_fragment"
        const val ARG_IS_DIALER = "dialer"
        private const val ARG_NUMBER = "number"

        fun newInstance(isDialer: Boolean, number: String? = null) = DialpadFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_DIALER, isDialer)
                putString(ARG_NUMBER, number)
            }
        }
    }

}