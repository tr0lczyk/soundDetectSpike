package com.example.spikeaudiodetect

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodec.BufferInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer

class AudioFromVideo(
  private val context:Context,
  private val video: Uri,
  private val audio: String
) {
  private var amc: MediaCodec? = null
  private val ame: MediaExtractor
  private var amf: MediaFormat? = null
  private var amime: String? = null
  fun init() {
    try {
      ame.setDataSource(context,video,null)
      amf = ame.getTrackFormat(1)
      ame.selectTrack(1)
      amime = amf!!.getString(MediaFormat.KEY_MIME)
      amc = MediaCodec.createDecoderByType(amime!!)
      amc!!.configure(amf, null, null, 0)
      amc!!.start()
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  fun start() {
    AudioService(amc, ame, audio).start()
  }

  private inner class AudioService internal constructor(
    private val amc: MediaCodec?,
    private val ame: MediaExtractor,
    private val destFile: String
  ) :
    Thread() {
    private val aInputBuffers: Array<ByteBuffer>
    private var aOutputBuffers: Array<ByteBuffer>
    override fun run() {
      try {
        val os: OutputStream = FileOutputStream(File(destFile))
        var count: Long = 0
        while (true) {
          val inputIndex = amc!!.dequeueInputBuffer(0)
          if (inputIndex == -1) {
            continue
          }
          val sampleSize = ame.readSampleData(aInputBuffers[inputIndex], 0)
          if (sampleSize == -1) break
          val presentationTime = ame.sampleTime
          val flag = ame.sampleFlags
          ame.advance()
          amc.queueInputBuffer(inputIndex, 0, sampleSize, presentationTime, flag)
          val info = BufferInfo()
          val outputIndex = amc.dequeueOutputBuffer(info, 0)
          if (outputIndex >= 0) {
            val data = ByteArray(info.size)
            aOutputBuffers[outputIndex].get(data, 0, data.size)
            aOutputBuffers[outputIndex].clear()
            os.write(data)
            count += data.size.toLong()
            Log.i("write", "" + count)
            amc.releaseOutputBuffer(outputIndex, false)
          } else if (outputIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            aOutputBuffers = amc.outputBuffers
          } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
          }
        }
        os.flush()
        os.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

    init {
      aInputBuffers = amc!!.inputBuffers
      aOutputBuffers = amc.outputBuffers
    }
  }

  init {
    ame = MediaExtractor()
    init()
  }
}