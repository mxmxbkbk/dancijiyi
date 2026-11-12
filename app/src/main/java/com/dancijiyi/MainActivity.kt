package com.dancijiyi

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * 应用唯一入口。当前仅承载空白占位页，后续接入单词本与复习流程。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
