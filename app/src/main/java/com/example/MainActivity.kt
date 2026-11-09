package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.hinamart.data.local.AppDatabase
import com.example.hinamart.data.repository.HinaMartRepository
import com.example.hinamart.ui.HinaMartApp
import com.example.hinamart.ui.viewmodel.HinaMartViewModel
import com.example.hinamart.ui.viewmodel.HinaMartViewModelFactory
import com.example.hinamart.utils.NetworkObserver
import com.example.ui.theme.HinaMartTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: HinaMartViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = HinaMartRepository(database, applicationContext)
        val networkObserver = NetworkObserver(applicationContext)
        val factory = HinaMartViewModelFactory(repository, networkObserver)
        viewModel = ViewModelProvider(this, factory)[HinaMartViewModel::class.java]

        setContent {
            HinaMartTheme {
                HinaMartApp(viewModel = viewModel)
            }
        }
    }
}
