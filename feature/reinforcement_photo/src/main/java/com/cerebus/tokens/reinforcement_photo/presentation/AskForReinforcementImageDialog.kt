package com.cerebus.tokens.reinforcement_photo.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
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

    private val getFromGalleryResultLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
           val image: Uri? = result.data?.data
            viewBinding.reinforcementImage.setImageURI(image)
        } catch (e: Exception) {
            Toast.makeText(requireActivity(), "No imageselected", Toast.LENGTH_LONG).show()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logger.d("dialog view created")
    }

    private fun initButtons() = with(viewBinding) {

        cancelButton.setOnClickListener {
            dismiss()
            logger.d("cancel photo selection")
        }

        makePhotoButton.setOnClickListener {
            //TODO
            logger.d("ask to make a photo")

        }

        getFromGalleryButton.setOnClickListener {
            getFromGalleryResultLauncher.launch(Intent(MediaStore.ACTION_PICK_IMAGES))
            //TODO
            logger.d("ask to get a photo from gallery")
        }
    }
}