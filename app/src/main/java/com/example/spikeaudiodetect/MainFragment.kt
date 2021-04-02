package com.example.spikeaudiodetect

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioRecord
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioEncoder
import android.media.MediaRecorder.AudioSource
import android.media.MediaRecorder.OutputFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
          val localData = data.data
          displayMediaData(localData)
          // verifyVideoForAudio(localData)
          // convertVideoToAudio(localData)
          convertVideoToMp3(localData)
        }
      }
    }
  }

  private fun displayMediaData(uri: Uri?) {
    val mediaPlayer = MediaPlayer().apply {
      setAudioAttributes(
        AudioAttributes.Builder()
          .build()
      )
      setDataSource(requireContext(), uri!!)
      prepare()
      start()
    }
    val info = mediaPlayer.trackInfo
    var output = getString(R.string.soundless)
    for (i in info) {
      if (i.trackType == MEDIA_TRACK_TYPE_AUDIO) {
        output = getString(R.string.audioExists)
      }
    }
    binding.videoData.text = output
  }

  private fun verifyVideoForAudio(uri: Uri?){
    MediaMetadataRetriever().run{
      setDataSource(requireContext(), uri!!)
      val hasAudioStr = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
      if (hasAudioStr != null) {
        binding.videoData.text = "${getString(R.string.audioExists)}\n ${verifyVideoDuration(this)}"
      } else {
        binding.videoData.text = "${getString(R.string.soundless)}\n ${verifyVideoDuration(this)} s"
      }
      verifyVideoData(this)
      release()
    }
  }

  private fun verifyVideoDuration(retriever: MediaMetadataRetriever) :Int {
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val timeInMs = time?.toLong() ?: 0
    return (timeInMs / 1000).toInt()
  }

  private fun verifyVideoData(retriever: MediaMetadataRetriever){
    try {
      for (i in 0..999) {
        if (retriever.extractMetadata(i) != null) {
          Log.i("TAG", "Metadata $i:: " + retriever.extractMetadata(i))
        }
      }
    } catch (e: Exception) {
      Log.e("TAG", "Exception : " + e.message)
    }
  }

  private fun convertVideoToAudio(uri: Uri?){
    var path = uri!!.path
    val name = path!!.split("/")
    val newPath = path.replace(name[name.size - 1], "audio.pcm")
    AudioFromVideo(
      requireContext(), uri, "/sdcard/Download/audio2.pcm"
    ).start()
  }

  private fun convertVideoToMp3(uri: Uri?){
    var path = uri!!.path
    val name = path!!.split("/")
    val newPath = path.replace(name[name.size - 1], "audio.pcm")
    AudioExtractor().genVideoUsingMuxer(
      requireContext(), uri, "/sdcard/Download/audio.mp3", -1, -1, true, false
    )
  }
}