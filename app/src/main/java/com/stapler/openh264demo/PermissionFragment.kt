package com.stapler.openh264demo

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation

class PermissionFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!hasPermission(requireContext())) {
            requestPermissions(PERMISSION_REQUIRED,PERMISSION_REQUEST_CODE)
        } else {
            navigateToCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (PackageManager.PERMISSION_GRANTED == grantResults.firstOrNull()) {
                Toast.makeText(requireContext(), "permission request granted", Toast.LENGTH_SHORT).show()
                navigateToCamera()
            } else {
                Toast.makeText(requireContext(), "permission request denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermission(requireContext: Context) = PERMISSION_REQUIRED.all {
        ContextCompat.checkSelfPermission(requireContext,it) == PackageManager.PERMISSION_GRANTED
    }

    private fun navigateToCamera() {
        lifecycleScope.launchWhenStarted {
            Navigation.findNavController(requireActivity(),R.id.fragment_container).navigate(
                PermissionFragmentDirections.actionPermissionFragmentToCameraFragment())
        }
    }

    companion object {
        private val PERMISSION_REQUIRED = arrayOf(android.Manifest.permission.CAMERA)
        private val PERMISSION_REQUEST_CODE = 10
    }
}