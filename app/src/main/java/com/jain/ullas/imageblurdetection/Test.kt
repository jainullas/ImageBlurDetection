package com.jain.ullas.imageblurdetection

object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        repeat(10)
        {
            functionA({
                print("Double Awesomeness")
            }, {
                print("Double Awesomeness")
            })
        }
    }

    inline fun functionA(lambda: () -> Unit, noinline lambda2: () -> Unit) {
        println("awesomeness !")
        lambda.invoke()
        lambda2.invoke()
    }

}