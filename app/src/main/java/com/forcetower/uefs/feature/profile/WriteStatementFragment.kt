/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2019.  João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.forcetower.uefs.feature.profile

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.forcetower.uefs.R
import com.forcetower.uefs.core.injection.Injectable
import com.forcetower.uefs.core.vm.EventObserver
import com.forcetower.uefs.core.vm.UViewModelFactory
import com.forcetower.uefs.databinding.FragmentWriteStatementBinding
import com.forcetower.uefs.feature.profile.ProfileActivity.Companion.EXTRA_STUDENT_ID
import com.forcetower.uefs.feature.shared.UFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import javax.inject.Inject

class WriteStatementFragment : UFragment(), Injectable {
    @Inject
    lateinit var factory: UViewModelFactory
    @Inject
    lateinit var preferences: SharedPreferences

    private lateinit var binding: FragmentWriteStatementBinding
    private val viewModel: ProfileViewModel by viewModels { factory }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        viewModel.setProfileId(requireNotNull(arguments).getLong(EXTRA_STUDENT_ID, 0))
        return FragmentWriteStatementBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateLabelTypeOnCheck(binding.typeSwitch.isChecked)
        binding.typeSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateLabelTypeOnCheck(isChecked)
        }

        binding.publish.setOnClickListener { onPublishStatement() }
        viewModel.sendingStatement.observe(this, Observer { binding.sending = it })
        viewModel.messages.observe(this, EventObserver { showSnack(it) })
        viewModel.statementSentSignal.observe(this, EventObserver { fragmentManager?.popBackStack() })
        binding.up.setOnClickListener { fragmentManager?.popBackStack() }
        viewModel.getMeProfile().observe(this, Observer {
            binding.student = it.data
        })
    }

    private fun updateLabelTypeOnCheck(checked: Boolean) {
        val text = if (checked) {
            getString(R.string.write_statement_type_anonymous)
        } else {
            getString(R.string.write_statement_type_public)
        }
        binding.postVisibility.text = text
    }

    private fun onPublishStatement() {
        binding.postContent.error = null
        val accepted = preferences.getBoolean("write_statement_user_has_accepted", false)
        if (accepted) {
            onContinuePublishStatement()
        } else {
            onShowWarningUserAgreement()
        }
    }

    private fun onContinuePublishStatement() {
        val statement = binding.postContent.text?.toString() ?: return
        val hidden = binding.typeSwitch.isChecked
        if (statement.length < 10) {
            binding.postContent.error = getString(R.string.write_statement_must_be_at_least_15_characters)
            return
        }

        val profileId = requireNotNull(arguments).getLong(EXTRA_STUDENT_ID, 0)
        viewModel.onSendStatement(statement, profileId, hidden)
    }

    private fun onShowWarningUserAgreement() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.write_statement_agreement_title)
            .setMessage(R.string.write_statement_agreement_message)
            .setCancelable(true)
            .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .setPositiveButton(R.string.write_statement_accept) { dialog, _ ->
                dialog.dismiss()
                preferences.edit().putBoolean("write_statement_user_has_accepted", true).apply()
                onContinuePublishStatement()
            }
            .create()
            .show()
    }
}