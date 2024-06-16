package com.example.ontariooutbacknavigator

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape

class AddWaypointDialogFragment(private val lat : Double?, private val lng : Double?, private val listener : AddWaypointListener) : DialogFragment() {

    var chosenColor = Color.BLACK
    private lateinit var chooseColorButton: Button

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val parentActivity = requireActivity()
            val builder = AlertDialog.Builder(it)
            val inflater = parentActivity.layoutInflater
            val view = inflater.inflate(R.layout.fragment_add_waypoint_dialog, null)
            val latitudeBox = view.findViewById<EditText>(R.id.latitudeBox)
            val longitudeBox = view.findViewById<EditText>(R.id.longitudeBox)
            val descriptionBox = view.findViewById<EditText>(R.id.descriptionBox)
            chooseColorButton = view.findViewById(R.id.chooseColorButton)
            chooseColorButton.setBackgroundColor(chosenColor)
            chooseColorButton.setOnClickListener { onChooseColorButtonClicked(view) }
            if (lat != null)
                latitudeBox.setText(lat.toString())
            if (lng != null)
                longitudeBox.setText(lng.toString())
            builder.setView(view)
            val dialog = builder.create()
            dialog.setTitle("Add Waypoint")
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Add") { _, _ ->
                listener.addWaypoint(latitudeBox.text.toString().toDouble(), longitudeBox.text.toString().toDouble(), descriptionBox.text.toString(), chosenColor)
            }
            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel") { _, _ ->
                dialog.dismiss()
            }
            return dialog
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun onChooseColorButtonClicked(view: View) {
        MaterialColorPickerDialog
            .Builder(requireContext())
            .setTitle("Choose Color")
            .setColorShape(ColorShape.SQAURE)
            .setDefaultColor(chosenColor)
            .setColorListener { color, colorHex ->
                chosenColor = color
                chooseColorButton.setBackgroundColor(color)
            }
            .show()
    }
}