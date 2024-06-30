package com.bennyhuo.kotlin.coroutines.sample

import com.bennyhuo.kotlin.coroutines.cancel.suspendCancellableCoroutine
import com.bennyhuo.kotlin.coroutines.dispatcher.Dispatchers
import com.bennyhuo.kotlin.coroutines.launch
import com.bennyhuo.kotlin.coroutines.scope.GlobalScope
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JFrame.EXIT_ON_CLOSE
import javax.swing.JLabel
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url

class MainWindow : JFrame() {

    lateinit var button: JButton
    private lateinit var image: JLabel

    fun init() {
        button = JButton("Click me to show Logo")
        image = JLabel()
        image.size = Dimension(540, 258)

        contentPane.add(button, BorderLayout.NORTH)
        contentPane.add(image, BorderLayout.CENTER)
    }

    fun onButtonClick(listener: (ActionEvent) -> Unit) {
        button.addActionListener(listener)
    }

    fun setLogo(logoData: ByteArray) {
        val icon = ImageIcon(logoData)
        image.icon = icon
        this.size = Dimension(icon.iconWidth, icon.iconHeight)
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

        onButtonClick {
            GlobalScope.launch(Dispatchers.Swing) {
                println("Hello")
                LogoService.service.getLogo("https://kotlinlang.org/_next/static/chunks/images/hero-cover@2x-0095f955d809bb1d716b7ad41889166b.png")
                    .await().bytes().let { setLogo(it) }
            }
        }
    }
}


interface Service {
    @GET
    fun getLogo(@Url fileUrl: String): Call<ResponseBody>
}

object LogoService {
    val service = Retrofit.Builder()
        .baseUrl("https://kotlinlang.org")
        .build()
        .create(Service::class.java)
}

suspend fun <T> Call<T>.await() = suspendCancellableCoroutine<T> {
    it.invokeOnCancellation {
        cancel()
    }

    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            response.body()?.let { body -> it.resume(body) }
                ?: it.resumeWithException(RuntimeException("response body is null"))
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            it.resumeWithException(t)
        }
    })
}
