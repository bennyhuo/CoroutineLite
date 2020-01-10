package com.bennyhuo.kotlin.coroutines.sample

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.JLabel

class MainWindow: JFrame(){

    lateinit var button: JButton
    private lateinit var image: JLabel

    fun init(){
        button = JButton("Click me to show Logo")
        image = JLabel()
        image.size = Dimension(540, 258)

        contentPane.add(button, BorderLayout.NORTH)
        contentPane.add(image, BorderLayout.CENTER)
    }

    fun onButtonClick(listener: (ActionEvent)->Unit){
        button.addActionListener(listener)
    }

    fun setLogo(logoData: ByteArray){
        image.icon = ImageIcon(logoData)
    }

}

fun main() {
    MainWindow().apply {
        title = "Coroutines"
        setSize(600, 300)
        isResizable = true
        defaultCloseOperation = EXIT_ON_CLOSE
        init()
        isVisible = true

        onButtonClick{
            println("Hello")
        }
    }
}


interface Service{
    @GET
    fun getLogo(@Url fileUrl: String): retrofit2.Call<ResponseBody>
}

object LogoService {
    val service = Retrofit.Builder().build().create(Service::class.java)
}