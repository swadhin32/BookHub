package com.example.bookhub.activity

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.textclassifier.TextLinks
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.bookhub.R
import com.example.bookhub.database.BookDatabase
import com.example.bookhub.database.BookEntity
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.lang.Exception

class DescriptionActivity : AppCompatActivity() {

    lateinit var txtBookName: TextView
    lateinit var txtBookAuthor: TextView
    lateinit var txtBookPrice: TextView
    lateinit var txtBookRating: TextView
    lateinit var txtBookImage: ImageView
    lateinit var txtBookDesc: TextView
    lateinit var btnAddToFav: Button
    lateinit var toolbar: Toolbar
//    lateinit var progressBar: ProgressBar
//    lateinit var progressBarLayout: RelativeLayout

    var bookId: String? = "100"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_description)

        txtBookName = findViewById(R.id.nameOfTheBook)
        txtBookAuthor = findViewById(R.id.nameOfAuthor)
        txtBookPrice = findViewById(R.id.rupeesPrice)
        txtBookRating = findViewById(R.id.ratingPoint)
        txtBookImage = findViewById(R.id.imageView6)
        txtBookDesc = findViewById(R.id.mainDescription)
        btnAddToFav = findViewById(R.id.addToFavouritesBtn)
        toolbar = findViewById(R.id.toolbar2)
//        progressBar = findViewById(R.id.progressBar)
//        progressBarLayout = findViewById(R.id.progressBarLayout)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Book Details"

        if(intent != null){
            bookId = intent.getStringExtra("book_id")
        }else{
            finish()
            Toast.makeText(this@DescriptionActivity, "Some unexpected error occurred!", Toast.LENGTH_SHORT).show()
        }

        if(bookId == "100"){
            finish()
            Toast.makeText(this@DescriptionActivity, "Some ", Toast.LENGTH_SHORT).show()
        }

        val queue = Volley.newRequestQueue(this@DescriptionActivity)
        val url = "http://13.235.250.119/v1/book/get_book/"

        val jsonParams = JSONObject()
        jsonParams.put("book_id", bookId)

        val jsonRequest = object: JsonObjectRequest(Request.Method.POST, url, jsonParams, Response.Listener {
            try {

                val success = it.getBoolean("success")
                if(success){
                    val bookJsonObject = it.getJSONObject("book_data")
                    //progressBarLayout.visibility = View.GONE

                    val bookImageUrl = bookJsonObject.getString("image")
                    Picasso.get().load(bookJsonObject.getString("image")).error(R.drawable.ic_launcher_foreground).into(txtBookImage)
                    txtBookName.text = bookJsonObject.getString("name")
                    txtBookAuthor.text = bookJsonObject.getString("author")
                    txtBookPrice.text = bookJsonObject.getString("price")
                    txtBookDesc.text = bookJsonObject.getString("description")
                    txtBookRating.text = bookJsonObject.getString("rating")

                    val bookEntity = BookEntity(
                        bookId?.toInt() as Int,
                        txtBookName.text.toString(),
                        txtBookAuthor.text.toString(),
                        txtBookPrice.text.toString(),
                        txtBookRating.text.toString(),
                        txtBookDesc.text.toString(),
                        bookImageUrl
                    )

                    val checkFav = DBAsyncTask(applicationContext, bookEntity, 1).execute()
                    val isFav = checkFav.get()

                    if(isFav){
                        btnAddToFav.text = "Remove from Favourites"
                        val favColor = ContextCompat.getColor(applicationContext, R.color.teal_200)
                        btnAddToFav.setBackgroundColor(favColor)
                    }else{
                        btnAddToFav.text = "Add to Favourites"
                        val noFavColor = ContextCompat.getColor(applicationContext, R.color.design_default_color_primary)
                        btnAddToFav.setBackgroundColor(noFavColor)
                    }

                    btnAddToFav.setOnClickListener{
                        if(!DBAsyncTask(applicationContext, bookEntity, 1).execute().get()){

                            val async = DBAsyncTask(applicationContext, bookEntity, 2).execute()
                            val result = async.get()
                            if(result){
                                Toast.makeText(this@DescriptionActivity, "Book added to favourites", Toast.LENGTH_SHORT).show()

                                btnAddToFav.text = "Remove from favourites"
                                val favColor = ContextCompat.getColor(applicationContext, R.color.teal_200)
                                btnAddToFav.setBackgroundColor(favColor)
                            } else {
                                Toast.makeText(this@DescriptionActivity, "Some error occured!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val async = DBAsyncTask(applicationContext, bookEntity, 3).execute()
                            val result = async.get()

                            if(result){
                                Toast.makeText(this@DescriptionActivity,"Book removed from favourites",Toast.LENGTH_SHORT).show()

                                btnAddToFav.text = "Add to favourites"
                                val noFavColor = ContextCompat.getColor(applicationContext, R.color.design_default_color_primary)
                                btnAddToFav.setBackgroundColor(noFavColor)
                            } else {

                            }
                        }
                    }

                }else{
                    Toast.makeText(this@DescriptionActivity, "Some Error Occured:",Toast.LENGTH_SHORT).show()
                }

            }catch (e: Exception){
                Toast.makeText(this@DescriptionActivity, "Some Error Occured:",Toast.LENGTH_SHORT).show()
            }

        },Response.ErrorListener {
            Toast.makeText(this@DescriptionActivity, "Some Error Occured:",Toast.LENGTH_SHORT).show()

        } ){
            override fun getHeaders(): MutableMap<String, String> {
                val header = HashMap<String, String> ()
                header["Content-type"] = "application/json"
                header["token"] = "9bf534118365f1"


                return header

            }
        }

        queue.add(jsonRequest)
    }

    class DBAsyncTask(val context: Context, val bookEntity: BookEntity, val mode: Int) : AsyncTask<Void, Void, Boolean>() {

       /* Mode 1 -> Check DB if the book is favourite or not
        Mode 2 -> Save the book into DB as favourite
        Mode 3 -> Remove the favourite book */

        val db = Room.databaseBuilder(context, BookDatabase::class.java, "books-db").build()

        override fun doInBackground(vararg params: Void?): Boolean {

            when(mode) {
                1 -> {
                    //Check DB if the book is favourite or not
                    val book: BookEntity? = db.bookDao().getBookById(bookEntity.book_id.toString())
                    db.close()
                    return book!=null

                }

                2-> {

                    //Save the book into DB as favourite
                    db.bookDao().insertBook(bookEntity)
                    db.close()
                    return true

                }

                3-> {

                    db.bookDao().deleteBook(bookEntity)
                    db.close()
                    return true

                }

            }
            return false
        }
    }


}