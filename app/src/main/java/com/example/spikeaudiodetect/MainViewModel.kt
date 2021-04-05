package com.example.spikeaudiodetect

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import linc.com.amplituda.Amplituda

class MainViewModel(application: Application) : AndroidViewModel(application) {

  private val job = Job()
  val viewModelScope = CoroutineScope(Dispatchers.Main + job)
  private val _progressVisible: MutableLiveData<Boolean> = MutableLiveData()
  val progressVisible: LiveData<Boolean> get() = _progressVisible
  private val _isAudioTrackAvailable: MutableLiveData<Boolean> = MutableLiveData()
  val isAudioTrackAvailable: LiveData<Boolean> get() = _isAudioTrackAvailable
  private val _videoDuration: MutableLiveData<String> = MutableLiveData("")
  val videoDuration: MutableLiveData<String> get() = _videoDuration
  private val _isThereAnySound: MutableLiveData<String> = MutableLiveData("")
  val isThereAnySound: MutableLiveData<String> get() = _isThereAnySound

  fun verifyVideoForAudio(uri: Uri?) {
    MediaMetadataRetriever().run {
      setDataSource(getApplication(), uri!!)
      _isAudioTrackAvailable.value = extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
      verifyDuration(this)
      release()
    }
  }

  private fun verifyDuration(retriever: MediaMetadataRetriever){
    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    val timeInMs = time?.toLong() ?: 0
    _videoDuration.value = (timeInMs / 1000).toInt().toString()
  }

  fun convertVideoToMp3(uri: Uri?) {
    _progressVisible.postValue(true)
    viewModelScope.launch {
      AudioExtractor().genVideoUsingMuxer(
        getApplication(), uri, "/sdcard/Download/audio.mp3", -1, -1, true, false
      )
    }.invokeOnCompletion {
      getAmplituda()
    }
  }

  private fun getAmplituda() {
    val amplituda = Amplituda(getApplication())
    amplituda.fromPath("/sdcard/Download/audio.mp3")
      .amplitudesAsList {
       for (i in it){
         if(i > 0){
           _isThereAnySound.postValue("And there is sound!")
           break
         } else {
           _isThereAnySound.postValue("And there is no sound :(")
         }
       }
      }
    _progressVisible.postValue(false)
  }

  override fun onCleared() {
    super.onCleared()
    job.complete()
  }
}