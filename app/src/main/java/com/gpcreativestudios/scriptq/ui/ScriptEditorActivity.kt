package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gpcreativestudios.scriptq.data.AppDatabase
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import com.gpcreativestudios.scriptq.databinding.ActivityScriptEditorBinding

class ScriptEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScriptEditorBinding
    private val scriptViewModel: ScriptViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = ScriptRepository(database.scriptDao())
        ScriptViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScriptEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSave.setOnClickListener {
            val title = binding.editTextTitle.text.toString()
            val content = binding.editTextContent.text.toString()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                val script = Script(title = title, textContent = content)
                scriptViewModel.insert(script)
                Toast.makeText(this, "Script saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter title and content", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
