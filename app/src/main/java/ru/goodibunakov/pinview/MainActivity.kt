package ru.goodibunakov.pinview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), PinView.PinEnterListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pinView.listener = this
        buttonSendCode.setOnClickListener {
            Toast.makeText(
                this,
                "Нажата кнопка",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun pinEnterFinished(pass: String) {
        buttonSendCode.isEnabled = true
    }

    override fun pinNotEntered() {
        buttonSendCode.isEnabled = false
    }
}