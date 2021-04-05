package com.example.spikeaudiodetect

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.spikeaudiodetect.databinding.FragmentMainBinding

class MainFragment : Fragment() {

  lateinit var binding: FragmentMainBinding
  private val viewModel: MainViewModel by viewModels()
  private val ACTIVITY_CHOOSE_FILE_CODE = 6666
  private val PERMISSION_STORAGE =
    arrayOf(
      permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE,
      permission.MANAGE_EXTERNAL_STORAGE, permission.CAMERA
    )
  private val PERMISSION_STORAGE_CODE = 1111
  private var currentUri: Uri? = Uri.parse("")

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentMainBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel
    return binding.root
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    binding.chooseVideoButton.setOnClickListener {
      if (hasPermissions(PERMISSION_STORAGE)) {
        chooseFile()
      } else {
        checkPermissions()
      }
    }
    viewModel.isAudioTrackAvailable.observe(viewLifecycleOwner) {
      if (it) {
        viewModel.convertVideoToMp3(currentUri)
      }
    }
  }

  private fun chooseFile() {
    val chooseFile = Intent(Intent.ACTION_GET_CONTENT)
    chooseFile.addCategory(Intent.CATEGORY_OPENABLE)
    chooseFile.type = "*/*";
    val intent = Intent.createChooser(chooseFile, "Choose a file")
    startActivityForResult(intent, ACTIVITY_CHOOSE_FILE_CODE)
  }

  private fun checkPermissions() {
    requestPermissions(PERMISSION_STORAGE, PERMISSION_STORAGE_CODE)
  }

  private fun hasPermissions(permissions: Array<String>): Boolean {
    for (permission in permissions) {
      if (ActivityCompat.checkSelfPermission(requireActivity(), permission)
        != PackageManager.PERMISSION_GRANTED
      ) {
        return false
      }
    }
    return true
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == PERMISSION_STORAGE_CODE) {
      chooseFile()
    } else {
      Toast.makeText(requireContext(), "Permission not granted", Toast.LENGTH_SHORT).show()
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == ACTIVITY_CHOOSE_FILE_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        if (data != null) {
          currentUri = data.data!!
          viewModel.verifyVideoForAudio(currentUri)
        }
      }
    }
  }
}