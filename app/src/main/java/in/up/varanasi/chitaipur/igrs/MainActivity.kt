package in.up.varanasi.chitaipur.igrs

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var officerName: EditText
    private lateinit var igrsNo: EditText
    private lateinit var complainant: EditText
    private lateinit var complaint: EditText
    private lateinit var investigation: EditText
    private lateinit var oppositeStatement: CheckBox
    private lateinit var satisfied: CheckBox
    private lateinit var reportView: TextView

    private fun id(name: String): Int =
        resources.getIdentifier(name, "id", packageName)

    private val speech =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val text = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()

            if (!text.isNullOrBlank()) {
                val old = investigation.text.toString()
                investigation.setText("$old $text".trim())
                investigation.setSelection(investigation.text.length)
            }
        }

    private val permission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startSpeech()
            else Toast.makeText(this, "Microphone permission आवश्यक है", Toast.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layoutId = resources.getIdentifier("activity_main", "layout", packageName)
        setContentView(layoutId)

        officerName = findViewById(id("officerName"))
        igrsNo = findViewById(id("igrsNo"))
        complainant = findViewById(id("complainant"))
        complaint = findViewById(id("complaint"))
        investigation = findViewById(id("investigation"))
        oppositeStatement = findViewById(id("oppositeStatement"))
        satisfied = findViewById(id("satisfied"))
        reportView = findViewById(id("report"))

        val micButton: Button = findViewById(id("micButton"))
        val generateButton: Button = findViewById(id("generateButton"))
        val shareButton: Button = findViewById(id("shareButton"))

        val prefs = getSharedPreferences("igrs", MODE_PRIVATE)
        officerName.setText(prefs.getString("officer", ""))

        micButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startSpeech()
            } else {
                permission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        generateButton.setOnClickListener {
            val officer = officerName.text.toString().trim()
            val no = igrsNo.text.toString().trim()
            val complainantText = complainant.text.toString().trim()
            val complaintText = complaint.text.toString().trim()
            val investigationText = investigation.text.toString().trim()

            if (no.isBlank() || complainantText.isBlank() || investigationText.isBlank()) {
                Toast.makeText(
                    this,
                    "IGRS नं., शिकायतकर्ता और जांच विवरण भरें",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            prefs.edit().putString("officer", officer).apply()

            val opposite = if (oppositeStatement.isChecked) "हाँ" else "नहीं"
            val applicantSatisfied = if (satisfied.isChecked) "हाँ" else "नहीं"

            val generatedReport = """
IGRS प्रार्थना पत्र पर जांच रिपोर्ट

A. जांचकर्ता अधिकारी का विवरण
1. नाम: $officer
2. पद: उ0नि0
3. नियुक्ति स्थान: थाना चितईपुर, कमिश्नरेट वाराणसी

B. शिकायत का विवरण
5. IGRS संदर्भ नं0: $no
7. शिकायतकर्ता का नाम व पता: $complainantText
9. शिकायत का संक्षिप्त विवरण: $complaintText

C. जांच का विवरण एवं कृत कार्यवाही
16. जांच का विवरण:

महोदय, प्रकरण की जांच के क्रम में उपलब्ध प्रार्थना पत्र तथा जांच के दौरान प्राप्त तथ्यों का अवलोकन किया गया। $investigationText

14. विपक्षी का बयान हुआ अथवा नहीं: $opposite
20. आवेदक जांच से संतुष्ट है: $applicantSatisfied

24. जांचकर्ता का हस्ताक्षर व नाम:
उ0नि0 $officer
            """.trimIndent()

            reportView.text = generatedReport
        }

        shareButton.setOnClickListener {
            val text = reportView.text.toString()

            if (text.isBlank() || text == "आख्या यहाँ दिखाई देगी।") {
                Toast.makeText(this, "पहले जांच आख्या तैयार करें", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            startActivity(Intent.createChooser(intent, "आख्या Share करें"))
        }
    }

    private fun startSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "जांच की कार्यवाही बोलें")
        }

        try {
            speech.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input उपलब्ध नहीं", Toast.LENGTH_SHORT).show()
        }
    }
}
