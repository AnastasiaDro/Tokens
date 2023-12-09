package com.cerebus.tokens.reinforcement_photo.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import by.kirich1409.viewbindingdelegate.viewBinding
import com.cerebus.tokens.logger.api.LoggerFactory
import com.cerebus.tokens.reinforcement_photo.R
import com.cerebus.tokens.reinforcement_photo.databinding.DialogAskForReinforcementImageBinding
import org.koin.android.ext.android.inject

/**
 * [AskForReinforcementImageDialog] - a dialog for selecting a new image of the reinforcement
 *
 * @author Anastasia Drogunova
 * @since 09.12.2023
 */
class AskForReinforcementImageDialog: DialogFragment(R.layout.dialog_ask_for_reinforcement_image) {

    private val viewBinding: DialogAskForReinforcementImageBinding by viewBinding()
    private val loggerFactory: LoggerFactory by inject()
    private val logger = loggerFactory.createLogger(this::class.java.simpleName)
    private val viewModel: ChangePhotoViewModel by viewModel()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d("dialog view created")
    }

    private fun initButtons() = with(viewBinding) {

        cancelButton.setOnClickListener {
            dismiss()
            logger.d("cancel photo selection")
        }

    }
}