package cn.dong.charcount

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val INPUT_COUNT_MAX = 10
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edit.filters = arrayOf(CharCountFilter(INPUT_COUNT_MAX))
        edit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                log("beforeTextChanged: s=$s")
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                log("onTextChanged: s=$s")
            }

            override fun afterTextChanged(s: Editable) {
                log("afterTextChanged: s=$s")
                editCounter.text = "${s.charCountCeil()} / $INPUT_COUNT_MAX"
            }
        })
    }

    private fun log(message: String) {
        Log.d("main", message)
    }
}
