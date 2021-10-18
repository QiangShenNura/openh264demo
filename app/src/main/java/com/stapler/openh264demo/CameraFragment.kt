package com.stapler.openh264demo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.display.DisplayManager
import android.media.Image
import android.media.Image.Plane
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.window.WindowManager
import com.stapler.openh264demo.databinding.FragmentCameraBinding
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CameraFragment : Fragment() {
    private var pEncoder: Long = 0
    private lateinit var h264Encoder: H264Encoder
    private lateinit var cameraExecutor: ExecutorService
    private var imageAnalysis: ImageAnalysis? = null
    private var videoCapture: VideoCapture? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var windowManager: WindowManager
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT
    private lateinit var container: ConstraintLayout
    private var displayId: Int = -1
    private lateinit var binding: FragmentCameraBinding
    private val displayManager by lazy {
        requireContext().getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentCameraBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view as ConstraintLayout
        windowManager = WindowManager(view.context)
        // Every time the orientation of device changes, update rotation for use cases
        displayManager.registerDisplayListener(displayListener, null)
        cameraExecutor = Executors.newFixedThreadPool(5)
        h264Encoder = H264Encoder()

        binding.viewFinder.post {
            displayId = binding.viewFinder.display.displayId

            // Build UI controls
            updateCameraUi()

            // Set up the camera and its use cases
            setUpCamera()
            val outputFile =
                File(requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES),
                    "test.h264")
            pEncoder = h264Encoder.createEncoder(480, 960, outputFile.absolutePath)
        }
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraFragment.displayId) {
                Log.d(TAG, "Rotation changed: ${view.display.rotation}")
                imageAnalysis?.targetRotation = view.display.rotation
            }
        } ?: Unit

    }

    private fun updateCameraUi() {
        container.findViewById<ConstraintLayout>(R.id.camera_ui_container)?.let {
            container.removeView(it)
        }
        val controls = View.inflate(requireContext(), R.layout.camera_ui_container, container)
        controls.findViewById<ImageButton>(R.id.camera_switch_button).let {
            it.isEnabled = false;
            it.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                bindCameraUseCases()
            }
        }

        controls.findViewById<ImageButton>(R.id.camera_capture_button).setOnClickListener {
            startRecord()
        }

    }

    private fun startRecord() {


    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        var metrics = windowManager.getCurrentWindowMetrics().bounds
        Log.d(TAG, "Screen metrics: ${metrics.width()} x ${metrics.height()}")
        val screenAspectRatio = aspectRatio(metrics.width(), metrics.height())
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")
        val rotation = binding.viewFinder.display.rotation
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetResolution(Size(480, 960))
            .setTargetRotation(rotation)
            .build()

        imageAnalysis = ImageAnalysis.Builder()
//            .setTargetAspectRatio(screenAspectRatio)
            .setTargetResolution(Size(480, 960))
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, { imageProxy ->
                    if (pEncoder == 0L) {
                        pEncoder =
                            h264Encoder.createEncoder(imageProxy.width, imageProxy.height, "")
                    }

                    h264Encoder.encode(pEncoder,
                        getDataFromImage(imageProxy.image!!),
                        imageProxy.width,
                        imageProxy.height)

//                    h264Encoder.encode()

//                    val rotateDegree = imageProxy.imageInfo.rotationDegrees
////                    Log.d(TAG,"rotateDegree=$rotateDegree") 图片的旋转角度,竖屏时270度,横屏时为0度
//                    val bitmap = Bitmap.createBitmap(imageProxy.width,imageProxy.height,Bitmap.Config.ARGB_8888)
//                    YuvToRgbConverter(requireContext()).yuvToRgb(imageProxy.image!!,bitmap)
//                    binding.root.post {
//                        binding.imageView.setImageBitmap(bitmap)
//                    }
                    imageProxy.close()//调用后才会回调下一帧
                })
            }

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            lensFacing = when {
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }
            updateCameraSwitchButton()

            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun updateCameraSwitchButton() {
        val switchCamerasButton = container.findViewById<ImageButton>(R.id.camera_switch_button)
        try {
            switchCamerasButton.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (e: Exception) {
            switchCamerasButton.isEnabled = false
        }
    }

    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private fun getDataFromImage(image: Image): ByteArray? {
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        var rowStride: Int
        var pixelStride: Int
        var data: ByteArray? = null
        // Read image data
        val planes = image.planes
        // Check image validity
        if (!checkAndroidImageFormat(image)) {
            return null
        }
        var buffer: ByteBuffer
        var offset = 0
        data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        for (i in planes.indices) {
            val shift = if (i == 0) 0 else 1
            var plane: Plane? = null
            if (i == 0) {
                plane = planes[0]
            } else if (i == 1) {
                plane = planes[2]
            } else if (i == 2) {
                plane = planes[1]
            }
            buffer = plane!!.buffer
            rowStride = plane.rowStride
            pixelStride = plane.pixelStride
            //            GenseeLog.i(TAG, "plane="+i+"pixelStride=" + pixelStride+",rowStride=" + rowStride+",width=" + width+",height=" + height);
            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            val w = crop.width() shr shift
            val h = crop.height() shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                val bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8
                var length: Int
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    length = w * bytesPerPixel
                    buffer[data, offset, length]
                    offset += length
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    length = (w - 1) * pixelStride + bytesPerPixel
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[offset++] = rowData[col * pixelStride]
                    }
                }
                // Advance buffer the remainder of the row stride
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
            //            if (true) GenseeLog.i(TAG, "Finished reading data from plane " + i);
        }
        return data
    }

    private fun checkAndroidImageFormat(image: Image): Boolean {
        val format = image.format
        val planes = image.planes
        var isValid = false
        when (format) {
            ImageFormat.YUV_420_888, ImageFormat.NV21, ImageFormat.YV12 -> if (planes.size == 3) {
                isValid = true
            } else {
                Log.e(TAG, "YUV420 format Images should have 3 planes!")
            }
            else -> Log.e(TAG, "YUV420 format not support!")
        }
        return isValid
    }

    companion object {
        private val TAG = CameraFragment::class.java.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0

    }
}