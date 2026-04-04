package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gpcreativestudios.scriptq.data.AppDatabase
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import com.gpcreativestudios.scriptq.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scriptViewModel: ScriptViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = ScriptRepository(database.scriptDao())
        ScriptViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = ScriptAdapter()
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(this)

        scriptViewModel.allScripts.observe(this) { scripts ->
            scripts?.let { adapter.submitList(it) }
        }

        binding.fab.setOnClickListener {
            // Sample script for demonstration
            val newScript = Script(
                title = "New Script ${System.currentTimeMillis() % 1000}",
                textContent = "This is a sample script content generated at ${System.currentTimeMillis()}."
            )
            scriptViewModel.insert(newScript)
        }
    }
}
