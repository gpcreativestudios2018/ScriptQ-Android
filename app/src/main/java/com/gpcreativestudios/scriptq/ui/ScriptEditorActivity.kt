package com.gpcreativestudios.scriptq.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.graphics.Typeface
import android.text.Html
import android.text.Spannable
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.gpcreativestudios.scriptq.data.AppDatabase
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import com.gpcreativestudios.scriptq.databinding.ActivityScriptEditorBinding
import com.gpcreativestudios.scriptq.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class ScriptEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScriptEditorBinding
    private val scriptViewModel: ScriptViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = ScriptRepository(database.scriptDao())
        ScriptViewModelFactory(repository)
    }
    private val billingViewModel: BillingViewModel by viewModels()
    private var scriptId: Int = 0
    private var createdAt: Long = 0
    private var isPremium = false

    private fun withPremiumAccess(featureName: String, source: String, action: () -> Unit) {
        if (isPremium) {
            action()
        } else {
            PremiumAccess.launchPaywall(this, source, featureName)
        }
    }

    private fun configureBannerAd() {
        if (BuildConfig.ADMOB_BANNER_ID.isBlank()) {
            binding.adView.visibility = View.GONE
            return
        }

        MobileAds.initialize(this) {}
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = java.lang.StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                    val importedText = stringBuilder.toString().trim()
                    
                    val currentText = binding.editTextContent.text.toString()
                    if (currentText.isEmpty()) {
                        binding.editTextContent.setText(importedText)
                    } else {
                        binding.editTextContent.setText("$currentText\n$importedText")
                    }
                    Toast.makeText(this, "Script imported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to import file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    val writer = OutputStreamWriter(outputStream)
                    val contentText = binding.editTextContent.text.toString()
                    writer.write(contentText)
                    writer.close()
                    Toast.makeText(this, "Script exported", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to export file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScriptEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check for existing script data for editing
        scriptId = intent.getIntExtra("SCRIPT_ID", 0)
        val existingTitle = intent.getStringExtra("SCRIPT_TITLE")
        val existingContent = intent.getStringExtra("SCRIPT_CONTENT")
        createdAt = intent.getLongExtra("SCRIPT_CREATED_AT", 0)

        if (scriptId != 0) {
            binding.editTextTitle.setText(existingTitle)
            if (!existingContent.isNullOrEmpty()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    binding.editTextContent.setText(Html.fromHtml(existingContent, Html.FROM_HTML_MODE_COMPACT))
                } else {
                    @Suppress("DEPRECATION")
                    binding.editTextContent.setText(Html.fromHtml(existingContent))
                }
            }
            title = "Edit Script"
        } else {
            title = "Add New Script"
        }

        // Formatting Logic
        fun applySpan(span: Any) {
            val start = binding.editTextContent.selectionStart
            val end = binding.editTextContent.selectionEnd
            if (start < end) {
                binding.editTextContent.text?.setSpan(span, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }

        binding.btnFormatBold.setOnClickListener {
            withPremiumAccess("Rich text formatting", "editor_format_bold") {
                applySpan(StyleSpan(Typeface.BOLD))
            }
        }
        binding.btnFormatItalic.setOnClickListener {
            withPremiumAccess("Rich text formatting", "editor_format_italic") {
                applySpan(StyleSpan(Typeface.ITALIC))
            }
        }
        binding.btnFormatUnderline.setOnClickListener {
            withPremiumAccess("Rich text formatting", "editor_format_underline") {
                applySpan(UnderlineSpan())
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            binding.btnFontSans.setOnClickListener {
                withPremiumAccess("Rich text formatting", "editor_font_sans") {
                    applySpan(TypefaceSpan(Typeface.create("sans-serif", Typeface.NORMAL)))
                }
            }
            binding.btnFontSerif.setOnClickListener {
                withPremiumAccess("Rich text formatting", "editor_font_serif") {
                    applySpan(TypefaceSpan(Typeface.create("serif", Typeface.NORMAL)))
                }
            }
            binding.btnFontMono.setOnClickListener {
                withPremiumAccess("Rich text formatting", "editor_font_mono") {
                    applySpan(TypefaceSpan(Typeface.create("monospace", Typeface.NORMAL)))
                }
            }
        } else {
            binding.btnFontSans.setOnClickListener {
                withPremiumAccess("Rich text formatting", "editor_font_sans_legacy") {
                    applySpan(TypefaceSpan("sans-serif"))
                }
            }
            binding.btnFontSerif.setOnClickListener {
                withPremiumAccess("Rich text formatting", "editor_font_serif_legacy") {
                    applySpan(TypefaceSpan("serif"))
                }
            }
            binding.btnFontMono.setOnClickListener {
                withPremiumAccess("Rich text formatting", "editor_font_mono_legacy") {
                    applySpan(TypefaceSpan("monospace"))
                }
            }
        }

        configureBannerAd()

        billingViewModel.isProActive.observe(this) { isPremium ->
            this.isPremium = isPremium
            PremiumAccess.cachePremiumStatus(this, isPremium)
            if (isPremium || BuildConfig.ADMOB_BANNER_ID.isBlank()) {
                binding.adView.visibility = View.GONE
            } else {
                binding.adView.visibility = View.VISIBLE
            }
        }

        binding.editTextContent.doOnTextChanged { text, _, _, _ ->
            val wordCount = text?.trim()?.split("\\s+".toRegex())?.filter { it.isNotEmpty() }?.size ?: 0
            val readTimeMinutes = Math.ceil(wordCount / 150.0).toInt()
            val readTimeStr = if (readTimeMinutes < 1) "< 1 min read" else "~$readTimeMinutes min read"
            binding.textStats.text = "$wordCount words • $readTimeStr"
            
            // Live Preview for Tablet layout
            val textPreview = findViewById<android.widget.TextView>(com.gpcreativestudios.scriptq.R.id.textPreview)
            textPreview?.text = binding.editTextContent.text
        }
        
        // Trigger it once initially to update stats for existing scripts
        val initialText = binding.editTextContent.text.toString()
        val initialCount = initialText.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        val initialMins = Math.ceil(initialCount / 150.0).toInt()
        val initialStr = if (initialMins < 1) "< 1 min read" else "~$initialMins min read"
        binding.textStats.text = "$initialCount words • $initialStr"
        
        // Live Preview for Tablet layout
        val textPreview = findViewById<android.widget.TextView>(com.gpcreativestudios.scriptq.R.id.textPreview)
        textPreview?.text = binding.editTextContent.text

        binding.buttonPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip() && clipboard.primaryClip?.itemCount ?: 0 > 0) {
                val pasteText = clipboard.primaryClip?.getItemAt(0)?.text
                if (!pasteText.isNullOrEmpty()) {
                    val currentText = binding.editTextContent.text.toString()
                    // Append pasted text or replace if empty
                    if (currentText.isEmpty()) {
                        binding.editTextContent.setText(pasteText)
                    } else {
                        binding.editTextContent.setText("$currentText\n$pasteText")
                    }
                    Toast.makeText(this, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonImport.setOnClickListener {
            withPremiumAccess("Script import", "editor_import") {
                importLauncher.launch("text/plain")
            }
        }

        binding.buttonExport.setOnClickListener {
            withPremiumAccess("Script export", "editor_export") {
                val titleText = binding.editTextTitle.text.toString()
                val fileName = if (titleText.isNotEmpty()) "$titleText.txt" else "script.txt"
                exportLauncher.launch(fileName)
            }
        }

        binding.buttonShare.setOnClickListener {
            val titleText = binding.editTextTitle.text.toString()
            val contentText = binding.editTextContent.text.toString()

            if (titleText.isNotEmpty() || contentText.isNotEmpty()) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TITLE, titleText)
                    putExtra(Intent.EXTRA_TEXT, "$titleText\n\n$contentText")
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Share Script via"))
            } else {
                Toast.makeText(this, "Nothing to share", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonSave.setOnClickListener {
            val titleText = binding.editTextTitle.text.toString()
            
            val contentHtml = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                Html.toHtml(binding.editTextContent.text, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
            } else {
                @Suppress("DEPRECATION")
                Html.toHtml(binding.editTextContent.text)
            }

            if (titleText.isNotEmpty() && contentHtml.isNotEmpty()) {
                val script = if (scriptId != 0) {
                    Script(
                        id = scriptId, 
                        title = titleText, 
                        textContent = contentHtml,
                        createdAt = createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    Script(title = titleText, textContent = contentHtml)
                }
                scriptViewModel.insert(script)
                Toast.makeText(this, "Script saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter title and content", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
