package com.example.call_flutter_to_android_activity

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.call_flutter_to_android_activity.databinding.ActivityTestBinding
import com.example.call_flutter_to_android_activity.databinding.ImageListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding

    private val file: File = //Environment.getExternalStorageDirectory()
        //getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    private val imageList: ArrayList<File> = ArrayList()
    val PICK_IMAGE = 405
    private var adapter: MyImageAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_test)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.llBrowseOtherFile.setOnClickListener {

            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageActivityLauncher.launch(intent)
        }
        val linearLayoutManager = GridLayoutManager(this, 3)
        adapter = MyImageAdapter(this)
        Log.e("imageList", imageList.size.toString())
        binding.rvMyImages.layoutManager = linearLayoutManager
        binding.rvMyImages.adapter = adapter
        onPickImage()
    }

    private fun onPickImage() {
        if (hasPermissions(storagePerm)) {
            imageList.clear()
            getAllImageFile()
            /*if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }
            ) {
                getAllImageFile()
            } else {
                //request for the permission
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, PICK_IMAGE)
            }*/

        } else requestPermissionsSafely(storagePerm, PICK_IMAGE)
    }

    private fun getAllImageFile() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    getImageList(file)
                    val path = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    //getImageList(path))
                    if (imageList.size > 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            imageList.sortBy { file -> file.lastModified() }
                            imageList.reverse()
                            adapter?.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getImageList(dir: File): ArrayList<File> {

        val files = dir.listFiles()
        if (files != null) for (singleFile in files) {
            when {
                singleFile.isDirectory -> {
                    getImageList(singleFile)
                }
                singleFile.name.endsWith(".jpg", true) -> {
                    imageList.add(singleFile)
                }
                else -> Log.e("testing", singleFile.absolutePath)
            }
        }

        return imageList
    }

    private val pickImageActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            try {
                Log.e("gallery image ->", result.data!!.data.toString())
                val path = getPathFromURI(result.data!!.data)!!
                val intent = Intent()
                intent.putExtra("imagePath", path)
                setResult(Activity.RESULT_OK, intent)
                finish()
                Log.e("path",path)
                imageList.add(File(path))
                adapter?.notifyDataSetChanged()
                /* val data = result.data
            val intent = Intent()
            if (data != null) {
                intent.putExtra("imagePath", data.data.toString())
            }
            setResult(Activity.RESULT_OK, intent)
            finish()*/
            }catch (e: java.lang.Exception){
                e.printStackTrace()
            }

        }

    fun getPathFromURI(contentUri: Uri?): String? {
        var res: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(contentUri!!, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            val column_index: Int = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return res
    }

    /*** permission ***/

    private var storagePerm: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun requestPermissionsSafely(permissions: Array<String>, requestCode: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode)
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun hasPermissions(permissions: Array<String>): Boolean {
        var hasAllPermissions = true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            hasAllPermissions = true
        } else {
            for (perm in permissions) {
                if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                    hasAllPermissions = false
                    break
                }
            }
        }
        return hasAllPermissions
    }


    private fun isAllPermissionsGranted(grantResults: IntArray): Boolean {
        var perGranted = true
        for (perm in grantResults) {
            if (perm != PackageManager.PERMISSION_GRANTED) {
                perGranted = false
                break
            }
        }
        return perGranted
    }

    /**
     * need for Android 6 real time permissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PICK_IMAGE -> {
                if (isAllPermissionsGranted(grantResults)) {
                    getAllImageFile()
                   /* if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Environment.isExternalStorageManager()
                        } else {
                            true
                        }
                    ) {
                        getAllImageFile()
                    } else {
                        //request for the permission
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivityForResult(intent, PICK_IMAGE)
                    }*/
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE) {
            if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }
            ) {
                onPickImage()
            }
        }
    }

    inner class MyImageAdapter(
        private val context: Context
    ) : RecyclerView.Adapter<MyImageAdapter.ViewHolder>() {
        //var appointmentList:ArrayList<AppointmentDTO> = ArrayList()
        var lastPosition = -1


        override fun onCreateViewHolder(
            parent: ViewGroup, viewType: Int
        ): MyImageAdapter.ViewHolder {

            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.image_list, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return imageList.size
        }

        override fun onBindViewHolder(holder: MyImageAdapter.ViewHolder, position: Int) {
            //val model = filteredList[position]
            holder.binding.apply {
                Glide.with(context).load(imageList[position].path).into(ivImage)

                llMain.setOnClickListener {
                    val intent = Intent()
                    intent.putExtra("imagePath", imageList[position].path)
                    setResult(Activity.RESULT_OK, intent)
                    finish()
                    //openImageViewActivity(imageList[position], ivImage)
                }
            }

            //set Animation
            if (position > lastPosition) {
                val animation = AnimationUtils.loadAnimation(
                    context, android.R.anim.slide_in_left
                )
                holder.itemView.startAnimation(animation)
                lastPosition = position
            }


        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var binding: ImageListBinding = DataBindingUtil.bind(itemView)!!
        }

    }

    private val imageActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {

            }
        }
}