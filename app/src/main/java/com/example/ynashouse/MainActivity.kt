package com.example.ynashouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ynashouse.house.HouseScreen
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.theme.YnasHouseTheme
import com.example.ynashouse.widget.YnaWidget

/*
 * MAINACTIVITY.KT
 * Pintu masuk app: ambil data Yna dari phone, kemudian buka rumah.
 */
class MainActivity : ComponentActivity() {

    private lateinit var yna: YnaHouseState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        yna = YnaHouseState(getSharedPreferences("yna_house", MODE_PRIVATE))
        yna.load()
        yna.onAppOpen()

        setContent {
            YnasHouseTheme {
                HouseScreen(state = yna)
            }
        }
    }

    // simpan bila app ditutup / ke belakang
    override fun onPause() {
        super.onPause()
        yna.save()
        YnaWidget.refresh(this)      // widget skrin utama ikut data terbaru
    }
}
