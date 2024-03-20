package com.example.kotlinartbooka

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kotlinartbooka.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var artList : ArrayList<Art>
    private lateinit var artAdapter : ArtAdapter
    private lateinit var database : SQLiteDatabase


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)

        artList = ArrayList<Art>()
        artAdapter = ArtAdapter(artList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = artAdapter

        retrieveDataFromDatabase()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //Inflater
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.add_art,menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun retrieveDataFromDatabase() {
        try {
            val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE, null)
            val cursor = database.rawQuery("SELECT * FROM arts", null)
            val artNameIx = cursor.getColumnIndex("artName")
            val idIx = cursor.getColumnIndex("id")

            while (cursor.moveToNext()) {
                val name = cursor.getString(artNameIx)
                val id = cursor.getInt(idIx)
                val art = Art(name, id)
                artList.add(art)
            }

            artAdapter.notifyDataSetChanged()
            cursor.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add_art_item) {
            val intent = Intent(this, DetailsActivity::class.java)
            intent.putExtra("info", "new")
            startActivity(intent)
        } else if (item.itemId == R.id.delete_all_item) {
            try {
                database.execSQL("DELETE FROM arts;")
                Toast.makeText(this, "Values Deleted", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error Deleting Values", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}