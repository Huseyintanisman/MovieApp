package com.example.kotlinartbooka
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.kotlinartbooka.databinding.ActivityArtActivtyBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.IOException

class DetailsActivity : AppCompatActivity() {

    var selectedBitmap : Bitmap? = null
    private lateinit var binding: ActivityArtActivtyBinding
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtActivtyBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)


        registerLauncher()

        val intent = intent

        val info = intent.getStringExtra("info")

        if (info.equals("new")) {
            binding.artText.setText("")
            binding.artistText.setText("")
            binding.yearText.setText("")
            binding.button.visibility = View.VISIBLE
            binding.imageView.isClickable = true
            binding.home.visibility = View.INVISIBLE


            val selectedImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.select)
            binding.imageView.setImageBitmap(selectedImageBackground)

        } else {
            binding.button.visibility = View.INVISIBLE
            binding.imageView.isClickable = false
            binding.home.visibility = View.VISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artName")
            val artistNameIx = cursor.getColumnIndex("artistName")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                binding.artText.setText(cursor.getString(artNameIx))
                binding.artistText.setText(cursor.getString(artistNameIx))
                binding.yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)

            }

            cursor.close()

        }


    }

    fun backToHome(view: View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }


    fun save(view: View) {

        val artName = binding.artText.text.toString()
        val artistName = binding.artistText.text.toString()
        val year = binding.yearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!, 300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG, 80, outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artName VARCHAR, artistName VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts (artName, artistName, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)

                statement.execute()

                Toast.makeText(this, "Kayit başarılı!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()

                Toast.makeText(this, "Kayıt başarısız!", Toast.LENGTH_SHORT).show()
            }

            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }


    fun makeSmallerBitmap(image: Bitmap, maximumSize : Int) : Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1) {
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

    fun selectImage(view: View) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                        View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        }).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        } else {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar.make(view, "Permission needed for gallery", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",
                        View.OnClickListener {
                            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }).show()
                } else {
                    permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            } else {
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }
        }


    }

    private fun registerLauncher() {
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intentFromResult != null) {
                    val imageData = intentFromResult.data
                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(this@DetailsActivity.contentResolver, imageData!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        } else {
                            selectedBitmap = MediaStore.Images.Media.getBitmap(this@DetailsActivity.contentResolver, imageData)
                            binding.imageView.setImageBitmap(selectedBitmap)
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
            if (result) {
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            } else {
                //permission denied
                Toast.makeText(this@DetailsActivity, "Permission needed!", Toast.LENGTH_LONG).show()
            }
        }
    }

}