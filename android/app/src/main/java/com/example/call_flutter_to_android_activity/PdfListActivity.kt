package com.example.call_flutter_to_android_activity

import android.Manifest
import android.animation.Animator
import android.annotation.TargetApi
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.Filter
import android.widget.Filterable
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.call_flutter_to_android_activity.databinding.ActivityPdfListBinding
import com.example.call_flutter_to_android_activity.databinding.PdfListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class PdfListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfListBinding

    private var isSearchOpen = false

    private lateinit var fileDownload: File
        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private lateinit var fileDocuments: File
        //Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    private var selectedFile: File? = null

    private val pdfList: ArrayList<File> = ArrayList()
    val PICK_DOCUMENT = 404
    private var adapter: MyPdfAdapter? = null


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_pdf_list)

        //Environment.getExternalStorageState(Environment.DIRECTORY_DOWNLOADS)

        fileDownload = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!

        fileDocuments = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)!!

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
        val linearLayoutManager = LinearLayoutManager(this@PdfListActivity)
        adapter = MyPdfAdapter(this@PdfListActivity, pdfList)
        binding.rvMyPdf.layoutManager = linearLayoutManager
        binding.rvMyPdf.adapter = adapter
        onPickDocument()

        binding.llBrowseOtherFile.setOnClickListener {
            val intent = Intent()
            intent.type = "application/pdf"
            val path =
                Environment.getExternalStorageDirectory().toString() + "/" + "Documents" + "/"
            val uri = Uri.parse(path)
            intent.setDataAndType(uri, "application/pdf")
            intent.action = Intent.ACTION_OPEN_DOCUMENT
            pickFileActivityLauncher.launch(intent)
            //startActivityForResult(intent, PICK_DOCUMENT)
        }

        binding.ivSearch.setOnClickListener {
            openSearchView()
        }

        binding.closeSearchButton.setOnClickListener {
            closeSearch()
            hideKeyboard(binding.closeSearchButton)
        }

        binding.ivClearText.setOnClickListener {
            if ((binding.etSearch.text.toString() != ""))
                binding.etSearch.setText("")
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(query: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(query: CharSequence, start: Int, before: Int, count: Int) {
                adapter?.filter?.filter(query)

            }
        })
    }


    private fun hideKeyboard(view: View) {
        val inputMethodManager =
            this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun closeSearch() {
        isSearchOpen = false
        val circularConceal = ViewAnimationUtils.createCircularReveal(
            binding.searchOpenView,
            (binding.ivSearch.right + binding.ivSearch.left) / 2,
            (binding.ivSearch.top + binding.ivSearch.bottom) / 2,
            binding.searchOpenView.width.toFloat(), 0f
        )

        circularConceal.duration = 300
        circularConceal.start()
        circularConceal.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) = Unit
            override fun onAnimationCancel(animation: Animator?) = Unit
            override fun onAnimationStart(animation: Animator?) = Unit
            override fun onAnimationEnd(animation: Animator?) {
                binding.searchOpenView.visibility = View.INVISIBLE
                if (binding.etSearch.text.toString() != "")
                    binding.etSearch.setText("")
                circularConceal.removeAllListeners()
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun openSearchView() {
        isSearchOpen = true
        binding.searchOpenView.visibility = View.VISIBLE
        val circularReveal =
            ViewAnimationUtils.createCircularReveal(
                binding.searchOpenView,
                (binding.ivSearch.right + binding.ivSearch.left) / 2,
                (binding.ivSearch.top + binding.ivSearch.bottom) / 2,
                0f, binding.searchOpenView.width.toFloat()
            )

        circularReveal.duration = 300
        circularReveal.start()
        val inputMethodManager =
            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInputFromWindow(
            binding.etSearch.applicationWindowToken,
            InputMethodManager.SHOW_FORCED, 0
        )
        binding.etSearch.requestFocus()
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private val pickFileActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            try {

                Uri.parse(result.data.toString())?.also { documentUri ->
                    this.contentResolver?.takePersistableUriPermission(
                        documentUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    val file = DocumentUtils.getFile(this, documentUri)//use pdf as file

                    val uri = FileProvider.getUriForFile(
                        this,
                        "$packageName.provider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = uri
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(intent)
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

    object DocumentUtils {
        fun getFile(mContext: Context?, documentUri: Uri): File {
            val inputStream = mContext?.contentResolver?.openInputStream(documentUri)
            var file = File("")
            inputStream.use { input ->
                file =
                    File(mContext?.cacheDir, System.currentTimeMillis().toString() + ".pdf")
                FileOutputStream(file).use { output ->
                    val buffer =
                        ByteArray(4 * 1024) // or other buffer size
                    var read: Int = -1
                    while (input?.read(buffer).also {
                            if (it != null) {
                                read = it
                            }
                        } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            return file
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun onPickDocument() {
        if (hasPermissions(storagePerm)) {
            pdfList.clear()
            getAllPdfFile()
            /*if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    true
                }
            ) {
                getAllPdfFile()
            } else {
                //request for the permission only above of 10
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    filePermissionActivityLauncher.launch(intent)
                }
            }*/
        } else
            requestPermissionsSafely(storagePerm, PICK_DOCUMENT)
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private val filePermissionActivityLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            onPickDocument()
        }

    private fun getPdfList(dir: File): ArrayList<File> {
        try {
            val files = dir.listFiles()
            if (files != null)
                for (singleFile in files) {
                    when {
                        singleFile.isDirectory -> {
                            getPdfList(singleFile)
                        }
                        singleFile.name.endsWith(".pdf", true) -> {
                            Log.e("singleFile", singleFile.absolutePath)
                            Log.e("name", singleFile.name)
                            Log.e("name", singleFile.length().toString())
                            pdfList.add(singleFile)
                        }
                    }
                }
            Log.e("dir", dir.absolutePath)


        } catch (e: Exception) {
            e.printStackTrace()
        }

        return pdfList
    }

    /*** permission ***/

    private var storagePerm: Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun getAllPdfFile() {
        Log.e("fileDocuments", fileDocuments.absolutePath)
        try {
            //binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val listOfDir = ArrayList<File>()
                    listOfDir.add(fileDownload)
                    listOfDir.add(fileDocuments)
                    for (i in 0 until listOfDir.size) {
                        getPdfList(listOfDir[i])
                    }
                    //val pdfList: ArrayList<File> = getPdfList(fileDownload)
                    //getSpecificDirFile()
                    if (pdfList.size > 0) {
                        CoroutineScope(Dispatchers.Main).launch {
                            //binding.progressBar.visibility = View.GONE
                            pdfList.sortBy { file -> file.lastModified() }
                            pdfList.reverse()
                            adapter?.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //binding.progressBar.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * need for Android 6 real time permissions
     */
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PICK_DOCUMENT -> {
                if (isAllPermissionsGranted(grantResults)) {
                    getAllPdfFile()
                    /*if (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Environment.isExternalStorageManager()
                        } else {
                            true
                        }
                    ) {
                        getAllPdfFile()
                    } else {
                        //request for the permission only above of 10
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            filePermissionActivityLauncher.launch(intent)
                        }
                    }*/
                }
            }
        }
    }

    inner class MyPdfAdapter(
        private val context: Context,
        private val showFiles: ArrayList<File>
    ) :
        RecyclerView.Adapter<MyPdfAdapter.ViewHolder>(), Filterable {
        //var appointmentList:ArrayList<AppointmentDTO> = ArrayList()
        private var lastPosition = -1
        var filteredList: ArrayList<File> = ArrayList()

        init {
            filteredList = showFiles
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MyPdfAdapter.ViewHolder {

            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.pdf_list, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return filteredList.size
        }

        override fun onBindViewHolder(holder: MyPdfAdapter.ViewHolder, position: Int) {
            //val model = filteredList[position]
            holder.binding.apply {
                tvPdfName.text = filteredList[position].name

                llMain.setOnClickListener {

                   /* val intent = Intent(context, ImageZoomAndPdfViewActivity::class.java)
                    intent.putExtra("documentPath", showFiles[position].path)
                    intent.putExtra("isImage", false)
                    val options: ActivityOptionsCompat =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                            this@PdfListActivity,
                            tvPdfName,
                            "transition_test"
                        )
                    //context.startActivity(intent, options.toBundle())
                    pdfViewActivityLauncher.launch(intent, options)*/
                    selectedFile = filteredList[position]
                    /*val intent = Intent(Intent.ACTION_VIEW)
                    val uri = FileProvider.getUriForFile(
                        context,
                        "$packageName.provider",
                        showFiles[position]
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.startActivity(intent)*/
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
            var binding: PdfListBinding = DataBindingUtil.bind(itemView)!!
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val charSearch = constraint.toString()
                    filteredList = if (charSearch.isEmpty()) {
                        showFiles
                    } else {

                        val resultList = ArrayList<File>()
                        for (row in showFiles) {
                            try {
                                if (row.name.uppercase(Locale.getDefault())
                                        .contains(charSearch.uppercase(Locale.getDefault()))
                                ) {
                                    resultList.add(row)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        resultList
                    }
                    val filterResults = FilterResults()
                    filterResults.values = filteredList
                    return filterResults
                }

                @Suppress("UNCHECKED_CAST")
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    filteredList = results?.values as ArrayList<File>
                    notifyDataSetChanged()
                }

            }
        }

    }


    override fun onBackPressed() {
        if(isSearchOpen){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                closeSearch()
            }
        }else {
            super.onBackPressed()
        }
    }
}