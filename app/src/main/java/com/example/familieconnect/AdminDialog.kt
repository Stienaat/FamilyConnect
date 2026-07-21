package com.example.familieconnect

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.method.ScrollingMovementMethod
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

object AdminDialog {

    fun show(context: AdminDialogContext) {
        val activity = context.activity

        val prefs =
            activity.getSharedPreferences(
                "tracker",
                Context.MODE_PRIVATE
            )

        var language =
            prefs.getString("language", "nl") ?: "nl"

        val pinInput = EditText(activity).apply {
            hint = "Beheer PIN"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }

        val codeInputAdmin = EditText(activity).apply {
            hint =
                if (language == "en") {
                    "Family code"
                } else {
                    "Gezinscode"
                }

            setText(
                prefs.getString(
                    "group_code",
                    context.defaultGroupCode
                )
            )

            visibility = View.GONE
        }

        val mapLabel = TextView(activity).apply {
            text =
                if (language == "en") {
                    "Map"
                } else {
                    "Kaart"
                }

            textSize = 14f
            setPadding(0, 20, 0, 6)
            visibility = View.GONE
        }

        val mapSpinner = Spinner(activity).apply {
            adapter = ArrayAdapter(
                activity,
                android.R.layout.simple_spinner_dropdown_item,
                MapSources.PROVIDERS.map { it.label }
            )

            val savedProvider =
                MapSources.getSelectedKey(activity)

            val selectedIndex =
                MapSources.PROVIDERS.indexOfFirst {
                    it.key == savedProvider
                }.coerceAtLeast(0)

            setSelection(selectedIndex)
            visibility = View.GONE
        }

        val adminStatus = TextView(activity).apply {
            text = context.statusView.text
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(Color.BLUE)
            setPadding(0, 20, 0, 20)
        }

        context.setAdminStatusView(adminStatus)

        val readmeText = TextView(activity).apply {
            textSize = 13f
            setPadding(0, 20, 0, 0)
            movementMethod = ScrollingMovementMethod()
        }

        AdminDialogTexts.refresh(
            language,
            readmeText,
            codeInputAdmin,
            mapLabel
        )

        val btnNL = ImageButton(activity).apply {
            setImageResource(R.drawable.flag_nl)
            background = null
            setPadding(8, 8, 8, 8)

            setOnClickListener {
                language = "nl"

                prefs.edit()
                    .putString("language", language)
                    .apply()

                AdminDialogTexts.refresh(
                    language,
                    readmeText,
                    codeInputAdmin,
                    mapLabel
                )
            }
        }

        val btnEN = ImageButton(activity).apply {
            setImageResource(R.drawable.flag_en)
            background = null
            setPadding(8, 8, 8, 8)

            setOnClickListener {
                language = "en"

                prefs.edit()
                    .putString("language", language)
                    .apply()

                AdminDialogTexts.refresh(
                    language,
                    readmeText,
                    codeInputAdmin,
                    mapLabel
                )
            }
        }

        val langRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END

            addView(
                btnNL,
                LinearLayout.LayoutParams(90, 90).apply {
                    setMargins(0, 0, 30, 0)
                }
            )

            addView(
                btnEN,
                LinearLayout.LayoutParams(90, 90)
            )
        }

        val scroll = ScrollView(activity).apply {
            addView(readmeText)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                500
            )
        }

        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 25, 40, 10)

            addView(langRow)
            addView(pinInput)
            addView(codeInputAdmin)
            addView(mapLabel)
            addView(mapSpinner)
            addView(adminStatus)
            addView(scroll)
        }

        val dialog =
            AlertDialog.Builder(activity)
                .setTitle(
                    if (language == "en") {
                        "Admin"
                    } else {
                        "Beheer"
                    }
                )
                .setView(box)
                .setPositiveButton("✖️", null)
                .setNegativeButton(
                    if (language == "en") {
                        "Cancel"
                    } else {
                        "❌"
                    },
                    null
                )
                .create()

        dialog.setOnShowListener {
            val okButton =
                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                )

            okButton.setOnClickListener {
                val enteredPin =
                    pinInput.text.toString().trim()

                if (codeInputAdmin.visibility == View.GONE) {
                    if (enteredPin == context.adminPin) {
                        pinInput.visibility = View.GONE
                        codeInputAdmin.visibility = View.VISIBLE
                        mapLabel.visibility = View.VISIBLE
                        mapSpinner.visibility = View.VISIBLE

                        okButton.text =
                            if (language == "en") {
                                "Save"
                            } else {
                                "Opslaan"
                            }

                        dialog.setTitle(
                            if (language == "en") {
                                "Family code"
                            } else {
                                "Gezinscode"
                            }
                        )
                    } else {
                        pinInput.error =
                            if (language == "en") {
                                "Wrong PIN"
                            } else {
                                "Foute PIN"
                            }
                    }
                } else {
                    val newCode =
                        codeInputAdmin.text
                            .toString()
                            .trim()

                    val selectedProvider =
                        MapSources.PROVIDERS[
                            mapSpinner.selectedItemPosition
                        ].key

                    prefs.edit()
                        .putString("group_code", newCode)
                        .putString(
                            "device_name",
                            context.nameInput.text
                                .toString()
                                .trim()
                        )
                        .putString(
                            MapSources.PREF_KEY,
                            selectedProvider
                        )
                        .commit()

                    context.map.setTileSource(
                        MapSources.get(activity)
                    )

                    context.attributionView.text =
                        MapSources.getAttribution(activity)

                    context.map.invalidate()

                    Toast.makeText(
                        activity,
                        if (language == "en") {
                            "Saved"
                        } else {
                            "Opgeslagen"
                        },
                        Toast.LENGTH_LONG
                    ).show()

                    context.restartTracking()

                    dialog.dismiss()
                }
            }
        }

        dialog.setOnDismissListener {
            context.setAdminStatusView(null)
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            R.drawable.dialog_bg
        )
    }
}